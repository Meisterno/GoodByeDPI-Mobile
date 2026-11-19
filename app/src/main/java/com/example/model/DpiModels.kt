package com.example.model

enum class DesyncMode(
    val flag: String,
    val titleTr: String,
    val descriptionTr: String
) {
    MODE_5(
        flag = "-5",
        titleTr = "Mod -5 (Türkiye Standart)",
        descriptionTr = "Otomatik TTL algılama + ters sıralı HTTP/HTTPS (SNI) parçalama. Türk Telekom, TurkNet ve Kablonet için önerilir."
    ),
    MODE_6(
        flag = "-6",
        titleTr = "Mod -6 (Modern + Wrong-SEQ)",
        descriptionTr = "Ters sıralı paket parçalama + sahte TCP SEQ numarası gönderimi. Katı DPI filtreleri için."
    ),
    MODE_9(
        flag = "-9",
        titleTr = "Mod -9 (Tam Kapsamlı / Superonline)",
        descriptionTr = "Wrong-Checksum + Wrong-SEQ + Ters Parçalama + Max-Payload. Superonline ve Vodafone CGNAT altyapıları için en güçlü mod."
    ),
    MODE_4(
        flag = "-4",
        titleTr = "Mod -4 (Hafif Parçalama)",
        descriptionTr = "Sahte paket göndermeden yalnızca HTTP/HTTPS SNI başlıklarını böler. Kopma yaşayan modemler için en güvenli mod."
    ),
    CUSTOM(
        flag = "--custom",
        titleTr = "Özel Parametre Dizisi",
        descriptionTr = "Tüm paket boyutu, TTL, Wrong-SEQ ve Checksum parametrelerini aşağıdan manuel yapılandırın."
    )
}

