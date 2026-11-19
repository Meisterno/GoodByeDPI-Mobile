package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.model.DpiConfig
import com.example.model.TunnelTelemetry
import com.example.ui.theme.JetBrainsMonoFamily
import kotlinx.coroutines.delay
import kotlin.math.sin

/**
 * Persistent monochrome TopBar connection status badge that updates in real-time
 * with live uptime and animated beacon when the DPI bypass service is active.
 */
@Composable
fun TopBarConnectionBadge(
    telemetry: TunnelTelemetry,
    config: DpiConfig,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "top_beacon_transition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "beacon_alpha"
    )

    val isConnected = telemetry.isActive
    val isConnecting = telemetry.isConnecting

    val containerColor by animateColorAsState(
        targetValue = if (isConnected) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        label = "badge_container_color"
    )

    val contentColor by animateColorAsState(
        targetValue = if (isConnected) {
            MaterialTheme.colorScheme.surface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "badge_content_color"
    )

    Surface(
        shape = CircleShape,
        color = containerColor,
        border = BorderStroke(
            width = 1.dp,
            color = if (isConnected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.outline
        ),
        modifier = modifier
            .clip(CircleShape)
            .clickable { onToggleConnection() }
            .testTag("top_bar_connection_status_badge")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            // Animated live status LED dot
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(10.dp)
            ) {
                if (isConnected || isConnecting) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(contentColor.copy(alpha = pulseAlpha * 0.35f))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(contentColor)
                )
            }

            Text(
                text = when {
                    isConnected -> "BAĞLI • ${config.desyncMode.flag}"
                    isConnecting -> "BAĞLANIYOR..."
                    else -> "BAĞLI DEĞİL"
                },
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
        }
    }
}

/**
 * Detailed real-time visual connection status monitor banner displaying
 * live packet flow wave, connection state (BAĞLI / BAĞLI DEĞİL), uptime counter,
 * and active DPI desync pipeline nodes (Uygulama -> SNI Split -> DNS Port -> İSS).
 */
@Composable
fun LiveConnectionStatusBanner(
    telemetry: TunnelTelemetry,
    config: DpiConfig,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var elapsedSec by remember { mutableLongStateOf(0L) }
    LaunchedEffect(telemetry.isActive, telemetry.connectedSinceEpochMs) {
        if (telemetry.isActive && telemetry.connectedSinceEpochMs > 0L) {
            while (true) {
                elapsedSec = ((System.currentTimeMillis() - telemetry.connectedSinceEpochMs) / 1000L).coerceAtLeast(0L)
                delay(1000L)
            }
        } else {
            elapsedSec = 0L
        }
    }

    val uptimeFormatted = remember(elapsedSec) {
        val hrs = elapsedSec / 3600
        val mins = (elapsedSec % 3600) / 60
        val secs = elapsedSec % 60
        if (hrs > 0) "%02d:%02d:%02d".format(hrs, mins, secs)
        else "%02d:%02d".format(mins, secs)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "packet_wave_transition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val isConnected = telemetry.isActive
    val isConnecting = telemetry.isConnecting
    val primaryTone = MaterialTheme.colorScheme.onSurface
    val mutedTone = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineTone = MaterialTheme.colorScheme.outline

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_connection_status_indicator"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (isConnected) 1.5.dp else 1.dp,
            color = if (isConnected) primaryTone else outlineTone
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: Live Status Indicator + Uptime / Packet Rate + Quick Power Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Animated Concentric Signal Ring Icon
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) primaryTone
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onToggleConnection() }
                            .testTag("status_banner_toggle_icon")
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                            contentDescription = if (isConnected) "Bağlantıyı Kes" else "Bağlan",
                            tint = if (isConnected) MaterialTheme.colorScheme.surface else mutedTone,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = when {
                                    isConnected -> "BAĞLI • DPI BYPASS AKTİF"
                                    isConnecting -> "TÜNEL BAĞLANIYOR..."
                                    else -> "BAĞLI DEĞİL • KORUMA KAPALI"
                                },
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isConnected) primaryTone else mutedTone
                            )
                        }

                        Text(
                            text = if (isConnected) {
                                "Süre: $uptimeFormatted • ${telemetry.totalPacketsInspected} Paket • SNI: ${telemetry.lastDetectedSni}"
                            } else {
                                "Başlatmak için dokunun • Mod ${config.desyncMode.flag} (${config.dnsAddress}:${config.dnsPort})"
                            },
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = mutedTone
                        )
                    }
                }

                // Live Waveform Signal Bars
                Canvas(
                    modifier = Modifier
                        .width(46.dp)
                        .height(24.dp)
                        .testTag("live_signal_waveform_canvas")
                ) {
                    val barCount = 6
                    val spacing = 4.dp.toPx()
                    val totalSpacing = spacing * (barCount - 1)
                    val barWidth = ((size.width - totalSpacing) / barCount).coerceAtLeast(2f)

                    for (i in 0 until barCount) {
                        val normalizedHeight = if (isConnected) {
                            val angle = wavePhase + (i * 0.85f)
                            (0.35f + 0.65f * ((sin(angle) + 1f) / 2f)).coerceIn(0.25f, 1f)
                        } else if (isConnecting) {
                            0.45f
                        } else {
                            0.18f
                        }

                        val barHeight = size.height * normalizedHeight
                        val x = i * (barWidth + spacing)
                        val y = (size.height - barHeight) / 2f

                        drawRoundRect(
                            color = if (isConnected) primaryTone else mutedTone.copy(alpha = 0.4f),
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            // Bottom Row: Real-Time Visual Packet Pipeline (TUN -> SNI Desync -> DNS Port -> İSS)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                PipelineStagePill(
                    label = "TUN:${telemetry.activeMtu}",
                    isActive = isConnected
                )
                PipelineConnectorLine(
                    isActive = isConnected,
                    phase = wavePhase,
                    modifier = Modifier.weight(1f)
                )
                PipelineStagePill(
                    label = "SNI -e ${config.httpsFragmentSize}B",
                    isActive = isConnected
                )
                PipelineConnectorLine(
                    isActive = isConnected,
                    phase = wavePhase,
                    modifier = Modifier.weight(1f)
                )
                PipelineStagePill(
                    label = "DNS :${config.dnsPort}",
                    isActive = isConnected
                )
                PipelineConnectorLine(
                    isActive = isConnected,
                    phase = wavePhase,
                    modifier = Modifier.weight(1f)
                )
                PipelineStagePill(
                    label = if (config.useSetTtl) "TTL=${config.ttlValue}" else "WRONG-SEQ",
                    isActive = isConnected
                )
            }
        }
    }
}

@Composable
private fun PipelineStagePill(
    label: String,
    isActive: Boolean
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isActive) MaterialTheme.colorScheme.surfaceVariant
        else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        )
    }
}

@Composable
private fun PipelineConnectorLine(
    isActive: Boolean,
    phase: Float,
    modifier: Modifier = Modifier
) {
    val activeColor = MaterialTheme.colorScheme.onSurface
    val inactiveColor = MaterialTheme.colorScheme.outline

    Canvas(
        modifier = modifier
            .height(8.dp)
            .padding(horizontal = 2.dp)
    ) {
        val centerY = size.height / 2f
        if (isActive) {
            drawLine(
                color = activeColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 1.8.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(8f, 6f),
                    phase = -phase * 4f
                )
            )
        } else {
            drawLine(
                color = inactiveColor,
                start = Offset(0f, centerY),
                end = Offset(size.width, centerY),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}
