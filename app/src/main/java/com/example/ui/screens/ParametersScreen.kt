package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Restore
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.model.DesyncMode
import com.example.model.DpiConfig
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ParametersScreen(
    config: DpiConfig,
    onSelectDesyncMode: (DesyncMode) -> Unit,
    onUpdateConfig: ((DpiConfig) -> DpiConfig) -> Unit,
    onResetToTurkeyDefault: () -> Unit,
    onCommandCopied: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    // Local float states for buttery-smooth 60fps dragging
    var httpFragSlider by remember { mutableFloatStateOf(config.httpFragmentSize.toFloat()) }
    var httpsFragSlider by remember { mutableFloatStateOf(config.httpsFragmentSize.toFloat()) }
    var maxPayloadSlider by remember { mutableFloatStateOf(config.maxPayload.toFloat()) }

    LaunchedEffect(config.httpFragmentSize) {
        httpFragSlider = config.httpFragmentSize.toFloat()
    }
    LaunchedEffect(config.httpsFragmentSize) {
        httpsFragSlider = config.httpsFragmentSize.toFloat()
    }
    LaunchedEffect(config.maxPayload) {
        maxPayloadSlider = config.maxPayload.toFloat()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("parameters_lazy_column"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Command Line Bar
        item {
            val cmdString = config.toCommandLineString()
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Üretilen GoodbyeDPI Komutu",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(cmdString))
                                onCommandCopied()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Komutu Kopyala",
                                tint = ElectricCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Text(
                            text = cmdString,
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelMedium,
                            color = CyberEmerald,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        )
                    }
                }
            }
        }

        // 2. Desync Mode Selector (-5, -6, -9, -4, Custom)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "DPI Bypass Modu (-4 / -5 / -6 / -9)",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    DesyncMode.entries.forEach { mode ->
                        val selected = config.desyncMode == mode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectDesyncMode(mode) }
                                .testTag("mode_option_${mode.name}"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            } else {
                                MaterialTheme.colorScheme.background
                            },
                            border = BorderStroke(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) ElectricCyan else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selected) ElectricCyan else MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = mode.flag,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.titleTr,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = mode.descriptionTr,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Packet Fragmentation Sizes (-f, -e, --max-payload)
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Paket Parçalama & SNI Boyutları",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    // HTTP Fragment (-f)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HTTP Parçalama (-f)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${httpFragSlider.roundToInt()} Bayt",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        Slider(
                            value = httpFragSlider,
                            onValueChange = { v ->
                                httpFragSlider = v
                                onUpdateConfig { it.copy(httpFragmentSize = v.roundToInt(), desyncMode = DesyncMode.CUSTOM) }
                            },
                            valueRange = 1f..16f,
                            steps = 14,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("slider_http_frag")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // HTTPS / TLS SNI Fragment (-e)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "HTTPS / TLS SNI Parçalama (-e)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${httpsFragSlider.roundToInt()} Bayt",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            listOf(2, 4, 20, 40).forEach { presetBytes ->
                                val isSel = httpsFragSlider.roundToInt() == presetBytes
                                FilterChip(
                                    selected = isSel,
                                    onClick = {
                                        httpsFragSlider = presetBytes.toFloat()
                                        onUpdateConfig { it.copy(httpsFragmentSize = presetBytes) }
                                    },
                                    label = { Text("-e $presetBytes") },
                                    leadingIcon = if (isSel) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                        Slider(
                            value = httpsFragSlider,
                            onValueChange = { v ->
                                httpsFragSlider = v
                                onUpdateConfig { it.copy(httpsFragmentSize = v.roundToInt()) }
                            },
                            valueRange = 2f..80f,
                            colors = SliderDefaults.colors(
                                thumbColor = ElectricCyan,
                                activeTrackColor = ElectricCyan
                            ),
                            modifier = Modifier.testTag("slider_https_frag")
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    // Max Payload (--max-payload)
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Maksimum Yük & MTU (--max-payload)",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = "${((maxPayloadSlider.roundToInt() / 20) * 20)} Bayt",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
                            )
                        }
                        Slider(
                            value = maxPayloadSlider,
                            onValueChange = { v ->
                                maxPayloadSlider = v
                                val rounded = ((v.roundToInt() / 20) * 20).coerceIn(600, 1480)
                                onUpdateConfig { it.copy(maxPayload = rounded) }
                            },
                            valueRange = 600f..1480f,
                            colors = SliderDefaults.colors(
                                thumbColor = CyberEmerald,
                                activeTrackColor = CyberEmerald
                            )
                        )
                    }
                }
            }
        }

        // 4. TTL & Fake Packet Desync Controls
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkPing,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sahte Paket (Fake Packet) & TTL Ayarları",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }

                    ParameterToggleRow(
                        title = "Sabit TTL Kullan (--set-ttl)",
                        flagBadge = "--set-ttl ${config.ttlValue}",
                        description = "Sahte paketin hedef sunucuya ulaşmadan DPI duvarında düşmesini sağlar. Kopma yaparsa kapatın.",
                        checked = config.useSetTtl,
                        onCheckedChange = { enabled ->
                            onUpdateConfig {
                                it.copy(
                                    useSetTtl = enabled,
                                    useAutoTtl = if (enabled) false else it.useAutoTtl
                                )
                            }
                        },
                        testTag = "switch_set_ttl"
                    )

                    if (config.useSetTtl) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            listOf(3, 4, 5, 6, 7, 8, 9).forEach { ttl ->
                                FilterChip(
                                    selected = config.ttlValue == ttl,
                                    onClick = { onUpdateConfig { it.copy(ttlValue = ttl) } },
                                    label = { Text("TTL $ttl") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                        selectedLabelColor = ElectricCyan
                                    ),
                                    modifier = Modifier.testTag("chip_ttl_$ttl")
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    ParameterToggleRow(
                        title = "Otomatik TTL (--auto-ttl)",
                        flagBadge = "--auto-ttl ${config.autoTtlSpec}",
                        description = "Sunucu mesafesine göre sahte paket TTL değerini otomatik ayarlar (--min-ttl ${config.minTtl}).",
                        checked = config.useAutoTtl && !config.useSetTtl,
                        onCheckedChange = { enabled ->
                            onUpdateConfig {
                                it.copy(
                                    useAutoTtl = enabled,
                                    useSetTtl = if (enabled) false else it.useSetTtl
                                )
                            }
                        },
                        testTag = "switch_auto_ttl"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    ParameterToggleRow(
                        title = "SNI Alan Adına Göre Böl",
                        flagBadge = "--frag-by-sni",
                        description = "TLS ClientHello paketinde tam SNI alan adını bulup ortasından böler.",
                        checked = config.fragBySni,
                        onCheckedChange = { enabled ->
                            onUpdateConfig { it.copy(fragBySni = enabled) }
                        },
                        testTag = "switch_frag_by_sni"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    ParameterToggleRow(
                        title = "Ters Sıralı Parçalama",
                        flagBadge = "--native-frag --reverse-frag",
                        description = "Önce 2. parçayı sonra 1. parçayı göndererek DPI durum makinesini atlatır.",
                        checked = config.nativeFragReverse,
                        onCheckedChange = { enabled ->
                            onUpdateConfig { it.copy(nativeFragReverse = enabled) }
                        },
                        testTag = "switch_reverse_frag"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    ParameterToggleRow(
                        title = "Hatalı TCP SEQ Numarası",
                        flagBadge = "--wrong-seq",
                        description = "Sahte paketleri geçmiş TCP Sequence penceresiyle gönderir.",
                        checked = config.wrongSeq,
                        onCheckedChange = { enabled ->
                            onUpdateConfig { it.copy(wrongSeq = enabled) }
                        },
                        testTag = "switch_wrong_seq"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    ParameterToggleRow(
                        title = "Bozuk TCP Checksum",
                        flagBadge = "--wrong-chksum",
                        description = "Geçersiz sağlama toplamlı sahte paket üretir.",
                        checked = config.wrongChksum,
                        onCheckedChange = { enabled ->
                            onUpdateConfig { it.copy(wrongChksum = enabled) }
                        },
                        testTag = "switch_wrong_chksum"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    ParameterToggleRow(
                        title = "UDP QUIC / HTTP3 Engelle",
                        flagBadge = "-q",
                        description = "Tarayıcıları UDP 443 yerine TCP/TLS kullanmaya zorlar.",
                        checked = config.blockQuic,
                        onCheckedChange = { enabled ->
                            onUpdateConfig { it.copy(blockQuic = enabled) }
                        },
                        testTag = "switch_block_quic"
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))

                    ParameterToggleRow(
                        title = "Host Başlığı Harf Karıştır",
                        flagBadge = "-m",
                        description = "HTTP Host başlığını 'hOsT:' şeklinde gönderir.",
                        checked = config.hostMixedCase,
                        onCheckedChange = { enabled ->
                            onUpdateConfig { it.copy(hostMixedCase = enabled) }
                        },
                        testTag = "switch_host_mixed"
                    )
                }
            }
        }

        // 5. Custom Extra Arguments & Reset Button
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Ek Komut Satırı Parametreleri (İsteğe Bağlı)",
                        style = MaterialTheme.typography.titleSmall
                    )
                    OutlinedTextField(
                        value = config.customExtraArgs,
                        onValueChange = { extra ->
                            onUpdateConfig { it.copy(customExtraArgs = extra) }
                        },
                        label = { Text("Örn: --blacklist tr_blacklist.txt -p -r -s") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_custom_extra_args")
                    )

                    OutlinedButton(
                        onClick = onResetToTurkeyDefault,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("btn_reset_turkey_defaults"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Türkiye Varsayılanlarına (turkey_dnsredir.cmd) Sıfırla")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterToggleRow(
    title: String,
    flagBadge: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = flagBadge,
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = ElectricCyan
            ),
            modifier = Modifier.testTag(testTag)
        )
    }
}