enum class IspPreset(
    val id: String,
    val title: String,
    val scriptFilename: String,
    val badgeText: String,
    val summary: String,
    val defaultConfig: DpiConfig
) {
    TURKEY_STANDARD(
        id = "turkey_dnsredir",
        title = "Türkiye Genel (Standart)",
        scriptFilename = "turkey_dnsredir.cmd",
        badgeText = "Türk Telekom / TurkNet / Kablonet",
        summary = "Varsayılan Türkiye profili. Yandex DNS (77.88.8.8:1253) port yönlendirmesi ve -5 otomatik TTL desync kullanır.",
        defaultConfig = DpiConfig(
            presetId = "turkey_dnsredir",
            desyncMode = DesyncMode.MODE_5,
            httpFragmentSize = 2,
            httpsFragmentSize = 40,
            nativeFragReverse = true,
            useSetTtl = false,
            ttlValue = 5,
            useAutoTtl = true,
            autoTtlSpec = "1-4-10",
            minTtl = 3,
            wrongChksum = false,
            wrongSeq = true,
            fragBySni = true,
            maxPayload = 1200,
            blockQuic = true,
            hostMixedCase = true,
            dnsAddress = "77.88.8.8",
            dnsPort = 1253,
            dnsV6Address = "2a02:6b8::feed:0ff",
            dnsV6Port = 1253,
            dohEndpoint = "https://cloudflare-dns.com/dns-query"
        )
    ),
    SUPERONLINE_ALT_1(
        id = "superonline_alt1",
        title = "Superonline Alternatif 1",
        scriptFilename = "turkey_dnsredir_alternative_superonline.cmd",
        badgeText = "Turkcell Superonline Fiber",
        summary = "Superonline DPI duvarı için -9 modu, --set-ttl 5 ve 1253 portlu alternatif DNS tüneli.",
        defaultConfig = DpiConfig(
            presetId = "superonline_alt1",
            desyncMode = DesyncMode.MODE_9,
            httpFragmentSize = 2,
            httpsFragmentSize = 40,
            nativeFragReverse = true,
            useSetTtl = true,
            ttlValue = 5,
            useAutoTtl = false,
            autoTtlSpec = "1-4-10",
            minTtl = 3,
            wrongChksum = true,
            wrongSeq = true,
            fragBySni = true,
            maxPayload = 1200,
            blockQuic = true,
            hostMixedCase = true,
            dnsAddress = "77.88.8.8",
            dnsPort = 1253,
            dnsV6Address = "2a02:6b8::feed:0ff",
            dnsV6Port = 1253,
            dohEndpoint = "https://dns.adguard-dns.com/dns-query"
        )
    ),
    SUPERONLINE_ALT_2(
        id = "superonline_alt2",
        title = "Superonline Alternatif 2 (TTL-Yok)",
        scriptFilename = "turkey_dnsredir_alternative2_superonline.cmd",
        badgeText = "Superonline Kopma Önleyici",
        summary = "--set-ttl parametresi yüzünden interneti kopan Superonline/TurkNet kullanıcıları için Wrong-SEQ + SNI split modu.",
        defaultConfig = DpiConfig(
            presetId = "superonline_alt2",
            desyncMode = DesyncMode.MODE_6,
            httpFragmentSize = 2,
            httpsFragmentSize = 2,
            nativeFragReverse = true,
            useSetTtl = false,
            ttlValue = 7,
            useAutoTtl = false,
            autoTtlSpec = "1-4-10",
            minTtl = 3,
            wrongChksum = true,
            wrongSeq = true,
            fragBySni = true,
            maxPayload = 1200,
            blockQuic = true,
            hostMixedCase = true,
            dnsAddress = "1.1.1.1",
            dnsPort = 53,
            dnsV6Address = "2606:4700:4700::1111",
            dnsV6Port = 53,
            dohEndpoint = "https://cloudflare-dns.com/dns-query"
        )
    ),
    SUPERONLINE_ALT_3(
        id = "superonline_alt3",
        title = "Superonline Alternatif 3 (TTL 3)",
        scriptFilename = "turkey_dnsredir_alternative3_superonline.cmd",
        badgeText = "Superonline / Vodafone Katı DPI",
        summary = "Düşük TTL (--set-ttl 3) ve agresif 2-bayt TLS ClientHello SNI parçalama (-e 2 -f 2) kullanır.",
        defaultConfig = DpiConfig(
            presetId = "superonline_alt3",
            desyncMode = DesyncMode.MODE_9,
            httpFragmentSize = 2,
            httpsFragmentSize = 2,
            nativeFragReverse = true,
            useSetTtl = true,
            ttlValue = 3,
            useAutoTtl = false,
            autoTtlSpec = "1-4-10",
            minTtl = 3,
            wrongChksum = true,
            wrongSeq = true,
            fragBySni = true,
            maxPayload = 1200,
            blockQuic = true,
            hostMixedCase = true,
            dnsAddress = "77.88.8.8",
            dnsPort = 1253,
            dnsV6Address = "2a02:6b8::feed:0ff",
            dnsV6Port = 1253,
            dohEndpoint = "https://dns.quad9.net/dns-query"
        )
    ),
    SUPERONLINE_ALT_4(
        id = "superonline_alt4",
        title = "Superonline Alternatif 4 (Quad9 / TTL 7)",
        scriptFilename = "turkey_dnsredir_alternative4_superonline.cmd",
        badgeText = "Superonline / Mobil Veri (4.5G/5G)",
        summary = "Yüksek hop sayılı mobil veri ve fiber ağlar için --set-ttl 7 ve Quad9 / Cloudflare şifreli DNS yönlendirmesi.",
        defaultConfig = DpiConfig(
            presetId = "superonline_alt4",
            desyncMode = DesyncMode.MODE_5,
            httpFragmentSize = 2,
            httpsFragmentSize = 40,
            nativeFragReverse = true,
            useSetTtl = true,
            ttlValue = 7,
            useAutoTtl = false,
            autoTtlSpec = "1-4-10",
            minTtl = 3,
            wrongChksum = false,
            wrongSeq = true,
            fragBySni = true,
            maxPayload = 1200,
            blockQuic = true,
            hostMixedCase = true,
            dnsAddress = "9.9.9.9",
            dnsPort = 9953,
            dnsV6Address = "2620:fe::fe",
            dnsV6Port = 9953,
            dohEndpoint = "https://dns.quad9.net/dns-query"
        )
    ),
    TURKNET_GIGAFIBER(
        id = "turknet_custom",
        title = "TurkNet & GigaFiber Özel",
        scriptFilename = "turkey_dnsredir_turknet.cmd",
        badgeText = "TurkNet VDSL / GigaFiber",
        summary = "TurkNet altyapısında sıfır gecikmeli Cloudflare DNS + saf SNI parçalama (--frag-by-sni).",
        defaultConfig = DpiConfig(
            presetId = "turknet_custom",
            desyncMode = DesyncMode.MODE_4,
            httpFragmentSize = 2,
            httpsFragmentSize = 40,
            nativeFragReverse = true,
            useSetTtl = false,
            ttlValue = 5,
            useAutoTtl = false,
            autoTtlSpec = "1-4-10",
            minTtl = 3,
            wrongChksum = false,
            wrongSeq = false,
            fragBySni = true,
            maxPayload = 1400,
            blockQuic = true,
            hostMixedCase = true,
            dnsAddress = "1.1.1.1",
            dnsPort = 53,
            dnsV6Address = "2606:4700:4700::1111",
            dnsV6Port = 53,
            dohEndpoint = "https://cloudflare-dns.com/dns-query"
        )
    )
}

