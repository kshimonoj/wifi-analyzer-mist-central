package com.kshimono.wifianalyzer.ui.scan

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiStatusbarNull
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kshimono.wifianalyzer.domain.model.BssidSummary
import com.kshimono.wifianalyzer.ui.snapshot.SaveSnapshotDialog
import com.kshimono.wifianalyzer.ui.snapshot.SnapshotViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel:           ScanViewModel     = hiltViewModel(),
    snapshotViewModel:   SnapshotViewModel = hiltViewModel(),
    onNavigateToCompare: (String) -> Unit  = {},
) {
    val filteredResults  by viewModel.filteredResults.collectAsStateWithLifecycle()
    val scanResults      by viewModel.scanResults.collectAsStateWithLifecycle()
    val isScanning       by viewModel.isScanning.collectAsStateWithLifecycle()
    val throttled        by viewModel.scanThrottled.collectAsStateWithLifecycle()
    val filterEssid      by viewModel.filterEssid.collectAsStateWithLifecycle()
    val filterBand       by viewModel.filterBand.collectAsStateWithLifecycle()
    val filterSecurity   by viewModel.filterSecurity.collectAsStateWithLifecycle()
    val sortOrder        by viewModel.sortOrder.collectAsStateWithLifecycle()
    val autoScanEnabled  by viewModel.autoScanEnabled.collectAsStateWithLifecycle()
    val autoScanInterval by viewModel.autoScanInterval.collectAsStateWithLifecycle()

    var showSaveDialog by remember { mutableStateOf(false) }
    var selectedBssids by remember { mutableStateOf(emptySet<String>()) }

    if (showSaveDialog) {
        SaveSnapshotDialog(
            onSave = { name, location, floor, note ->
                snapshotViewModel.saveSnapshot(name, location, floor, note, filteredResults)
                showSaveDialog = false
            },
            onCancel = { showSaveDialog = false },
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        if (selectedBssids.isNotEmpty()) {
                            Text("${selectedBssids.size} selected")
                        } else {
                            Text("Wi-Fi Analyzer")
                        }
                    },
                    actions = {
                        if (selectedBssids.isNotEmpty()) {
                            IconButton(onClick = {
                                val encoded = selectedBssids.joinToString(",")
                                selectedBssids = emptySet()
                                onNavigateToCompare(encoded)
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.CompareArrows,
                                    contentDescription = "Compare RSSI",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            IconButton(onClick = { selectedBssids = emptySet() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear selection")
                            }
                        } else {
                            IconButton(
                                onClick  = { showSaveDialog = true },
                                enabled  = filteredResults.isNotEmpty(),
                            ) {
                                Icon(
                                    imageVector        = Icons.Filled.Save,
                                    contentDescription = "Save snapshot",
                                    tint               = if (filteredResults.isNotEmpty())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(onClick = viewModel::toggleAutoScan) {
                                Icon(
                                    imageVector = Icons.Filled.Loop,
                                    contentDescription = "Auto scan",
                                    tint = if (autoScanEnabled)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    },
                )
                if (autoScanEnabled && selectedBssids.isEmpty()) {
                    AutoScanIntervalRow(
                        selected = autoScanInterval,
                        onSelect = viewModel::setAutoScanInterval,
                    )
                }
            }
        },
        floatingActionButton = {
            if (!autoScanEnabled && selectedBssids.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { if (!isScanning) viewModel.startScan() },
                    icon = {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier    = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color       = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null)
                        }
                    },
                    text = { Text(if (isScanning) "Scanning…" else "Scan") },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {

            FilterRow(
                essid      = filterEssid,
                band       = filterBand,
                security   = filterSecurity,
                sort       = sortOrder,
                onEssid    = viewModel::setFilterEssid,
                onBand     = viewModel::setFilterBand,
                onSecurity = viewModel::setFilterSecurity,
                onSort     = viewModel::setSortOrder,
            )

            if (throttled) {
                ThrottleBanner()
            }

            when {
                filteredResults.isEmpty() && isScanning -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                filteredResults.isEmpty() -> {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                Icons.Filled.Wifi,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint     = MaterialTheme.colorScheme.outline,
                            )
                            Text(
                                text  = if (scanResults.isNotEmpty()) "No BSSIDs match the current filter"
                                        else "Press Scan to start",
                                color = MaterialTheme.colorScheme.outline,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier        = Modifier.weight(1f),
                        contentPadding  = PaddingValues(
                            start  = 8.dp,
                            end    = 8.dp,
                            top    = 4.dp,
                            bottom = if (!autoScanEnabled) 88.dp else 8.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(
                            filteredResults,
                            key = { "${it.observation.bssid}/${it.observation.ssid}" },
                        ) { summary ->
                            BssidCard(
                                summary    = summary,
                                selected   = summary.observation.bssid in selectedBssids,
                                onLongPress = {
                                    val bssid = summary.observation.bssid
                                    selectedBssids = if (bssid in selectedBssids)
                                        selectedBssids - bssid
                                    else
                                        selectedBssids + bssid
                                },
                            )
                        }
                    }
                }
            }

            // Footer
            Surface(
                color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text     = "${scanResults.size} BSSIDs (${filteredResults.size} shown)" +
                               if (isScanning) " — scanning…" else "",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ── Auto-scan interval chips ────────────────────────────────────────────────

@Composable
private fun AutoScanIntervalRow(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        Text("Interval:", style = MaterialTheme.typography.labelSmall)
        listOf(5, 10, 30).forEach { sec ->
            FilterChip(
                selected = selected == sec,
                onClick  = { onSelect(sec) },
                label    = { Text("${sec}s") },
            )
        }
    }
}

// ── Filter row ──────────────────────────────────────────────────────────────

@Composable
private fun FilterRow(
    essid:      String,
    band:       String,
    security:   String,
    sort:       String,
    onEssid:    (String) -> Unit,
    onBand:     (String) -> Unit,
    onSecurity: (String) -> Unit,
    onSort:     (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value         = essid,
            onValueChange = onEssid,
            placeholder   = { Text("Search SSID", style = MaterialTheme.typography.bodySmall) },
            singleLine    = true,
            modifier      = Modifier.width(140.dp),
            textStyle     = MaterialTheme.typography.bodySmall,
        )
        FilterDropdown("Band",     band,     listOf("All", "2.4 GHz", "5 GHz", "6 GHz"), onBand)
        FilterDropdown("Security", security, listOf("All", "WPA3", "WPA2", "WPA", "Open"), onSecurity)
        FilterDropdown("Sort",     sort,     listOf("RSSI", "SSID", "Channel"), onSort)
    }
}

@Composable
private fun FilterDropdown(
    label:    String,
    selected: String,
    options:  List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick          = { expanded = true },
            contentPadding   = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text  = if (selected == "All") label else "$label: $selected",
                style = MaterialTheme.typography.labelMedium,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text          = { Text(option) },
                    onClick       = { onSelect(option); expanded = false },
                    leadingIcon   = if (option == selected) {
                        { Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp)) }
                    } else null,
                )
            }
        }
    }
}

// ── BSSID card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BssidCard(
    summary:     BssidSummary,
    selected:    Boolean,
    onLongPress: () -> Unit,
) {
    val obs  = summary.observation
    val band = summary.band

    val cardColors = if (selected)
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    else
        CardDefaults.cardColors()

    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick     = {},
                onLongClick = onLongPress,
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors    = cardColors,
    ) {
        Column(
            modifier            = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Row 1: signal icon + SSID + RSSI badge
            Row(verticalAlignment = Alignment.CenterVertically) {
                SignalIcon(obs.rssi)
                Spacer(Modifier.width(8.dp))
                Text(
                    text       = obs.ssid.ifBlank { "(hidden)" },
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f),
                )
                RssiBadge(obs.rssi)
            }
            // Row 2: AP name (Mist > Aruba > none) or BSSID + vendor
            val resolvedApName = obs.mistApName ?: obs.arubaApName
            if (resolvedApName != null) {
                Text(
                    text       = resolvedApName,
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text       = obs.bssid,
                    style      = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color      = MaterialTheme.colorScheme.outline,
                )
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = obs.bssid,
                        style      = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color      = MaterialTheme.colorScheme.outline,
                        modifier   = Modifier.weight(1f),
                    )
                    if (obs.vendor.isNotEmpty() && obs.vendor != "Unknown") {
                        Text(
                            text  = obs.vendor,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            // Row 3: chips
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                BandChip(band)
                InfoChip("Ch ${obs.channel}")
                InfoChip(obs.channelWidth)
                InfoChip(obs.security)
            }
        }
    }
}

