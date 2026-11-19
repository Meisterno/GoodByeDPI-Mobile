package com.example.service

import com.example.model.DpiConfig
import com.example.model.LogSeverity
import com.example.model.PacketLogEntry
import com.example.model.TunnelTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * Shared singleton telemetry and packet inspection hub between [GoodbyeDpiVpnService] and the UI ViewModel.
 */
object DpiTelemetryHub {
    private val logIdCounter = AtomicLong(1000L)
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val _telemetry = MutableStateFlow(TunnelTelemetry())
    val telemetry: StateFlow<TunnelTelemetry> = _telemetry.asStateFlow()

    private val _activeConfig = MutableStateFlow(DpiConfig())
    val activeConfig: StateFlow<DpiConfig> = _activeConfig.asStateFlow()

    private val _logs = MutableStateFlow<List<PacketLogEntry>>(
        listOf(
            PacketLogEntry(
                id = 1L,
                timestamp = timeFormatter.format(Date()),
                severity = LogSeverity.INFO,
                protocol = "CORE",
                title = "GoodbyeDPI-Turkey Mobil Motoru Hazır",
                detail = "WinDivert tabanlı masaüstü parametreleri Android yerel VpnService & Soket katmanına eşlendi."
            )
        )
    )
    val logs: StateFlow<List<PacketLogEntry>> = _logs.asStateFlow()

    fun updateActiveConfig(config: DpiConfig) {
        _activeConfig.value = config
    }

    fun setConnecting(connecting: Boolean) {
        _telemetry.update { it.copy(isConnecting = connecting) }
    }

    fun setServiceActive(active: Boolean, mtu: Int = 1280) {
        _telemetry.update { current ->
            val since = when {
                !active -> 0L
                current.isActive && current.connectedSinceEpochMs > 0L -> current.connectedSinceEpochMs
                else -> System.currentTimeMillis()
            }
            current.copy(
                isActive = active,
                isConnecting = false,
                connectedSinceEpochMs = since,
                activeMtu = mtu
            )
        }
    }

    fun recordPacketEvent(
        packetsDelta: Long = 1L,
        sniFragDelta: Long = 0L,
        dnsRedirDelta: Long = 0L,
        quicBlockDelta: Long = 0L,
        bytesDelta: Long = 0L,
        detectedSni: String? = null
    ) {
        _telemetry.update { current ->
            current.copy(
                totalPacketsInspected = current.totalPacketsInspected + packetsDelta,
                sniFragmentsSent = current.sniFragmentsSent + sniFragDelta,
                dnsQueriesRedirected = current.dnsQueriesRedirected + dnsRedirDelta,
                quicPacketsBlocked = current.quicPacketsBlocked + quicBlockDelta,
                bytesProcessed = current.bytesProcessed + bytesDelta,
                lastDetectedSni = detectedSni ?: current.lastDetectedSni
            )
        }
    }

    fun appendLog(
        severity: LogSeverity,
        protocol: String,
        title: String,
        detail: String
    ) {
        val entry = PacketLogEntry(
            id = logIdCounter.incrementAndGet(),
            timestamp = timeFormatter.format(Date()),
            severity = severity,
            protocol = protocol,
            title = title,
            detail = detail
        )
        _logs.update { currentList ->
            (listOf(entry) + currentList).take(120)
        }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        appendLog(
            severity = LogSeverity.INFO,
            protocol = "SYSTEM",
            title = "Kayıt Defteri Temizlendi",
            detail = "Paket yakalama ve DPI desync günlükleri sıfırlandı."
        )
    }

    fun resetTelemetryCounters() {
        _telemetry.update {
            it.copy(
                totalPacketsInspected = 0L,
                sniFragmentsSent = 0L,
                dnsQueriesRedirected = 0L,
                quicPacketsBlocked = 0L,
                bytesProcessed = 0L,
                lastDetectedSni = "-"
            )
        }
    }
}