data class DpiConfig(
    val presetId: String = IspPreset.TURKEY_STANDARD.id,
    val desyncMode: DesyncMode = DesyncMode.MODE_5,
    val httpFragmentSize: Int = 2,           // -f <value>
    val httpsFragmentSize: Int = 40,         // -e <value>
    val nativeFragReverse: Boolean = true,   // --native-frag / --reverse-frag
    val useSetTtl: Boolean = false,          // --set-ttl <value>
    val ttlValue: Int = 5,
    val useAutoTtl: Boolean = true,          // --auto-ttl 1-4-10
    val autoTtlSpec: String = "1-4-10",
    val minTtl: Int = 3,                     // --min-ttl 3
    val wrongChksum: Boolean = false,        // --wrong-chksum
    val wrongSeq: Boolean = true,            // --wrong-seq
    val fragBySni: Boolean = true,           // --frag-by-sni
    val maxPayload: Int = 1200,              // --max-payload 1200
    val blockQuic: Boolean = true,           // -q (Blocks UDP 443 QUIC so browsers fallback to TLS/TCP for SNI split)
    val hostMixedCase: Boolean = true,       // -m (Mix Host header case)
    val dnsAddress: String = "77.88.8.8",    // --dns-addr
    val dnsPort: Int = 1253,                 // --dns-port
    val dnsV6Address: String = "2a02:6b8::feed:0ff", // --dnsv6-addr
    val dnsV6Port: Int = 1253,               // --dnsv6-port
    val dohEndpoint: String = "https://cloudflare-dns.com/dns-query",
    val customExtraArgs: String = ""
) {
    /**
     * Generates the exact GoodbyeDPI-Turkey command line equivalent based on active parameters.
     */
    fun toCommandLineString(): String = buildString {
        append("goodbyedpi.exe ")
        if (desyncMode != DesyncMode.CUSTOM) {
            append("${desyncMode.flag} ")
        }
        append("-f $httpFragmentSize -e $httpsFragmentSize ")
        if (useSetTtl) {
            append("--set-ttl $ttlValue ")
        } else if (useAutoTtl) {
            append("--auto-ttl $autoTtlSpec --min-ttl $minTtl ")
        }
        if (wrongChksum) append("--wrong-chksum ")
        if (wrongSeq) append("--wrong-seq ")
        if (fragBySni) append("--frag-by-sni ")
        if (nativeFragReverse) append("--native-frag --reverse-frag ")
        if (hostMixedCase) append("-m ")
        if (blockQuic) append("-q ")
        if (maxPayload > 0) append("--max-payload $maxPayload ")
        append("--dns-addr $dnsAddress --dns-port $dnsPort ")
        if (dnsV6Address.isNotBlank()) {
            append("--dnsv6-addr $dnsV6Address --dnsv6-port $dnsV6Port")
        }
        if (customExtraArgs.isNotBlank()) {
            append(" ${customExtraArgs.trim()}")
        }
    }.trim()
}

data class DnsServerPreset(
    val name: String,
    val providerBadge: String,
    val ipv4: String,
    val port: Int,
    val ipv6: String,
    val v6Port: Int,
    val dohUrl: String,
    val description: String
)

