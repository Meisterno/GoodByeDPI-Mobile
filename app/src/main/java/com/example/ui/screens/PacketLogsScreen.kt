package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.LogSeverity
import com.example.model.PacketLogEntry
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun PacketLogsScreen(
    logs: List<PacketLogEntry>,
    onClearLogs: () -> Unit,
    onLogsCopied: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedFilter by remember { mutableStateOf<LogSeverity?>(null) }
    var showGuideExpanded by remember { mutableStateOf(false) }
    var showTeamExpanded by remember { mutableStateOf(false) }

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs else logs.filter { it.severity == selectedFilter }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Paket Konsolu (${filteredLogs.size})",
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                IconButton(
                    onClick = { showTeamExpanded = !showTeamExpanded },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_toggle_team_credits")
                ) {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = "Team & GitHub Credits (TR/EN)",
                        tint = if (showTeamExpanded) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showGuideExpanded = !showGuideExpanded },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_toggle_turkey_guide")
                ) {
                    Icon(
                        imageVector = Icons.Default.HelpOutline,
                        contentDescription = "GoodbyeDPI-Turkey Rehberi",
                        tint = if (showGuideExpanded) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = {
                        val allText = filteredLogs.joinToString("\n") {
                            "[${it.timestamp}] [${it.severity.label}/${it.protocol}] ${it.title} -> ${it.detail}"
                        }
                        clipboardManager.setText(AnnotatedString(allText))
                        onLogsCopied()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_copy_all_logs")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Tüm Kayıtları Kopyala",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onClearLogs,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("btn_clear_logs")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Kayıtları Temizle",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("Tümü") }
            )
            LogSeverity.entries.forEach { sev ->
                FilterChip(
                    selected = selectedFilter == sev,
                    onClick = { selectedFilter = if (selectedFilter == sev) null else sev },
                    label = { Text(sev.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        selectedLabelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
            FilterChip(
                selected = showTeamExpanded,
                onClick = { showTeamExpanded = !showTeamExpanded },
                label = { Text("Team / GitHub (TR·EN)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Groups,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier.testTag("chip_team_github_info")
            )
        }

        // Bilingual (TR & EN) Team, Credits & GitHub Info Card
        if (showTeamExpanded) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("team_credits_card"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.onSurface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TEAM & CREDITS / EKİP VE KATKIDA BULUNANLAR",
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "[TR] • Mobil Uygulama & Android Mimarisi: GoodbyeDPI TR Mobile Team (@demez938) — Yerel VpnService tüneli, monokrom Compose arayüzü, canlı bağlantı monitörü ve çoklu ping/throttling teşhis aracı.\n" +
                            "• GoodbyeDPI-Turkey Çatalı & İSS Betikleri: Çağrı Taşkın (github.com/cagritaskn/GoodbyeDPI-Turkey) — Türkiye İSS profilleri (turkey_dnsredir.cmd & Superonline alternatifleri).\n" +
                            "• Orijinal GoodbyeDPI Motoru: ValdikSS (github.com/ValdikSS/GoodbyeDPI) — DPI paket parçalama ve TCP/SNI desync algoritmaları.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    Text(
                        text = "[EN] • Android Lead & Mobile Architecture: GoodbyeDPI TR Mobile Team (@demez938) — Non-root Android VpnService local tunnel, minimalist monochrome Jetpack Compose UI, real-time connection indicator, and multi-sample latency/throttling diagnostic engine.\n" +
                            "• GoodbyeDPI-Turkey Maintainer: Çağrı Taşkın (github.com/cagritaskn/GoodbyeDPI-Turkey) — Turkey-specific ISP scripts, port 1253 DNS redirection & community guides.\n" +
                            "• Original GoodbyeDPI Author: ValdikSS (github.com/ValdikSS/GoodbyeDPI) — Core DPI circumvention & TCP/TLS SNI fragmentation research.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(
                                AnnotatedString(
                                    "GoodbyeDPI-Turkey Android (GoodbyeDPI TR)\n" +
                                        "Based on: https://github.com/cagritaskn/GoodbyeDPI-Turkey & https://github.com/ValdikSS/GoodbyeDPI\n" +
                                        "Android Architecture & Monochrome UI: GoodbyeDPI TR Mobile Team (@demez938)"
                                )
                            )
                            onLogsCopied()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("GitHub & Team Bilgisini Kopyala (TR/EN)")
                    }
                }
            }
        }

        if (showGuideExpanded) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "GoodbyeDPI-Turkey Mobil Çalışma Mimarisi",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "• DNS Yönlendirme: İSS'lerin standart 53 portundaki şeffaf DNS engelini aşmak için sorguları 77.88.8.8:1253 (Yandex) veya 9.9.9.9:9953 (Quad9) portuna yönlendirir.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• TLS SNI Parçalama: HTTPS ClientHello paketini SNI alan adından bölerek (-e 2 / -e 40) İSS DPI filtrelerini atlatır.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Kopma Çözümü: İnternet kopması yaşıyorsanız 'Superonline Alternatif 2 (TTL-Yok)' profilini seçin veya --set-ttl'yi kapatın.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Seçili filtreye uygun paket kaydı bulunmuyor.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("packet_logs_lazy_column"),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredLogs, key = { it.id }) { entry ->
                    PacketLogItemCard(entry = entry)
                }
            }
        }
    }
}

@Composable
private fun PacketLogItemCard(entry: PacketLogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(11.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = entry.severity.label,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.background,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = entry.protocol,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Text(
                    text = entry.timestamp,
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(1.dp))

            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = entry.detail,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
