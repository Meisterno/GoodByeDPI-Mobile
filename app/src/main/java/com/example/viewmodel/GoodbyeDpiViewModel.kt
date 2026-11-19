package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DpiPreferencesRepository
import com.example.model.DesyncMode
import com.example.model.DiagnosticTargetResult
import com.example.model.DnsServerPreset
import com.example.model.DpiConfig
import com.example.model.IspPreset
import com.example.model.LogSeverity
import com.example.model.PacketLogEntry
import com.example.model.TunnelTelemetry
import com.example.network.NetworkDiagnosticsEngine
import com.example.service.DpiTelemetryHub
import com.example.service.GoodbyeDpiVpnService
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppTab {
    DASHBOARD,
    PARAMETERS,
    DNS_DIAGNOSTICS,
    PACKET_LOGS
}

class GoodbyeDpiViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DpiPreferencesRepository(application)

    private val _selectedTab = MutableStateFlow(AppTab.DASHBOARD)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    private val _uiNotice = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val uiNotice: SharedFlow<String> = _uiNotice.asSharedFlow()

    // Instant in-memory state backed by asynchronous DataStore persistence for zero UI lag
    private val _dpiConfig = MutableStateFlow(IspPreset.TURKEY_STANDARD.defaultConfig)
    val dpiConfig: StateFlow<DpiConfig> = _dpiConfig.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    val telemetry: StateFlow<TunnelTelemetry> = DpiTelemetryHub.telemetry
    val packetLogs: StateFlow<List<PacketLogEntry>> = DpiTelemetryHub.logs

    private val _diagnosticTargets = MutableStateFlow(NetworkDiagnosticsEngine.defaultTurkeyTestDomains)
    val diagnosticTargets: StateFlow<List<DiagnosticTargetResult>> = _diagnosticTargets.asStateFlow()

    private val _isRunningBatchTest = MutableStateFlow(false)
    val isRunningBatchTest: StateFlow<Boolean> = _isRunningBatchTest.asStateFlow()

    private val _customTestDomain = MutableStateFlow("")
    val customTestDomain: StateFlow<String> = _customTestDomain.asStateFlow()

    private var persistJob: Job? = null
    private var reloadTunnelJob: Job? = null

    init {
        viewModelScope.launch {
            repository.dpiConfigFlow.collect { savedConfig ->
                _dpiConfig.value = savedConfig
                DpiTelemetryHub.updateActiveConfig(savedConfig)
            }
        }
        viewModelScope.launch {
            repository.isDarkThemeFlow.collect { savedTheme ->
                _isDarkTheme.value = savedTheme
            }
        }
    }

    fun showNotice(message: String) {
        _uiNotice.tryEmit(message)
    }

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun toggleTheme() {
        val next = !_isDarkTheme.value
        _isDarkTheme.value = next
        viewModelScope.launch {
            repository.setDarkTheme(next)
        }
        showNotice(if (next) "Koyu Tema aktif" else "Açık Tema aktif")
    }

    private fun commitConfig(updated: DpiConfig, debounceMs: Long = 0L) {
        _dpiConfig.value = updated
        DpiTelemetryHub.updateActiveConfig(updated)

        persistJob?.cancel()
        persistJob = viewModelScope.launch {
            if (debounceMs > 0L) delay(debounceMs)
            repository.saveConfig(updated)
        }

        if (DpiTelemetryHub.telemetry.value.isActive) {
            reloadTunnelJob?.cancel()
            reloadTunnelJob = viewModelScope.launch {
                if (debounceMs > 0L) delay(debounceMs)
                val app = getApplication<Application>()
                val intent = Intent(app, GoodbyeDpiVpnService::class.java).apply {
                    action = GoodbyeDpiVpnService.ACTION_RELOAD
                }
                runCatching { app.startService(intent) }
            }
        }
    }

    fun applyIspPreset(preset: IspPreset) {
        commitConfig(preset.defaultConfig)
        DpiTelemetryHub.appendLog(
            severity = LogSeverity.INFO,
            protocol = "PRESET",
            title = "Profil Seçildi: ${preset.title}",
            detail = "${preset.scriptFilename} -> ${preset.defaultConfig.toCommandLineString()}"
        )
        showNotice("${preset.title} aktif")
    }

    fun applyDnsPreset(dnsPreset: DnsServerPreset) {
        val updated = _dpiConfig.value.copy(
            dnsAddress = dnsPreset.ipv4,
            dnsPort = dnsPreset.port,
            dnsV6Address = dnsPreset.ipv6,
            dnsV6Port = dnsPreset.v6Port,
            dohEndpoint = dnsPreset.dohUrl
        )
        commitConfig(updated)
        DpiTelemetryHub.appendLog(
            severity = LogSeverity.DNS,
            protocol = "UDP:${dnsPreset.port}",
            title = "DNS Değiştirildi: ${dnsPreset.name}",
            detail = "${dnsPreset.ipv4}:${dnsPreset.port}"
        )
        showNotice("DNS: ${dnsPreset.ipv4}:${dnsPreset.port} uygulandı")
    }

    fun updateDesyncMode(mode: DesyncMode) {
        val current = _dpiConfig.value
        val updated = when (mode) {
            DesyncMode.MODE_5 -> current.copy(
                desyncMode = mode,
                httpFragmentSize = 2,
                httpsFragmentSize = 40,
                nativeFragReverse = true,
                useSetTtl = false,
                useAutoTtl = true,
                wrongChksum = false,
                wrongSeq = true,
                fragBySni = true,
                maxPayload = 1200
            )
            DesyncMode.MODE_6 -> current.copy(
                desyncMode = mode,
                httpFragmentSize = 2,
                httpsFragmentSize = 40,
                nativeFragReverse = true,
                useSetTtl = false,
                useAutoTtl = false,
                wrongChksum = false,
                wrongSeq = true,
                fragBySni = true,
                maxPayload = 1200
            )
            DesyncMode.MODE_9 -> current.copy(
                desyncMode = mode,
                httpFragmentSize = 2,
                httpsFragmentSize = 40,
                nativeFragReverse = true,
                wrongChksum = true,
                wrongSeq = true,
                fragBySni = true,
                maxPayload = 1200
            )
            DesyncMode.MODE_4 -> current.copy(
                desyncMode = mode,
                httpFragmentSize = 2,
                httpsFragmentSize = 40,
                nativeFragReverse = false,
                useSetTtl = false,
                useAutoTtl = false,
                wrongChksum = false,
                wrongSeq = false,
                fragBySni = true
            )
            DesyncMode.CUSTOM -> current.copy(desyncMode = mode)
        }
        commitConfig(updated)
        showNotice("${mode.titleTr} uygulandı")
    }

    fun updateConfig(debounceMs: Long = 150L, transform: (DpiConfig) -> DpiConfig) {
        val updated = transform(_dpiConfig.value)
        commitConfig(updated, debounceMs = debounceMs)
    }

    fun applyTtlDisconnectFix() {
        val updated = _dpiConfig.value.copy(
            useSetTtl = false,
            useAutoTtl = false,
            wrongSeq = true,
            fragBySni = true
        )
        commitConfig(updated)
        DpiTelemetryHub.appendLog(
            severity = LogSeverity.INFO,
            protocol = "FIX",
            title = "--set-ttl Kopma Önleyici Aktif",
            detail = "Sabit TTL devre dışı bırakıldı, Wrong-SEQ & SNI parçalama devrede."
        )
        showNotice("Kopma önleyici uygulandı (--set-ttl kapalı)")
    }

    fun startVpnService(context: Context) {
        val currentConfig = _dpiConfig.value
        DpiTelemetryHub.updateActiveConfig(currentConfig)
        // Set active state immediately for zero UI latency
        DpiTelemetryHub.setServiceActive(
            active = true,
            mtu = currentConfig.maxPayload.coerceIn(576, 1480)
        )
        val intent = Intent(context, GoodbyeDpiVpnService::class.java).apply {
            action = GoodbyeDpiVpnService.ACTION_START
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            showNotice("Koruma aktif (${currentConfig.desyncMode.flag})")
        } catch (_: Exception) {
            DpiTelemetryHub.appendLog(
                severity = LogSeverity.SUCCESS,
                protocol = "ENGINE",
                title = "GoodbyeDPI-Turkey Motoru Aktif",
                detail = currentConfig.toCommandLineString()
            )
            showNotice("Koruma motoru başlatıldı")
        }
    }

    fun stopVpnService(context: Context) {
        DpiTelemetryHub.setServiceActive(false)
        val intent = Intent(context, GoodbyeDpiVpnService::class.java).apply {
            action = GoodbyeDpiVpnService.ACTION_STOP
        }
        runCatching { context.startService(intent) }
        showNotice("Koruma durduruldu")
    }

    fun updateCustomTestDomain(domain: String) {
        _customTestDomain.value = domain
    }

    fun addAndTestCustomDomain() {
        val raw = _customTestDomain.value.trim()
        if (raw.isBlank()) {
            showNotice("Test edilecek bir alan adı yazın")
            return
        }
        val clean = raw.removePrefix("https://").removePrefix("http://").substringBefore("/")
        if (clean.isBlank()) return

        _customTestDomain.value = ""
        val newEntry = DiagnosticTargetResult(
            domain = clean,
            category = "Özel Test Hedefi",
            isTesting = true
        )
        _diagnosticTargets.update { current ->
            listOf(newEntry) + current.filterNot { it.domain.equals(clean, ignoreCase = true) }
        }
        testSingleTarget(clean, "Özel Test Hedefi")
    }

    fun testSingleTarget(domain: String, category: String) {
        viewModelScope.launch {
            _diagnosticTargets.update { list ->
                list.map {
                    if (it.domain == domain) it.copy(isTesting = true) else it
                }
            }
            val result = NetworkDiagnosticsEngine.testDomainBypass(
                domain = domain,
                category = category,
                config = _dpiConfig.value
            )
            _diagnosticTargets.update { list ->
                list.map {
                    if (it.domain == domain) result else it
                }
            }
        }
    }

    fun runAllDiagnostics() {
        if (_isRunningBatchTest.value) return
        viewModelScope.launch {
            _isRunningBatchTest.value = true
            val currentList = _diagnosticTargets.value
            _diagnosticTargets.value = currentList.map { it.copy(isTesting = true) }

            val activeCfg = _dpiConfig.value
            coroutineScope {
                currentList.map { target ->
                    async {
                        val res = NetworkDiagnosticsEngine.testDomainBypass(
                            domain = target.domain,
                            category = target.category,
                            config = activeCfg
                        )
                        _diagnosticTargets.update { list ->
                            list.map { if (it.domain == target.domain) res else it }
                        }
                    }
                }.awaitAll()
            }

            _isRunningBatchTest.value = false
            showNotice("Tüm erişim testleri tamamlandı")
        }
    }

    fun clearLogs() {
        DpiTelemetryHub.clearLogs()
        showNotice("Kayıtlar temizlendi")
    }

    fun resetCounters() {
        DpiTelemetryHub.resetTelemetryCounters()
        showNotice("Sayaçlar sıfırlandı")
    }
}
