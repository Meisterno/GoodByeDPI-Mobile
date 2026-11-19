package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.DesyncMode
import com.example.model.DpiConfig
import com.example.model.IspPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "goodbyedpi_tr_prefs")

class DpiPreferencesRepository(private val context: Context) {

    private object Keys {
        val PRESET_ID = stringPreferencesKey("preset_id")
        val DESYNC_MODE = stringPreferencesKey("desync_mode")
        val HTTP_FRAG_SIZE = intPreferencesKey("http_frag_size")
        val HTTPS_FRAG_SIZE = intPreferencesKey("https_frag_size")
        val NATIVE_FRAG_REVERSE = booleanPreferencesKey("native_frag_reverse")
        val USE_SET_TTL = booleanPreferencesKey("use_set_ttl")
        val TTL_VALUE = intPreferencesKey("ttl_value")
        val USE_AUTO_TTL = booleanPreferencesKey("use_auto_ttl")
        val AUTO_TTL_SPEC = stringPreferencesKey("auto_ttl_spec")
        val MIN_TTL = intPreferencesKey("min_ttl")
        val WRONG_CHKSUM = booleanPreferencesKey("wrong_chksum")
        val WRONG_SEQ = booleanPreferencesKey("wrong_seq")
        val FRAG_BY_SNI = booleanPreferencesKey("frag_by_sni")
        val MAX_PAYLOAD = intPreferencesKey("max_payload")
        val BLOCK_QUIC = booleanPreferencesKey("block_quic")
        val HOST_MIXED_CASE = booleanPreferencesKey("host_mixed_case")
        val DNS_ADDRESS = stringPreferencesKey("dns_address")
        val DNS_PORT = intPreferencesKey("dns_port")
        val DNS_V6_ADDRESS = stringPreferencesKey("dns_v6_address")
        val DNS_V6_PORT = intPreferencesKey("dns_v6_port")
        val DOH_ENDPOINT = stringPreferencesKey("doh_endpoint")
        val CUSTOM_EXTRA_ARGS = stringPreferencesKey("custom_extra_args")
        val DARK_THEME = booleanPreferencesKey("dark_theme")
    }

    val dpiConfigFlow: Flow<DpiConfig> = context.dataStore.data.map { prefs ->
        val default = IspPreset.TURKEY_STANDARD.defaultConfig
        val modeName = prefs[Keys.DESYNC_MODE] ?: default.desyncMode.name
        val desyncMode = runCatching { DesyncMode.valueOf(modeName) }.getOrDefault(default.desyncMode)

        DpiConfig(
            presetId = prefs[Keys.PRESET_ID] ?: default.presetId,
            desyncMode = desyncMode,
            httpFragmentSize = prefs[Keys.HTTP_FRAG_SIZE] ?: default.httpFragmentSize,
            httpsFragmentSize = prefs[Keys.HTTPS_FRAG_SIZE] ?: default.httpsFragmentSize,
            nativeFragReverse = prefs[Keys.NATIVE_FRAG_REVERSE] ?: default.nativeFragReverse,
            useSetTtl = prefs[Keys.USE_SET_TTL] ?: default.useSetTtl,
            ttlValue = prefs[Keys.TTL_VALUE] ?: default.ttlValue,
            useAutoTtl = prefs[Keys.USE_AUTO_TTL] ?: default.useAutoTtl,
            autoTtlSpec = prefs[Keys.AUTO_TTL_SPEC] ?: default.autoTtlSpec,
            minTtl = prefs[Keys.MIN_TTL] ?: default.minTtl,
            wrongChksum = prefs[Keys.WRONG_CHKSUM] ?: default.wrongChksum,
            wrongSeq = prefs[Keys.WRONG_SEQ] ?: default.wrongSeq,
            fragBySni = prefs[Keys.FRAG_BY_SNI] ?: default.fragBySni,
            maxPayload = prefs[Keys.MAX_PAYLOAD] ?: default.maxPayload,
            blockQuic = prefs[Keys.BLOCK_QUIC] ?: default.blockQuic,
            hostMixedCase = prefs[Keys.HOST_MIXED_CASE] ?: default.hostMixedCase,
            dnsAddress = prefs[Keys.DNS_ADDRESS] ?: default.dnsAddress,
            dnsPort = prefs[Keys.DNS_PORT] ?: default.dnsPort,
            dnsV6Address = prefs[Keys.DNS_V6_ADDRESS] ?: default.dnsV6Address,
            dnsV6Port = prefs[Keys.DNS_V6_PORT] ?: default.dnsV6Port,
            dohEndpoint = prefs[Keys.DOH_ENDPOINT] ?: default.dohEndpoint,
            customExtraArgs = prefs[Keys.CUSTOM_EXTRA_ARGS] ?: default.customExtraArgs
        )
    }

    val isDarkThemeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.DARK_THEME] ?: true
    }

    suspend fun saveConfig(config: DpiConfig) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PRESET_ID] = config.presetId
            prefs[Keys.DESYNC_MODE] = config.desyncMode.name
            prefs[Keys.HTTP_FRAG_SIZE] = config.httpFragmentSize
            prefs[Keys.HTTPS_FRAG_SIZE] = config.httpsFragmentSize
            prefs[Keys.NATIVE_FRAG_REVERSE] = config.nativeFragReverse
            prefs[Keys.USE_SET_TTL] = config.useSetTtl
            prefs[Keys.TTL_VALUE] = config.ttlValue
            prefs[Keys.USE_AUTO_TTL] = config.useAutoTtl
            prefs[Keys.AUTO_TTL_SPEC] = config.autoTtlSpec
            prefs[Keys.MIN_TTL] = config.minTtl
            prefs[Keys.WRONG_CHKSUM] = config.wrongChksum
            prefs[Keys.WRONG_SEQ] = config.wrongSeq
            prefs[Keys.FRAG_BY_SNI] = config.fragBySni
            prefs[Keys.MAX_PAYLOAD] = config.maxPayload
            prefs[Keys.BLOCK_QUIC] = config.blockQuic
            prefs[Keys.HOST_MIXED_CASE] = config.hostMixedCase
            prefs[Keys.DNS_ADDRESS] = config.dnsAddress
            prefs[Keys.DNS_PORT] = config.dnsPort
            prefs[Keys.DNS_V6_ADDRESS] = config.dnsV6Address
            prefs[Keys.DNS_V6_PORT] = config.dnsV6Port
            prefs[Keys.DOH_ENDPOINT] = config.dohEndpoint
            prefs[Keys.CUSTOM_EXTRA_ARGS] = config.customExtraArgs
        }
    }

    suspend fun setDarkTheme(isDark: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.DARK_THEME] = isDark
        }
    }
}
