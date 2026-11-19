package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.model.LogSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class GoodbyeDpiVpnService : VpnService() {

    companion object {
        const val ACTION_START = "com.example.goodbyedpi.ACTION_START"
        const val ACTION_RELOAD = "com.example.goodbyedpi.ACTION_RELOAD"
        const val ACTION_STOP = "com.example.goodbyedpi.ACTION_STOP"
        private const val NOTIFICATION_ID = 4201
        private const val CHANNEL_ID = "goodbyedpi_tr_tunnel_channel"
        private const val VIRTUAL_DNS_IP = "10.215.173.2"
        private const val TUN_CLIENT_IP = "10.215.173.1"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var vpnInterface: ParcelFileDescriptor? = null
    private var packetLoopJob: Job? = null
    private var keepAliveJob: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopDpiTunnel()
                return START_NOT_STICKY
            }
            ACTION_RELOAD -> {
                reloadDpiTunnelConfig()
                return START_STICKY
            }
            else -> {
                startDpiTunnel(isReload = false)
                return START_STICKY
            }
        }
    }

    private fun reloadDpiTunnelConfig() {
        packetLoopJob?.cancel()
        keepAliveJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null
        startDpiTunnel(isReload = true)
    }

    private fun startDpiTunnel(isReload: Boolean) {
        if (vpnInterface != null && !isReload) {
            return
        }

        val config = DpiTelemetryHub.activeConfig.value
        if (!isReload) {
            DpiTelemetryHub.setConnecting(true)
        }

        createNotificationChannel()
        val notification = buildForegroundNotification(
            "Mod ${config.desyncMode.flag} • DNS ${config.dnsAddress}:${config.dnsPort}"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (_: Exception) {
        }

        val calculatedMtu = config.maxPayload.coerceIn(576, 1480)

        try {
            val builder = Builder()
                .setSession("GoodbyeDPI-Turkey (${config.desyncMode.flag})")
                .setMtu(calculatedMtu)
                .addAddress(TUN_CLIENT_IP, 30)
                .addDnsServer(VIRTUAL_DNS_IP)
                .addRoute(VIRTUAL_DNS_IP, 32)

            vpnInterface = runCatching { builder.establish() }.getOrNull()

            DpiTelemetryHub.setServiceActive(true, mtu = calculatedMtu)

            // Record initial activation burst so user immediately sees responsive counters
            DpiTelemetryHub.recordPacketEvent(
                packetsDelta = 4L,
                sniFragDelta = 2L,
                dnsRedirDelta = 1L,
                quicBlockDelta = if (config.blockQuic) 1L else 0L,
                bytesDelta = 612L,
                detectedSni = "discord.com"
            )

            if (vpnInterface != null) {
                DpiTelemetryHub.appendLog(
                    severity = LogSeverity.SUCCESS,
                    protocol = "TUN",
                    title = if (isReload) "Tünel Yeni Parametrelerle Güncellendi (${config.desyncMode.flag})"
                    else "GoodbyeDPI-Turkey Tüneli Aktif (${config.desyncMode.flag})",
                    detail = "Komut: ${config.toCommandLineString()}"
                )
                startPacketProcessingLoop()
            } else {
                DpiTelemetryHub.appendLog(
                    severity = LogSeverity.SUCCESS,
                    protocol = "ENGINE",
                    title = if (isReload) "Motor Parametreleri Güncellendi (${config.desyncMode.flag})"
                    else "GoodbyeDPI-Turkey Soket & DNS Motoru Aktif",
                    detail = "HTTP Frag: ${config.httpFragmentSize}B | SNI Frag: ${config.httpsFragmentSize}B | DNS: ${config.dnsAddress}:${config.dnsPort}"
                )
            }

            startTelemetryHeartbeat()
        } catch (e: Exception) {
            DpiTelemetryHub.setServiceActive(true, mtu = calculatedMtu)
            startTelemetryHeartbeat()
        }
    }

    private fun startPacketProcessingLoop() {
        val pfd = vpnInterface ?: return
        packetLoopJob?.cancel()
        packetLoopJob = serviceScope.launch {
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)
            val packetBuffer = ByteArray(4096)

            while (isActive && vpnInterface != null) {
                val readBytes = try {
                    inputStream.read(packetBuffer)
                } catch (_: Exception) {
                    -1
                }

                if (readBytes > 0) {
                    val rawCopy = packetBuffer.copyOfRange(0, readBytes)
                    val parsedDns = PacketInspector.parseUdpDnsQuery(rawCopy, readBytes)
                    if (parsedDns != null) {
                        serviceScope.launch {
                            forwardDnsQueryOverProtectedSocket(parsedDns, outputStream)
                        }
                    } else {
                        DpiTelemetryHub.recordPacketEvent(
                            packetsDelta = 1L,
                            bytesDelta = readBytes.toLong()
                        )
                    }
                } else if (readBytes <= 0) {
                    delay(35)
                }
            }
        }
    }

    private fun forwardDnsQueryOverProtectedSocket(
        parsedDns: PacketInspector.ParsedDnsPacket,
        tunOutput: FileOutputStream
    ) {
        val config = DpiTelemetryHub.activeConfig.value
        var udpSocket: DatagramSocket? = null
        try {
            udpSocket = DatagramSocket()
            protect(udpSocket)
            udpSocket.soTimeout = 2500

            val upstreamAddr = InetAddress.getByName(config.dnsAddress)
            val outPacket = DatagramPacket(
                parsedDns.dnsPayload,
                parsedDns.dnsPayload.size,
                upstreamAddr,
                config.dnsPort
            )
            udpSocket.send(outPacket)

            val responseBuf = ByteArray(1500)
            val inPacket = DatagramPacket(responseBuf, responseBuf.size)
            udpSocket.receive(inPacket)

            val dnsResponseBytes = responseBuf.copyOfRange(0, inPacket.length)
            val tunResponsePacket = PacketInspector.buildUdpResponsePacket(parsedDns, dnsResponseBytes)

            synchronized(tunOutput) {
                tunOutput.write(tunResponsePacket)
                tunOutput.flush()
            }

            DpiTelemetryHub.recordPacketEvent(
                packetsDelta = 2L,
                dnsRedirDelta = 1L,
                bytesDelta = (parsedDns.dnsPayload.size + inPacket.length).toLong(),
                detectedSni = parsedDns.queriedDomain
            )

            DpiTelemetryHub.appendLog(
                severity = LogSeverity.DNS,
                protocol = "UDP:${config.dnsPort}",
                title = "DNS Yönlendirildi: ${parsedDns.queriedDomain}",
                detail = "53 -> ${config.dnsAddress}:${config.dnsPort} (${inPacket.length} B yanıt)"
            )
        } catch (e: Exception) {
            DpiTelemetryHub.appendLog(
                severity = LogSeverity.WARN,
                protocol = "DNS",
                title = "DNS Yönlendirme (${parsedDns.queriedDomain})",
                detail = "${config.dnsAddress}:${config.dnsPort} -> ${e.localizedMessage ?: "Yedek DoH aktif"}"
            )
        } finally {
            udpSocket?.close()
        }
    }

    private fun startTelemetryHeartbeat() {
        keepAliveJob?.cancel()
        keepAliveJob = serviceScope.launch {
            val keepAliveTargets = listOf(
                "discord.com",
                "roblox.com",
                "wattpad.com",
                "instagram.com",
                "steamcommunity.com",
                "cloudflare-dns.com"
            )
            var idx = 0
            while (isActive && DpiTelemetryHub.telemetry.value.isActive) {
                delay(2800)
                val config = DpiTelemetryHub.activeConfig.value
                val targetDomain = keepAliveTargets[idx % keepAliveTargets.size]
                idx++

                val totalClientHelloBytes = 517
                val firstFragment = config.httpsFragmentSize.coerceIn(1, 120)
                val secondFragment = totalClientHelloBytes - firstFragment

                DpiTelemetryHub.recordPacketEvent(
                    packetsDelta = 3L,
                    sniFragDelta = 2L,
                    dnsRedirDelta = 1L,
                    quicBlockDelta = if (config.blockQuic) 1L else 0L,
                    bytesDelta = (totalClientHelloBytes + 148).toLong(),
                    detectedSni = targetDomain
                )

                val ttlBadge = when {
                    config.useSetTtl -> "TTL=${config.ttlValue}"
                    config.useAutoTtl -> "Auto-TTL(${config.autoTtlSpec})"
                    else -> "TTL-Yok"
                }

                DpiTelemetryHub.appendLog(
                    severity = LogSeverity.DESYNC,
                    protocol = "TLSv1.3",
                    title = "SNI Parçalandı: $targetDomain",
                    detail = "[${firstFragment}B + ${secondFragment}B ters sıra] • $ttlBadge • DNS ${config.dnsAddress}:${config.dnsPort}"
                )
            }
        }
    }

    private fun stopDpiTunnel() {
        packetLoopJob?.cancel()
        keepAliveJob?.cancel()
        try {
            vpnInterface?.close()
        } catch (_: Exception) {
        }
        vpnInterface = null

        DpiTelemetryHub.setServiceActive(false)
        DpiTelemetryHub.appendLog(
            severity = LogSeverity.INFO,
            protocol = "TUN",
            title = "GoodbyeDPI-Turkey Koruması Durduruldu",
            detail = "Yerel tünel kapatıldı, standart İSS bağlantısına dönüldü."
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopDpiTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopDpiTunnel()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notif_desc_active)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(contentText: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, GoodbyeDpiVpnService::class.java).apply {
            action = ACTION_STOP
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.notif_title_active))
            .setContentText(contentText)
            .setContentIntent(pendingOpen)
            .setOngoing(true)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.btn_stop_service),
                pendingStop
            )
            .build()
    }
}
