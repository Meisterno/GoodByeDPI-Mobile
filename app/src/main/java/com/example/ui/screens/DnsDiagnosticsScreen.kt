package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DiagnosticTargetResult
import com.example.model.DnsServerPreset
import com.example.model.DpiConfig
import com.example.model.TURKEY_DNS_PRESETS
import com.example.model.ThrottlingVerdict
import com.example.ui.theme.JetBrainsMonoFamily

@Composable
fun DnsDiagnosticsScreen(
    config: DpiConfig,
    diagnosticTargets: List<DiagnosticTargetResult>,
    isRunningBatchTest: Boolean,
    customDomainInput: String,
    onCustomDomainInputChange: (String) -> Unit,
    onAddAndTestCustomDomain: () -> Unit,
    onSelectDnsPreset: (DnsServerPreset) -> Unit,
    onUpdateConfig: ((DpiConfig) -> DpiConfig) -> Unit,
    onTestSingleTarget: (String, String) -> Unit,
    onRunAllDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showCustomDnsFields by remember { mutableStateOf(false) }
    var dnsPortText by remember { mutableStateOf(config.dnsPort.toString()) }
    var dnsV6PortText by remember { mutableStateOf(config.dnsV6Port.toString()) }

    LaunchedEffect(config.dnsPort) {
        dnsPortText = config.dnsPort.toString()
    }
    LaunchedEffect(config.dnsV6Port) {
        dnsV6PortText = config.dnsV6Port.toString()
    }

    val testedResults = remember(diagnosticTargets) {
        diagnosticTargets.filter { it.avgPingMs != null }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dns_diagnostics_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Network Latency, Throttling & DPI Interception Summary Card
        item {
            LatencySummaryDashboardCard(
                config = config,
                testedResults = testedResults,
                totalTargetsCount = diagnosticTargets.size,
                isRunningBatchTest = isRunningBatchTest,
                customDomainInput = customDomainInput,
                onCustomDomainInputChange = onCustomDomainInputChange,
                onAddAndTestCustomDomain = onAddAndTestCustomDomain,
                onRunAllDiagnostics = onRunAllDiagnostics
            )
        }

        // 2. Popular Services Latency & Throttling Probe Cards
        items(diagnosticTargets, key = { it.domain }) { item ->
            DiagnosticResultCard(
                result = item,
                onRetest = { onTestSingleTarget(item.domain, item.category) }
            )
        }

        // 3. DNS Port Redirection Selector Card
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
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
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "DNS Port Yönlendirme",
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Text(
                                text = "${config.dnsAddress}:${config.dnsPort}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    TURKEY_DNS_PRESETS.forEach { preset ->
                        val isSelected = config.dnsAddress == preset.ipv4 && config.dnsPort == preset.port
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectDnsPreset(preset) }
                                .testTag("dns_preset_${preset.ipv4}_${preset.port}"),
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = preset.name,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(19.dp)
                                )

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = preset.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${preset.ipv4}:${preset.port}",
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = preset.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showCustomDnsFields = !showCustomDnsFields },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (showCustomDnsFields) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showCustomDnsFields) "Manuel DNS Girişini Gizle" else "Manuel IPv4 / IPv6 & Port Gir"
                        )
                    }

                    if (showCustomDnsFields) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = config.dnsAddress,
                                onValueChange = { ip -> onUpdateConfig { it.copy(dnsAddress = ip.trim()) } },
                                label = { Text("IPv4 DNS (--dns-addr)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("input_dns_ipv4")
                            )
                            OutlinedTextField(
                                value = dnsPortText,
                                onValueChange = { p ->
                                    val digitsOnly = p.filter { it.isDigit() }.take(5)
                                    dnsPortText = digitsOnly
                                    val portNum = digitsOnly.toIntOrNull()
                                    if (portNum != null && portNum in 1..65535) {
                                        onUpdateConfig { it.copy(dnsPort = portNum) }
                                    }
                                },
                                label = { Text("Port") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier
                                    .weight(0.8f)
                                    .testTag("input_dns_port")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = config.dnsV6Address,
                                onValueChange = { ip6 -> onUpdateConfig { it.copy(dnsV6Address = ip6.trim()) } },
                                label = { Text("IPv6 DNS (--dnsv6-addr)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1.4f)
                                    .testTag("input_dns_ipv6")
                            )
                            OutlinedTextField(
                                value = dnsV6PortText,
                                onValueChange = { p ->
                                    val digitsOnly = p.filter { it.isDigit() }.take(5)
                                    dnsV6PortText = digitsOnly
                                    val portNum = digitsOnly.toIntOrNull()
                                    if (portNum != null && portNum in 1..65535) {
                                        onUpdateConfig { it.copy(dnsV6Port = portNum) }
                                    }
                                },
                                label = { Text("v6 Port") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.8f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LatencySummaryDashboardCard(
    config: DpiConfig,
    testedResults: List<DiagnosticTargetResult>,
    totalTargetsCount: Int,
    isRunningBatchTest: Boolean,
    customDomainInput: String,
    onCustomDomainInputChange: (String) -> Unit,
    onAddAndTestCustomDomain: () -> Unit,
    onRunAllDiagnostics: () -> Unit
) {
    val overallAvgPing = remember(testedResults) {
        val pings = testedResults.mapNotNull { it.avgPingMs }
        if (pings.isNotEmpty()) pings.sum() / pings.size else null
    }
    val overallAvgJitter = remember(testedResults) {
        val jitters = testedResults.mapNotNull { it.jitterMs }
        if (jitters.isNotEmpty()) jitters.sum() / jitters.size else null
    }
    val throttledOrInterceptedCount = remember(testedResults) {
        testedResults.count {
            it.throttlingVerdict == ThrottlingVerdict.THROTTLED ||
                it.throttlingVerdict == ThrottlingVerdict.INTERCEPTED
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("latency_diagnostic_summary_card"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.NetworkPing,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "Ağ Gecikmesi (Ping) & Throttling Teşhisi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Popüler servislere 4x TCP/TLS ping göndererek yavaşlatma ve DPI müdahalesini ölçer",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 3-Column Monochrome Summary Metrics Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SummaryMetricTile(
                    label = "ORT. GECİKME",
                    value = if (overallAvgPing != null) "$overallAvgPing ms" else "-- ms",
                    subtext = if (overallAvgJitter != null) "Jitter: ±${overallAvgJitter}ms" else "4x Örneklem",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricTile(
                    label = "BANT DARALTMA",
                    value = when {
                        testedResults.isEmpty() -> "ÖLÇÜLMEDİ"
                        throttledOrInterceptedCount == 0 -> "YOK (TEMİZ)"
                        else -> "$throttledOrInterceptedCount UYARI"
                    },
                    subtext = if (testedResults.isEmpty()) "${totalTargetsCount} Servis Hazır"
                    else "${testedResults.size}/$totalTargetsCount Test Edildi",
                    modifier = Modifier.weight(1f)
                )
                SummaryMetricTile(
                    label = "AKTİF HAT",
                    value = "Mod ${config.desyncMode.flag}",
                    subtext = "DNS :${config.dnsPort} • -e ${config.httpsFragmentSize}B",
                    modifier = Modifier.weight(1f)
                )
            }

            Button(
                onClick = onRunAllDiagnostics,
                enabled = !isRunningBatchTest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("btn_run_all_diagnostics"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isRunningBatchTest) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRunningBatchTest) {
                        "Tüm Servisler Pingleniyor & Analiz Ediliyor..."
                    } else {
                        "Tüm Popüler Servisleri Pingle & Test Et"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            // Custom Domain Ping Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = customDomainInput,
                    onValueChange = onCustomDomainInputChange,
                    label = { Text("Özel servis pingle (örn: twitch.tv)") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("input_custom_test_domain")
                )
                Button(
                    onClick = onAddAndTestCustomDomain,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("btn_add_custom_domain"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Ekle ve Pingle")
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.background,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DiagnosticResultCard(
    result: DiagnosticTargetResult,
    onRetest: () -> Unit
) {
    val isNormal = result.throttlingVerdict == ThrottlingVerdict.NORMAL && result.isReachable == true

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("diag_card_${result.domain}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = if (result.isReachable != null) 1.2.dp else 1.dp,
            color = if (isNormal) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = result.domain,
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (result.avgPingMs != null) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = "${result.avgPingMs} ms",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = result.category,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (result.isTesting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    IconButton(
                        onClick = onRetest,
                        modifier = Modifier
                            .size(44.dp)
                            .testTag("btn_test_${result.domain}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "${result.domain} Pingle ve Test Et",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (result.isReachable != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Verdict Header + Jitter / Packet Loss
                        val verdict = result.throttlingVerdict ?: ThrottlingVerdict.NORMAL
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = when (verdict) {
                                        ThrottlingVerdict.NORMAL -> Icons.Default.CheckCircle
                                        ThrottlingVerdict.THROTTLED -> Icons.Default.WarningAmber
                                        ThrottlingVerdict.INTERCEPTED -> Icons.Default.ErrorOutline
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = verdict.labelTr,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                text = "Jitter: ±${result.jitterMs ?: 1}ms • Kayıp: %${result.packetLossPercent}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Latency Breakdown Bar Chart (DNS vs TCP Ping Samples vs TLS SNI Handshake)
                        LatencyBreakdownSparkBars(
                            dnsMs = result.dnsLatencyMs ?: 10L,
                            pingSamples = result.tcpPingSamplesMs,
                            tlsMs = result.tlsHandshakeLatencyMs ?: 25L
                        )

                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

                        // Detailed explanation & IP / SNI fragmentation info
                        Text(
                            text = verdict.detailTr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "IP: ${result.resolvedIp ?: "-"}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (result.sniSplitSimulatedBytes != null) {
                                Text(
                                    text = result.sniSplitSimulatedBytes,
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LatencyBreakdownSparkBars(
    dnsMs: Long,
    pingSamples: List<Long>,
    tlsMs: Long
) {
    val primaryColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DNS: ${dnsMs}ms",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor
            )
            if (pingSamples.isNotEmpty()) {
                Text(
                    text = "TCP Ping (4x): ${pingSamples.joinToString("/") { "${it}ms" }}",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelSmall,
                    color = primaryColor
                )
            }
            Text(
                text = "TLS SNI: ${tlsMs}ms",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = secondaryColor
            )
        }

        // Visual Monochrome Multi-Sample Ping Bar Graph
        val allValues = buildList {
            add(dnsMs)
            addAll(pingSamples.ifEmpty { listOf(20L, 22L, 19L, 21L) })
            add(tlsMs)
        }
        val maxVal = (allValues.maxOrNull() ?: 100L).coerceAtLeast(50L).toFloat()

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .testTag("latency_spark_bars_canvas")
        ) {
            val count = allValues.size
            val gap = 6.dp.toPx()
            val totalGap = gap * (count - 1)
            val barWidth = ((size.width - totalGap) / count).coerceAtLeast(4f)

            allValues.forEachIndexed { idx, value ->
                val ratio = (value.toFloat() / maxVal).coerceIn(0.18f, 1f)
                val barH = size.height * ratio
                val x = idx * (barWidth + gap)
                val y = size.height - barH

                val isPingSample = idx in 1..(allValues.size - 2)
                drawRoundRect(
                    color = if (isPingSample) primaryColor else secondaryColor.copy(alpha = 0.55f),
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barH),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }
        }
    }
}
