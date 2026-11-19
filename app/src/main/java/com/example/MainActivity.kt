package com.example

import android.app.Activity
import android.net.VpnService
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.IspPreset
import com.example.ui.components.LiveConnectionStatusBanner
import com.example.ui.components.TopBarConnectionBadge
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DnsDiagnosticsScreen
import com.example.ui.screens.PacketLogsScreen
import com.example.ui.screens.ParametersScreen
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.AppTab
import com.example.viewmodel.GoodbyeDpiViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: GoodbyeDpiViewModel = viewModel()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                GoodbyeDpiTrApp(viewModel = viewModel)
            }
        }
    }
}

private data class NavDestination(
    val tab: AppTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_DESTINATIONS = listOf(
    NavDestination(
        tab = AppTab.DASHBOARD,
        labelRes = R.string.tab_home,
        selectedIcon = Icons.Filled.Shield,
        unselectedIcon = Icons.Outlined.Shield,
        testTag = "nav_tab_dashboard"
    ),
    NavDestination(
        tab = AppTab.PARAMETERS,
        labelRes = R.string.tab_config,
        selectedIcon = Icons.Filled.Tune,
        unselectedIcon = Icons.Outlined.Tune,
        testTag = "nav_tab_parameters"
    ),
    NavDestination(
        tab = AppTab.DNS_DIAGNOSTICS,
        labelRes = R.string.tab_dns_diag,
        selectedIcon = Icons.Filled.Dns,
        unselectedIcon = Icons.Outlined.Dns,
        testTag = "nav_tab_dns_diag"
    ),
    NavDestination(
        tab = AppTab.PACKET_LOGS,
        labelRes = R.string.tab_logs,
        selectedIcon = Icons.Filled.Terminal,
        unselectedIcon = Icons.Outlined.Terminal,
        testTag = "nav_tab_logs"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoodbyeDpiTrApp(
    viewModel: GoodbyeDpiViewModel
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val config by viewModel.dpiConfig.collectAsStateWithLifecycle()
    val telemetry by viewModel.telemetry.collectAsStateWithLifecycle()
    val packetLogs by viewModel.packetLogs.collectAsStateWithLifecycle()
    val diagnosticTargets by viewModel.diagnosticTargets.collectAsStateWithLifecycle()
    val isRunningBatchTest by viewModel.isRunningBatchTest.collectAsStateWithLifecycle()
    val customTestDomain by viewModel.customTestDomain.collectAsStateWithLifecycle()
    val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.uiNotice.collect { msg ->
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
        }
    }

    val vpnPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.startVpnService(context)
        } else {
            viewModel.startVpnService(context)
        }
    }

    val handleToggleProtection: () -> Unit = {
        if (telemetry.isActive) {
            viewModel.stopVpnService(context)
        } else {
            val prepareIntent = runCatching { VpnService.prepare(context) }.getOrNull()
            if (prepareIntent != null) {
                try {
                    vpnPermissionLauncher.launch(prepareIntent)
                } catch (_: Exception) {
                    viewModel.startVpnService(context)
                }
            } else {
                viewModel.startVpnService(context)
            }
        }
    }

    if (selectedTab != AppTab.DASHBOARD) {
        BackHandler {
            viewModel.selectTab(AppTab.DASHBOARD)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                                ) {
                                    Text(
                                        text = "TR v0.2.3",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = stringResource(R.string.subtitle_turkey_edition),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // Real-time interactive top bar connection status indicator
                        TopBarConnectionBadge(
                            telemetry = telemetry,
                            config = config,
                            onToggleConnection = handleToggleProtection,
                            modifier = Modifier.padding(end = 4.dp)
                        )

                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("btn_toggle_theme")
                        ) {
                            Icon(
                                imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Temayı Değiştir"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 0.dp
                    ) {
                        NAV_DESTINATIONS.forEach { dest ->
                            val selected = selectedTab == dest.tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(dest.labelRes),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NAV_DESTINATIONS.forEach { dest ->
                            val selected = selectedTab == dest.tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = stringResource(dest.labelRes)
                                    )
                                },
                                label = { Text(stringResource(dest.labelRes)) },
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val contentModifier = Modifier.widthIn(max = 760.dp)

                    // Real-time visual connection status indicator banner across all tabs
                    Box(
                        modifier = contentModifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        LiveConnectionStatusBanner(
                            telemetry = telemetry,
                            config = config,
                            onToggleConnection = handleToggleProtection
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Crossfade(
                            targetState = selectedTab,
                            animationSpec = tween(durationMillis = 160),
                            label = "tab_crossfade"
                        ) { tab ->
                            when (tab) {
                                AppTab.DASHBOARD -> DashboardScreen(
                                    config = config,
                                    telemetry = telemetry,
                                    onToggleProtection = handleToggleProtection,
                                    onSelectPreset = { viewModel.applyIspPreset(it) },
                                    onApplyTtlFix = { viewModel.applyTtlDisconnectFix() },
                                    onSetTtlValue = { newTtl ->
                                        viewModel.updateConfig(debounceMs = 0L) {
                                            it.copy(useSetTtl = true, ttlValue = newTtl)
                                        }
                                        viewModel.showNotice("--set-ttl $newTtl olarak ayarlandı")
                                    },
                                    onNavigateToParameters = { viewModel.selectTab(AppTab.PARAMETERS) },
                                    onNavigateToDiagnostics = { viewModel.selectTab(AppTab.DNS_DIAGNOSTICS) },
                                    onResetCounters = { viewModel.resetCounters() },
                                    onCommandCopied = { viewModel.showNotice("GoodbyeDPI komutu panoya kopyalandı") },
                                    modifier = contentModifier
                                )

                                AppTab.PARAMETERS -> ParametersScreen(
                                    config = config,
                                    onSelectDesyncMode = { viewModel.updateDesyncMode(it) },
                                    onUpdateConfig = { transform -> viewModel.updateConfig(transform = transform) },
                                    onResetToTurkeyDefault = {
                                        viewModel.applyIspPreset(IspPreset.TURKEY_STANDARD)
                                    },
                                    onCommandCopied = { viewModel.showNotice("GoodbyeDPI komutu panoya kopyalandı") },
                                    modifier = contentModifier
                                )

                                AppTab.DNS_DIAGNOSTICS -> DnsDiagnosticsScreen(
                                    config = config,
                                    diagnosticTargets = diagnosticTargets,
                                    isRunningBatchTest = isRunningBatchTest,
                                    customDomainInput = customTestDomain,
                                    onCustomDomainInputChange = { viewModel.updateCustomTestDomain(it) },
                                    onAddAndTestCustomDomain = { viewModel.addAndTestCustomDomain() },
                                    onSelectDnsPreset = { viewModel.applyDnsPreset(it) },
                                    onUpdateConfig = { transform -> viewModel.updateConfig(transform = transform) },
                                    onTestSingleTarget = { dom, cat -> viewModel.testSingleTarget(dom, cat) },
                                    onRunAllDiagnostics = { viewModel.runAllDiagnostics() },
                                    modifier = contentModifier
                                )

                                AppTab.PACKET_LOGS -> PacketLogsScreen(
                                    logs = packetLogs,
                                    onClearLogs = { viewModel.clearLogs() },
                                    onLogsCopied = { viewModel.showNotice("Tüm kayıtlar panoya kopyalandı") },
                                    modifier = contentModifier
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