@Composable
private fun SignalIcon(rssi: Int) {
    val iconInfo = when {
        rssi >= -60 -> Icons.Filled.SignalWifi4Bar          to Color(0xFF2E7D32)
        rssi >= -70 -> Icons.Filled.NetworkWifi             to Color(0xFFF9A825)
        rssi >= -80 -> Icons.Filled.NetworkWifi3Bar         to Color(0xFFE65100)
        else        -> Icons.Filled.SignalWifiStatusbarNull to MaterialTheme.colorScheme.error
    }
    Icon(iconInfo.first, contentDescription = "$rssi dBm", tint = iconInfo.second, modifier = Modifier.size(22.dp))
}

@Composable
private fun RssiBadge(rssi: Int) {
    val color = when {
        rssi >= -60 -> Color(0xFF2E7D32)
        rssi >= -75 -> MaterialTheme.colorScheme.secondary
        else        -> MaterialTheme.colorScheme.error
    }
    Text(
        text       = "$rssi dBm",
        style      = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color      = color,
    )
}

@Composable
private fun BandChip(band: String) {
    val (bg, fg) = when (band) {
        "2.4 GHz" -> Color(0xFF1565C0) to Color.White
        "5 GHz"   -> Color(0xFF2E7D32) to Color.White
        "6 GHz"   -> Color(0xFF6A1B9A) to Color.White
        else      -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(shape = MaterialTheme.shapes.extraSmall, color = bg) {
        Text(
            text     = band,
            style    = MaterialTheme.typography.labelSmall,
            color    = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun InfoChip(text: String) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text     = text,
            style    = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun ThrottleBanner() {
    Surface(
        color    = Color(0xFFFFF9C4),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text     = "⚠ Scan throttled by OS – showing cached results",
            style    = MaterialTheme.typography.bodySmall,
            color    = Color(0xFF5D4037),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}
