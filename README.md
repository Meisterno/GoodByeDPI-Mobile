# GoodbyeDPI-Turkey Android (GoodbyeDPI TR)

[![Platform](https://img.shields.io/badge/Platform-Android%207.0%2B%20(API%2024%2B)-141414?style=flat-square&logo=android)](#)
[![Language](https://img.shields.io/badge/Kotlin-2.0%2B-262626?style=flat-square&logo=kotlin)](#)
[![UI](https://img.shields.io/badge/UI-Jetpack%20Compose%20(Monochrome%20M3)-404040?style=flat-square)](#)
[![Root](https://img.shields.io/badge/Root-Not%20Required%20(VpnService)-595959?style=flat-square)](#)

> **[TR]** Türkiye'deki İnternet Servis Sağlayıcılarının (İSS) uyguladığı DPI (Derin Paket İnceleme) ve şeffaf DNS engellemelerini harici bir VPN sunucusuna bağlanmadan, hız kaybı yaşatmadan aşmak için tasarlanmış açık kaynaklı Android uygulaması.  
> **[EN]** An open-source Android client adapting the parameters and DNS redirection strategies of **GoodbyeDPI-Turkey** into a non-root local `VpnService` & socket desync architecture with a clean, minimalist monochrome interface.

---

## İçindekiler / Table of Contents
1. [Türkçe Dokümantasyon (TR)](#-türkçe-dokümantasyon-tr)
   - [Özellikler](#özellikler)
   - [Hazır Türkiye İSS Profilleri (.cmd Karşılıkları)](#hazır-türkiye-iss-profilleri-cmd-karşılıkları)
   - [Ağ Gecikmesi (Ping) & Throttling Teşhis Aracı](#ağ-gecikmesi-ping--throttling-teşhis-aracı)
   - [Sorun Giderme (`--set-ttl` Kopma Çözümü)](#sorun-giderme---set-ttl-kopma-çözümü)
2. [English Documentation (EN)](#-english-documentation-en)
   - [Key Features](#key-features)
   - [Turkish ISP Preset Mapping](#turkish-isp-preset-mapping)
   - [Latency, Jitter & Throttling Diagnostic Suite](#latency-jitter--throttling-diagnostic-suite)
   - [Architecture & Technical Stack](#architecture--technical-stack)
3. [Team & Credits / Ekip ve Katkıda Bulunanlar](#-team--credits--ekip-ve-katkıda-bulunanlar)
4. [GitHub'da Paylaşma & Kurulum / Publishing to GitHub & Build](#-githubda-paylaşma--kurulum--publishing-to-github--build)

---

## 🇹🇷 Türkçe Dokümantasyon (TR)

### Özellikler
- **Root Gerektirmeyen Yerel Tünel (`VpnService`):** Harici bir uzak sunucuya trafik yönlendirmez; internet hızınızı ve oyun içi ping değerlerinizi düşürmez.
- **Şeffaf DNS Engelini Aşma (Alternatif Port Yönlendirme):** Türkiye'deki İSS'ler standart `UDP 53` portundaki DNS sorgularını yakalar. Uygulama, yerel tünel üzerinden gelen DNS isteklerini korumalı soketlerle **`77.88.8.8:1253` (Yandex)**, **`9.9.9.9:9953` (Quad9)** veya **`94.140.14.14:5353` (AdGuard)** gibi alternatif portlara ve DoH uç noktalarına yönlendirir.
- **TLS SNI & HTTP Paket Parçalama (`Desync`):** `-4`, `-5`, `-6`, `-9` modları ile `-f` (HTTP parçalama), `-e` (HTTPS/TLS ClientHello SNI parçalama), `--frag-by-sni`, `--native-frag --reverse-frag`, `--wrong-seq`, `--wrong-chksum`, `--auto-ttl`, `--set-ttl` ve `-q` (UDP QUIC/HTTP3 engelleme) parametrelerini tam olarak destekler.
- **Canlı Bağlantı Durumu & Paket Akış Monitörü:** Servis aktif olduğunda tüm ekranların üstünde gerçek zamanlı bağlantı süresini (`00:00`), canlı sinyal dalgasını (`Waveform`) ve aktif paket işleme hattını (`TUN -> SNI Split -> DNS Port -> Wrong-SEQ/TTL`) gösterir.
- **Sade Monokrom (Siyah / Beyaz / Gri) Arayüz:** Göz yormayan, yüksek kontrastlı ve 60 FPS akıcı çalışacak şekilde optimize edilmiş sade tasarım.

### Hazır Türkiye İSS Profilleri (`.cmd` Karşılıkları)
| Profil Adı | Orijinal Betik Karşılığı | Hedef İSS | Varsayılan Parametre Özeti |
| :--- | :--- | :--- | :--- |
| **Türkiye Genel (Standart)** | `turkey_dnsredir.cmd` | Türk Telekom, TurkNet, Kablonet | `-5 -f 2 -e 40 --auto-ttl 1-4-10 --dns-addr 77.88.8.8 --dns-port 1253` |
| **Superonline Alternatif 1** | `turkey_dnsredir_alternative_superonline.cmd` | Turkcell Superonline Fiber | `-9 -f 2 -e 40 --set-ttl 5 --wrong-chksum --wrong-seq --dns-port 1253` |
| **Superonline Alternatif 2 (TTL-Yok)** | `turkey_dnsredir_alternative2_superonline.cmd` | Superonline / TurkNet (Kopma Önleyici) | `-6 -f 2 -e 2 --wrong-chksum --wrong-seq --frag-by-sni` (`--set-ttl` kapalı) |
| **Superonline Alternatif 3 (TTL 3)** | `turkey_dnsredir_alternative3_superonline.cmd` | Superonline / Vodafone Katı DPI | `-9 -f 2 -e 2 --set-ttl 3 --wrong-chksum --wrong-seq --dns-port 1253` |
| **Superonline Alternatif 4 (TTL 7)** | `turkey_dnsredir_alternative4_superonline.cmd` | Superonline / Mobil Veri (4.5G/5G) | `-5 -f 2 -e 40 --set-ttl 7 --dns-addr 9.9.9.9 --dns-port 9953` |
| **TurkNet & GigaFiber Özel** | `turkey_dnsredir_turknet.cmd` | TurkNet VDSL / GigaFiber | `-4 -f 2 -e 40 --frag-by-sni --max-payload 1400 --dns-addr 1.1.1.1` |

### Ağ Gecikmesi (Ping) & Throttling Teşhis Aracı
**DNS & Test** sekmesinde yer alan teşhis motoru; `discord.com`, `roblox.com`, `youtube.com`, `x.com`, `instagram.com`, `wattpad.com`, `steamcommunity.com` ve `cloudflare.com` (veya ekleyeceğiniz özel alan adları) için:
1. **Alternatif Port DNS Çözümleme Süresi (`ms`)**
2. **4x Ardışık TCP/443 Ping Örneklemi, Ortalama Ping (`ms`), Jitter (`±ms`) ve Paket Kaybı (`%`)**
3. **TLSv1.3 SNI El Sıkışma Süresi (`ms`)**
ölçümlerini paralel olarak yapar ve bağlantınızın **`NORMAL / ENGELSİZ`**, **`YAVAŞLATMA (THROTTLING) ŞÜPHESİ`** veya **`DPI / DNS MÜDAHALESİ ALGILANDI`** durumunda olup olmadığını raporlar.

### Sorun Giderme (`--set-ttl` Kopma Çözümü)
Eğer korumayı başlattığınızda bazı sitelerde *"Bağlantı Sıfırlandı"* hatası alıyor veya internet kopması yaşıyorsanız:
- Ana ekrandaki **`--set-ttl Kaldır (Önerilen)`** butonuna dokunun veya **`Superonline Alternatif 2 (TTL-Yok)`** profilini seçin.
- Alternatif olarak **`TTL=7 Yap`** butonuyla `--set-ttl 7` değerini tek dokunuşla uygulayabilirsiniz.

---

## 🇬🇧 English Documentation (EN)

### Key Features
- **Non-Root Local Tunnel (`VpnService`):** Operates entirely on-device without routing traffic through third-party external proxy servers, preserving 100% of your ISP bandwidth and low gaming latency.
- **Alternative Port DNS Redirection:** Turkish ISPs frequently hijack standard `UDP 53` DNS traffic via transparent DNS proxies. This app intercepts local DNS packets and forwards them via protected sockets to non-standard ports such as **`77.88.8.8:1253` (Yandex)**, **`9.9.9.9:9953` (Quad9)**, or **`94.140.14.14:5353` (AdGuard)** with DNS-over-HTTPS (DoH) fallback.
- **Full GoodbyeDPI Parameter Editor:** Fine-grained controls for modes `-4`, `-5`, `-6`, `-9`, HTTP fragmentation (`-f`), HTTPS/TLS SNI fragmentation (`-e`), `--frag-by-sni`, `--native-frag --reverse-frag`, `--wrong-seq`, `--wrong-chksum`, `--auto-ttl`, `--set-ttl` (`3..9`), `--max-payload`, and QUIC blocking (`-q`).
- **Real-Time Visual Connection Status Indicator:** Features a persistent top-bar beacon badge and a live packet-pipeline monitor (`TUN -> SNI Split -> DNS Port -> Wrong-SEQ/TTL`) with an animated waveform and live uptime counter.
- **Minimalist Monochrome Design:** Pure Black, White, and Gray Material 3 interface engineered for zero visual clutter and instant 60 FPS state responsiveness.

### Turkish ISP Preset Mapping
All presets mirror the official batch scripts from `cagritaskn/GoodbyeDPI-Turkey`, including `turkey_dnsredir.cmd` and the `alternative_superonline` (`1..4`) variants tailored for Turkcell Superonline CGNAT, Türk Telekom, TurkNet GigaFiber, Kablonet, and Vodafone networks.

### Latency, Jitter & Throttling Diagnostic Suite
The built-in diagnostic tool runs 4x consecutive TCP/TLS probes against popular services (`discord.com`, `roblox.com`, `youtube.com`, `x.com`, `instagram.com`, `wattpad.com`, `steamcommunity.com`, `cloudflare.com`, plus custom user domains) to measure:
- **DNS Resolution Latency** vs. **4x TCP RTT Ping Samples** vs. **TLS SNI Handshake Duration**
- **Jitter (`±ms`)** and **Packet Loss (`%`)**
- Automated verdict detection for **Bandwidth Throttling (QoS shaping)** and **DPI SNI Inspection / RST Interception**.

### Architecture & Technical Stack
- **Language & UI:** 100% Kotlin + Jetpack Compose (Material 3 Monochrome Theme, Adaptive `Compact`/`Expanded` layouts).
- **State & Persistence:** MVVM (`GoodbyeDpiViewModel` + in-memory zero-latency `StateFlow` backed by debounced Jetpack `DataStore`).
- **Networking & Tunneling:** Android `VpnService` (`GoodbyeDpiVpnService`), raw IPv4/UDP DNS packet parser & checksum rebuilder (`PacketInspector`), `SSLSocket` with `SNIHostName` & `TCP_NODELAY`, and `OkHttp` DoH client.

---

## 👥 Team & Credits / Ekip ve Katkıda Bulunanlar

### 🇹🇷 Ekip & Teşekkür (TR)
Bu proje, açık kaynak topluluğunun internet özgürlüğü ve ağ teşhis araçlarına yaptığı katkılar üzerine inşa edilmiştir:

| Rol / Katkı | İsim / Proje | Açıklama |
| :--- | :--- | :--- |
| **Mobil Uygulama & Android Mimarisi** | **GoodbyeDPI TR Mobile Team** (`@demez938`) | Android `VpnService` tünel mimarisi, monokrom Jetpack Compose arayüzü, canlı bağlantı monitörü ve çoklu ping/throttling teşhis motoru geliştirmesi. |
| **GoodbyeDPI-Turkey Çatalı & İSS Betikleri** | **Çağrı Taşkın** ([`cagritaskn/GoodbyeDPI-Turkey`](https://github.com/cagritaskn/GoodbyeDPI-Turkey)) | Türkiye'deki İSS altyapılarına (Türk Telekom, Superonline Alternatif 1-4, TurkNet, Kablonet) özel parametre profilleri, `1253` port yönlendirmesi ve `--set-ttl` çözüm rehberleri. |
| **Orijinal GoodbyeDPI Motoru** | **ValdikSS** ([`ValdikSS/GoodbyeDPI`](https://github.com/ValdikSS/GoodbyeDPI)) | Orijinal pasif/aktif DPI paket parçalama, TCP pencere/SNI bölme ve sahte paket (Fake Packet) desync algoritmalarının yaratıcısı. |

### 🇬🇧 Team & Acknowledgments (EN)
This project builds upon the open-source research and contributions of the network privacy community:

| Role / Contribution | Name / Project | Description |
| :--- | :--- | :--- |
| **Android Lead & Mobile Architecture** | **GoodbyeDPI TR Mobile Team** (`@demez938`) | Android `VpnService` local tunnel implementation, minimalist monochrome Jetpack Compose UI, real-time connection pipeline monitor, and multi-sample latency/throttling diagnostic engine. |
| **GoodbyeDPI-Turkey Maintainer** | **Çağrı Taşkın** ([`cagritaskn/GoodbyeDPI-Turkey`](https://github.com/cagritaskn/GoodbyeDPI-Turkey)) | Creator and maintainer of the Turkey-specific fork, ISP preset configurations (`turkey_dnsredir.cmd` & Superonline alternatives), and community troubleshooting guides. |
| **Original GoodbyeDPI Author** | **ValdikSS** ([`ValdikSS/GoodbyeDPI`](https://github.com/ValdikSS/GoodbyeDPI)) | Author of the original GoodbyeDPI deep packet inspection circumvention utility and TCP/TLS desynchronization techniques. |

---

## 🚀 GitHub'da Paylaşma & Kurulum / Publishing to GitHub & Build

### 1. AI Studio Üzerinden Tek Tıkla GitHub'a Gönderme (Recommended)
1. AI Studio sağ üst köşesindeki **Settings / Export (Ayarlar / Dışa Aktar)** menüsüne tıklayın.
2. **Push to GitHub (GitHub'a Gönder)** seçeneğini seçin, GitHub hesabınızı bağlayın ve depo adı olarak `GoodbyeDPI-Turkey-Android` girip onaylayın.

### 2. Git Komut Satırı ile Paylaşma (CLI)
Projeyi ZIP olarak indirdikten sonra kendi GitHub hesabınızda paylaşmak için:
```bash
git init
git add .
git commit -m "feat: GoodbyeDPI-Turkey Android monochrome edition with live status & latency diagnostics"
git branch -M main
git remote add origin https://github.com/<KULLANICI_ADINIZ>/GoodbyeDPI-Turkey-Android.git
git push -u origin main
```

### 3. Derleme / Building the APK
```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```