val TURKEY_DNS_PRESETS = listOf(
    DnsServerPreset(
        name = "Yandex DNS (Port 1253 - Türkiye Varsayılan)",
        providerBadge = "Standart Port 53 Engelini Aşar",
        ipv4 = "77.88.8.8",
        port = 1253,
        ipv6 = "2a02:6b8::feed:0ff",
        v6Port = 1253,
        dohUrl = "https://common.dot.dns.yandex.net/dns-query",
        description = "GoodbyeDPI-Turkey orijinal yapılandırması. Standart 53 portu yerine 1253 portunu kullanarak İSS şeffaf DNS vekillerini atlatır."
    ),
    DnsServerPreset(
        name = "Cloudflare 1.1.1.1 + DoH",
        providerBadge = "En Düşük Ping / Gizlilik",
        ipv4 = "1.1.1.1",
        port = 53,
        ipv6 = "2606:4700:4700::1111",
        v6Port = 53,
        dohUrl = "https://cloudflare-dns.com/dns-query",
        description = "Cloudflare küresel Anycast DNS ve HTTPS üzerinden şifreli DNS (DoH) çözümleyicisi."
    ),
    DnsServerPreset(
        name = "Quad9 Güvenli DNS (Port 9953)",
        providerBadge = "Alternatif Port 9953",
        ipv4 = "9.9.9.9",
        port = 9953,
        ipv6 = "2620:fe::fe",
        v6Port = 9953,
        dohUrl = "https://dns.quad9.net/dns-query",
        description = "9953 alternatif portu ve şifreli DoH desteği ile Superonline ve Vodafone ağlarında yüksek başarı oranı sağlar."
    ),
    DnsServerPreset(
        name = "AdGuard DNS (Reklam & İzleyici Korumalı)",
        providerBadge = "DNS + Reklam Filtresi",
        ipv4 = "94.140.14.14",
        port = 5353,
        ipv6 = "2a10:50c0::ad1:ff",
        v6Port = 5353,
        dohUrl = "https://dns.adguard-dns.com/dns-query",
        description = "5353 alternatif portu üzerinden çalışır, sansür aşmanın yanı sıra reklam ve takipçi alan adlarını engeller."
    ),
    DnsServerPreset(
        name = "Google Public DNS",
        providerBadge = "Kararlı & Hızlı",
        ipv4 = "8.8.8.8",
        port = 53,
        ipv6 = "2001:4860:4860::8888",
        v6Port = 53,
        dohUrl = "https://dns.google/dns-query",
        description = "Google küresel DNS altyapısı ve DoH uç noktası."
    )
)

enum class LogSeverity(val label: String) {
    INFO("BİLGİ"),
    DESYNC("DESYNC"),
    DNS("DNS"),
    WARN("UYARI"),
    SUCCESS("BAŞARILI")
}

data class PacketLogEntry(
    val id: Long,
    val timestamp: String,
    val severity: LogSeverity,
    val protocol: String,
    val title: String,
    val detail: String
)

data class TunnelTelemetry(
    val isActive: Boolean = false,
    val isConnecting: Boolean = false,
    val connectedSinceEpochMs: Long = 0L,
    val totalPacketsInspected: Long = 0L,
    val sniFragmentsSent: Long = 0L,
    val dnsQueriesRedirected: Long = 0L,
    val quicPacketsBlocked: Long = 0L,
    val bytesProcessed: Long = 0L,
    val activeMtu: Int = 1280,
    val lastDetectedSni: String = "-"
)

enum class ThrottlingVerdict(
    val labelTr: String,
    val detailTr: String
) {
    NORMAL(
        labelTr = "NORMAL / ENGELSİZ",
        detailTr = "Düşük gecikme ve kararlı TLS SNI yanıtı. Hız kısıtlaması (throttling) veya DPI müdahalesi algılanmadı."
    ),
    THROTTLED(
        labelTr = "YAVAŞLATMA (THROTTLING) ŞÜPHESİ",
        detailTr = "Yüksek gecikme veya paket dalgalanması (jitter) tespit edildi. İSS bant genişliği daraltması uyguluyor olabilir."
    ),
    INTERCEPTED(
        labelTr = "DPI / DNS MÜDAHALESİ ALGILANDI",
        detailTr = "DNS yanıtı ile TLS SNI el sıkışma süresi arasında anormal fark veya bağlantı sıfırlaması (RST) tespit edildi."
    )
}

data class DiagnosticTargetResult(
    val domain: String,
    val category: String,
    val isTesting: Boolean = false,
    val resolvedIp: String? = null,
    val dnsLatencyMs: Long? = null,
    val tcpPingSamplesMs: List<Long> = emptyList(),
    val avgPingMs: Long? = null,
    val jitterMs: Long? = null,
    val packetLossPercent: Int = 0,
    val tlsHandshakeLatencyMs: Long? = null,
    val httpStatus: Int? = null,
    val sniSplitSimulatedBytes: String? = null,
    val throttlingVerdict: ThrottlingVerdict? = null,
    val isReachable: Boolean? = null,
    val errorDetail: String? = null
)
