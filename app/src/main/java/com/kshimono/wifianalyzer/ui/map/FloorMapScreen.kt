package com.kshimono.wifianalyzer.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import android.content.Intent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kshimono.wifianalyzer.data.aruba.ArubaRadio
import com.kshimono.wifianalyzer.data.db.entities.ApLocationEntity
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private val apJson = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloorMapScreen(
    viewModel: FloorMapViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
) {
    val floorMaps            by viewModel.floorMaps.collectAsStateWithLifecycle()
    val selectedMap          by viewModel.selectedMap.collectAsStateWithLifecycle()
    val placedSnapshots      by viewModel.placedSnapshots.collectAsStateWithLifecycle()
    val unplacedSnapshots    by viewModel.unplacedSnapshots.collectAsStateWithLifecycle()
    val selectedForPlacement by viewModel.selectedForPlacement.collectAsStateWithLifecycle()
    val heatmapEnabled       by viewModel.heatmapEnabled.collectAsStateWithLifecycle()
    val apLocations              by viewModel.apLocations.collectAsStateWithLifecycle()
    val showAps                  by viewModel.showAps.collectAsStateWithLifecycle()
    val apSyncStatus             by viewModel.apSyncStatus.collectAsStateWithLifecycle()
    val exportStatus             by viewModel.exportStatus.collectAsStateWithLifecycle()
    val showConnectionLines      by viewModel.showConnectionLines.collectAsStateWithLifecycle()
    val snapshotsWithConnection  by viewModel.snapshotsWithConnection.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(exportStatus) {
        if (exportStatus is ExportStatus.Done) {
            val uri = (exportStatus as ExportStatus.Done).uri
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/zip"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share Survey Export"))
            viewModel.clearExportStatus()
        }
    }

    var mapDropdownExpanded   by remember { mutableStateOf(false) }
    var selectedAp           by remember { mutableStateOf<ApLocationEntity?>(null) }
    var deleteRequest        by remember { mutableStateOf<Pair<Long, String>?>(null) }
    var showClearDialog      by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.reloadSelectedMap() }
    LaunchedEffect(selectedMap?.id) { selectedAp = null }

    // Clear placements confirmation dialog
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Placements") },
            text  = { Text("Remove all ${placedSnapshots.size} snapshot placements from this map?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeAllSnapshotsFromMap()
                    showClearDialog = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    // Delete confirmation dialog
    deleteRequest?.let { (id, name) ->
        AlertDialog(
            onDismissRequest = { deleteRequest = null },
            title = { Text("Remove Snapshot") },
            text  = { Text("Remove \"$name\" from map?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSnapshotFromMap(id)
                    deleteRequest = null
                }) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { deleteRequest = null }) { Text("Cancel") }
            },
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .statusBarsPadding()
            ) {
                // 1行目: マップ名 + Sync ステータス
                Text(
                    text       = selectedMap?.name ?: "Floor Map",
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                    modifier   = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = if (apSyncStatus.isNotEmpty()) 2.dp else 6.dp),
                )
                if (apSyncStatus.isNotEmpty()) {
                    Text(
                        text     = apSyncStatus,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = when {
                            apSyncStatus.startsWith("✓") -> MaterialTheme.colorScheme.primary
                            apSyncStatus.startsWith("✗") -> MaterialTheme.colorScheme.error
                            else                          -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 2.dp),
                    )
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)

                // 2行目: 全ボタン・アイコン
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // マップ選択 dropdown
                    Box {
                        OutlinedButton(
                            onClick        = { mapDropdownExpanded = true },
                            enabled        = floorMaps.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier       = Modifier.height(36.dp),
                        ) {
                            Icon(Icons.Filled.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Select", style = MaterialTheme.typography.labelSmall)
                            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(
                            expanded         = mapDropdownExpanded,
                            onDismissRequest = { mapDropdownExpanded = false },
                        ) {
                            floorMaps.forEach { map ->
                                DropdownMenuItem(
                                    text    = { Text(map.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = {
                                        viewModel.selectMap(map.id)
                                        mapDropdownExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    // Heatmap トグル
                    IconButton(
                        onClick = viewModel::toggleHeatmap,
                        modifier = Modifier.size(40.dp),
                        colors  = if (heatmapEnabled)
                            IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        else IconButtonDefaults.iconButtonColors(),
                    ) { Icon(Icons.Filled.Grain, contentDescription = "Heatmap", modifier = Modifier.size(22.dp)) }

                    // AP 表示トグル
                    IconButton(
                        onClick  = viewModel::toggleShowAps,
                        modifier = Modifier.size(40.dp),
                        colors   = if (showAps)
                            IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        else IconButtonDefaults.iconButtonColors(),
                    ) { Icon(Icons.Filled.Router, contentDescription = "Show APs", modifier = Modifier.size(22.dp)) }

                    // 接続線トグル
                    IconButton(
                        onClick  = viewModel::toggleConnectionLines,
                        modifier = Modifier.size(40.dp),
                        colors   = if (showConnectionLines)
                            IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                        else IconButtonDefaults.iconButtonColors(),
                    ) { Icon(Icons.Filled.Link, contentDescription = "Connection lines", modifier = Modifier.size(22.dp)) }

                    // Sync APs (Mist/Aruba のみ)
                    if (selectedMap != null && selectedMap!!.source != "local") {
                        TextButton(
                            onClick        = viewModel::syncApLocations,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier       = Modifier.height(36.dp),
                        ) { Text("Sync", style = MaterialTheme.typography.labelSmall) }
                    }

                    // Clear placements
                    if (selectedMap != null && placedSnapshots.isNotEmpty()) {
                        TextButton(
                            onClick        = { showClearDialog = true },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            modifier       = Modifier.height(36.dp),
                        ) { Text("Clear", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error) }
                    }

                    // Export
                    if (selectedMap != null) {
                        IconButton(
                            onClick  = viewModel::exportSurvey,
                            enabled  = exportStatus !is ExportStatus.Working,
                            modifier = Modifier.size(40.dp),
                        ) {
                            if (exportStatus is ExportStatus.Working) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Filled.Download, contentDescription = "Export", modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }

                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {

            // ── Main map area ───────────────────────────────────────────────
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                if (floorMaps.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Map,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint     = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "No floor maps. Import a map in Settings.",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onNavigateToSettings) { Text("Go to Settings") }
                    }
                } else if (selectedMap == null) {
                    Text(
                        "Select a floor map above",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                } else {
                    MapCanvas(
                        map                     = selectedMap!!,
                        placedSnapshots         = placedSnapshots,
                        selectedForPlacement    = selectedForPlacement,
                        heatmapEnabled          = heatmapEnabled,
                        apLocations             = if (showAps) apLocations else emptyList(),
                        showAps                 = showAps,
                        showConnectionLines     = showConnectionLines,
                        snapshotsWithConnection = snapshotsWithConnection,
                        onPlace                 = { rx, ry -> viewModel.placeSnapshot(rx, ry) },
                        onTapAp                 = { ap -> selectedAp = if (selectedAp?.id == ap.id) null else ap },
                        onLongPressPin          = { id, name -> deleteRequest = id to name },
                        onDragEnd               = { id, relX, relY -> viewModel.updateSnapshotPosition(id, relX, relY) },
                    )
                }

                selectedAp?.let { ap ->
                    ApDetailCard(
                        ap        = ap,
                        onDismiss = { selectedAp = null },
                        modifier  = Modifier.align(Alignment.TopStart).padding(8.dp),
                    )
                }
            }

            // ── Bottom panel: all snapshots (placed grayed out) ─────────────
            if (selectedMap != null) {
                Surface(
                    color    = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        if (selectedForPlacement != null) {
                            Text(
                                "Tap on map to place: ${selectedForPlacement!!.name}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
                        }

                        val placedIds = remember(placedSnapshots) {
                            placedSnapshots.map { it.snapshot.id }.toSet()
                        }
                        // Unplaced first, then placed (grayed out)
                        val allPanelItems = remember(unplacedSnapshots, placedSnapshots) {
                            unplacedSnapshots + placedSnapshots.map { it.snapshot }
                        }

                        if (allPanelItems.isEmpty()) {
                            Text(
                                "No snapshots",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                allPanelItems.forEach { snap ->
                                    val isPlaced   = snap.id in placedIds
                                    val isSelected = snap.id == selectedForPlacement?.id
                                    FilterChip(
                                        selected = isSelected && !isPlaced,
                                        enabled  = !isPlaced,
                                        onClick  = {
                                            if (!isPlaced) {
                                                viewModel.selectForPlacement(
                                                    if (isSelected) null else snap
                                                )
                                            }
                                        },
                                        colors = if (isPlaced) FilterChipDefaults.filterChipColors(
                                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                            disabledLabelColor     = MaterialTheme.colorScheme.outline,
                                        ) else FilterChipDefaults.filterChipColors(),
                                        label = {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        snap.name,
                                                        style      = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.SemiBold,
                                                    )
                                                    if (isPlaced) {
                                                        Spacer(Modifier.width(4.dp))
                                                        Text(
                                                            "Placed",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.outline,
                                                        )
                                                    }
                                                }
                                                Text(
                                                    SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
                                                        .format(Date(snap.timestamp)),
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── AP detail popup ──────────────────────────────────────────────────────────

@Composable
private fun ApDetailCard(
    ap: ApLocationEntity,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val radios = remember(ap.radiosJson) {
        ap.radiosJson?.let { jsonStr ->
            runCatching { apJson.decodeFromString<List<ArubaRadio>>(jsonStr) }.getOrNull()
        } ?: emptyList()
    }

    Card(
        modifier  = modifier.widthIn(max = 280.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Text(
                    ap.apName,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier   = Modifier.weight(1f),
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = "Close", modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(4.dp))
            ap.model?.let { Text("Model: $it", style = MaterialTheme.typography.bodySmall) }
            ap.status?.let {
                val statusColor = when (it.uppercase()) {
                    "ONLINE"  -> Color(0xFF27AE60)
                    "OFFLINE" -> Color(0xFF7F8C8D)
                    else      -> MaterialTheme.colorScheme.onSurface
                }
                Text("Status: $it", style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
            ap.macAddress?.let { Text("MAC: $it", style = MaterialTheme.typography.bodySmall) }
            if (radios.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                radios.forEach { radio ->
                    Text(
                        "${radio.band ?: "?"}: Ch ${radio.channel ?: "?"} / ${radio.power ?: "?"}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

// ── Map canvas with gestures ─────────────────────────────────────────────────

private fun matchApByBssidOrName(
    ap: ApLocationEntity,
    connectedBssid: String?,
    connectedApName: String?,
): Boolean {
    if (connectedApName != null && ap.apName == connectedApName) return true
    if (connectedBssid != null) {
        val bssidPrefix = connectedBssid.replace(":", "").lowercase().take(10)
        val apMacPrefix = ap.macAddress?.replace(":", "")?.lowercase()?.take(10)
        if (apMacPrefix != null && apMacPrefix == bssidPrefix) return true
    }
    return false
}

// ── Coordinate frame (image-relative, letterbox-aware) ───────────────────────
// ContentScale.Fit leaves padding when the canvas aspect differs from the image.
// Both AP and snapshot coordinates are stored as 0..1 relative to the IMAGE (not
// the canvas), so all pixel conversions go through this same image rect. Keeping
// snapshots in the image frame makes the exported map_x_relative consistent with
// AP map_x, so the web analyzer renders them at the matching position.
private data class ImgRect(val left: Float, val top: Float, val width: Float, val height: Float)

private fun mapImageRect(canvasW: Float, canvasH: Float, mapWpx: Int, mapHpx: Int): ImgRect {
    val aspect = if (mapWpx > 0 && mapHpx > 0) mapWpx.toFloat() / mapHpx.toFloat() else 1f
    return if (canvasW / canvasH > aspect) {
        val h = canvasH
        val w = h * aspect
        ImgRect((canvasW - w) / 2f, 0f, w, h)
    } else {
        val w = canvasW
        val h = w / aspect
        ImgRect(0f, (canvasH - h) / 2f, w, h)
    }
}

// image-relative (0..1) → screen pixel, along one axis (matches AP rendering)
private fun relToScreen(rel: Float, imgStart: Float, imgSize: Float, canvasSize: Float, scale: Float, off: Float): Float =
    (imgStart + rel * imgSize - canvasSize / 2f) * scale + canvasSize / 2f + off

// screen pixel → image-relative (0..1), along one axis (inverse of relToScreen)
private fun screenToRel(px: Float, imgStart: Float, imgSize: Float, canvasSize: Float, scale: Float, off: Float): Float =
    ((px - off - canvasSize / 2f) / scale + canvasSize / 2f - imgStart) / imgSize

@Composable
private fun MapCanvas(
    map: com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity,
    placedSnapshots: List<SnapshotWithPosition>,
    selectedForPlacement: com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity?,
    heatmapEnabled: Boolean,
    apLocations: List<ApLocationEntity>,
    showAps: Boolean,
    showConnectionLines: Boolean,
    snapshotsWithConnection: List<SnapshotWithPosition>,
    onPlace: (relX: Float, relY: Float) -> Unit,
    onTapAp: (ApLocationEntity) -> Unit,
    onLongPressPin: (snapshotId: Long, name: String) -> Unit,
    onDragEnd: (snapshotId: Long, newRelX: Float, newRelY: Float) -> Unit,
) {
    var scale  by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Dragging state (triggers recompose for real-time visual feedback)
    var draggingId  by remember { mutableStateOf<Long?>(null) }
    var draggingPos by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            // Gesture 1: Pan / Zoom — suppressed while dragging a pin
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    if (draggingId == null) {
                        scale  = (scale * zoom).coerceIn(0.5f, 5f)
                        offset += pan
                    }
                }
            }
            // Gesture 2: Tap — place snapshot or tap AP
            .pointerInput(selectedForPlacement, showAps, apLocations) {
                detectTapGestures { tapOffset ->
                    val cW = size.width.toFloat()
                    val cH = size.height.toFloat()
                    // Image display rect (ContentScale.Fit) — snapshots & APs share this frame
                    val rect = mapImageRect(cW, cH, map.widthPx, map.heightPx)
                    if (selectedForPlacement != null) {
                        // Store snapshot position relative to the IMAGE (not the canvas),
                        // matching AP coordinates so exports line up in the web analyzer.
                        val relX = screenToRel(tapOffset.x, rect.left, rect.width, cW, scale, offset.x).coerceIn(0f, 1f)
                        val relY = screenToRel(tapOffset.y, rect.top, rect.height, cH, scale, offset.y).coerceIn(0f, 1f)
                        onPlace(relX, relY)
                    } else if (showAps) {
                        val tapRadius = maxOf(24f, (12f * scale).coerceIn(16f, 48f) * 1.5f)
                        val tapped = apLocations.firstOrNull { ap ->
                            val ax = relToScreen(ap.mapX, rect.left, rect.width, cW, scale, offset.x)
                            val ay = relToScreen(ap.mapY, rect.top, rect.height, cH, scale, offset.y)
                            val dx = tapOffset.x - ax
                            val dy = tapOffset.y - ay
                            dx * dx + dy * dy <= tapRadius * tapRadius
                        }
                        if (tapped != null) onTapAp(tapped)
                    }
                }
            }
            // Gesture 3: Long press + optional drag — delete (no movement) or reposition (drag)
            .pointerInput(placedSnapshots) {
                var totalDrag = Offset.Zero
                detectDragGesturesAfterLongPress(
                    onDragStart = { startOffset ->
                        totalDrag = Offset.Zero
                        val cW = size.width.toFloat()
                        val cH = size.height.toFloat()
                        val rect = mapImageRect(cW, cH, map.widthPx, map.heightPx)
                        val nearestPin = placedSnapshots.minByOrNull { sp ->
                            val px = relToScreen(sp.mapX, rect.left, rect.width, cW, scale, offset.x)
                            val py = relToScreen(sp.mapY, rect.top, rect.height, cH, scale, offset.y)
                            (startOffset.x - px) * (startOffset.x - px) +
                            (startOffset.y - py) * (startOffset.y - py)
                        }
                        if (nearestPin != null) {
                            val px = relToScreen(nearestPin.mapX, rect.left, rect.width, cW, scale, offset.x)
                            val py = relToScreen(nearestPin.mapY, rect.top, rect.height, cH, scale, offset.y)
                            val dx = startOffset.x - px
                            val dy = startOffset.y - py
                            val dragRadius = (12f * scale).coerceIn(16f, 48f) * 2f
                            if (dx * dx + dy * dy <= dragRadius * dragRadius) {
                                draggingId  = nearestPin.snapshot.id
                                draggingPos = Offset(px, py)
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (draggingId != null) {
                            draggingPos += dragAmount
                            totalDrag   += dragAmount
                        }
                    },
                    onDragEnd = {
                        val snapId = draggingId
                        if (snapId != null) {
                            val dist = sqrt(totalDrag.x * totalDrag.x + totalDrag.y * totalDrag.y)
                            if (dist < 10f) {
                                // Long press without significant drag → delete dialog
                                val snap = placedSnapshots.firstOrNull { it.snapshot.id == snapId }
                                if (snap != null) onLongPressPin(snapId, snap.snapshot.name)
                            } else {
                                // Actual drag → save new position (image-relative)
                                val cW = size.width.toFloat()
                                val cH = size.height.toFloat()
                                val rect = mapImageRect(cW, cH, map.widthPx, map.heightPx)
                                val newRelX = screenToRel(draggingPos.x, rect.left, rect.width, cW, scale, offset.x).coerceIn(0f, 1f)
                                val newRelY = screenToRel(draggingPos.y, rect.top, rect.height, cH, scale, offset.y).coerceIn(0f, 1f)
                                onDragEnd(snapId, newRelX, newRelY)
                            }
                            draggingId = null
                        }
                    },
                    onDragCancel = { draggingId = null },
                )
            },
    ) {
        AsyncImage(
            model              = File(map.imageUri.removePrefix("file://")),
            contentDescription = map.name,
            contentScale       = ContentScale.Fit,
            modifier           = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX       = scale,
                    scaleY       = scale,
                    translationX = offset.x,
                    translationY = offset.y,
                ),
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val pinRadius = (12f * scale).coerceIn(16f, 48f)
            val apSize    = (12f * scale).coerceIn(14f, 44f)

            // Actual image display area within canvas (ContentScale.Fit may leave padding)
            val imgAspect = if (map.widthPx > 0 && map.heightPx > 0)
                map.widthPx.toFloat() / map.heightPx.toFloat() else 1f
            val imgW: Float
            val imgH: Float
            val imgLeft: Float
            val imgTop: Float
            if (size.width / size.height > imgAspect) {
                imgH = size.height
                imgW = imgH * imgAspect
                imgLeft = (size.width - imgW) / 2f
                imgTop = 0f
            } else {
                imgW = size.width
                imgH = imgW / imgAspect
                imgLeft = 0f
                imgTop = (size.height - imgH) / 2f
            }

            // ── Snapshot pins ───────────────────────────────────────────────
            placedSnapshots.forEach { sp ->
                val isDragging = sp.snapshot.id == draggingId
                val x = if (isDragging) draggingPos.x
                        else (imgLeft + sp.mapX * imgW - size.width  / 2f) * scale + size.width  / 2f + offset.x
                val y = if (isDragging) draggingPos.y
                        else (imgTop  + sp.mapY * imgH - size.height / 2f) * scale + size.height / 2f + offset.y

                val pinColor = when {
                    sp.maxRssi >= -65 -> Color(0xFF27AE60)
                    sp.maxRssi >= -75 -> Color(0xFFE67E22)
                    else              -> Color(0xFFC0392B)
                }
                val alpha  = if (isDragging) 0.75f else 1f
                val radius = if (isDragging) pinRadius * 1.2f else pinRadius

                if (heatmapEnabled && !isDragging) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(pinColor.copy(alpha = 0.6f), Color.Transparent),
                            center = Offset(x, y),
                            radius = pinRadius * 5f,
                        ),
                        radius = pinRadius * 5f,
                        center = Offset(x, y),
                    )
                }

                drawCircle(color = pinColor.copy(alpha = alpha), radius = radius, center = Offset(x, y))
                drawCircle(
                    color  = Color.White.copy(alpha = alpha),
                    radius = radius,
                    center = Offset(x, y),
                    style  = Stroke(2f),
                )
            }

            // ── Connection lines ─────────────────────────────────────────────
            if (showConnectionLines) {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
                snapshotsWithConnection.forEach { sp ->
                    val connectedAp = apLocations.firstOrNull { ap ->
                        matchApByBssidOrName(ap, sp.snapshot.connectedBssid, sp.snapshot.connectedApName)
                    } ?: return@forEach

                    val sx = (imgLeft + sp.mapX * imgW - size.width  / 2f) * scale + size.width  / 2f + offset.x
                    val sy = (imgTop  + sp.mapY * imgH - size.height / 2f) * scale + size.height / 2f + offset.y
                    val ax = (imgLeft + connectedAp.mapX * imgW - size.width  / 2f) * scale + size.width  / 2f + offset.x
                    val ay = (imgTop  + connectedAp.mapY * imgH - size.height / 2f) * scale + size.height / 2f + offset.y

                    val lineColor = when {
                        sp.maxRssi >= -65 -> Color(0xFF1D9E75)
                        sp.maxRssi >= -75 -> Color(0xFFEF9F27)
                        else              -> Color(0xFFD85A30)
                    }

                    drawLine(
                        color       = lineColor.copy(alpha = 0.7f),
                        start       = Offset(sx, sy),
                        end         = Offset(ax, ay),
                        strokeWidth = 2f,
                        pathEffect  = dashEffect,
                    )

                    val midX = (sx + ax) / 2f
                    val midY = (sy + ay) / 2f
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            "${sp.maxRssi} dBm",
                            midX, midY,
                            android.graphics.Paint().apply {
                                color     = android.graphics.Color.WHITE
                                textSize  = 28f
                                textAlign = android.graphics.Paint.Align.CENTER
                                setShadowLayer(4f, 0f, 0f, android.graphics.Color.BLACK)
                            },
                        )
                    }
                }
            }

            // ── AP pins ─────────────────────────────────────────────────────
            if (showAps) {
                apLocations.forEach { ap ->
                    val x = (imgLeft + ap.mapX * imgW - size.width  / 2f) * scale + size.width  / 2f + offset.x
                    val y = (imgTop  + ap.mapY * imgH - size.height / 2f) * scale + size.height / 2f + offset.y

                    val apColor = when (ap.status?.uppercase()) {
                        "ONLINE"  -> Color(0xFF2980B9)
                        "OFFLINE" -> Color(0xFF7F8C8D)
                        else      -> Color(0xFF2980B9)
                    }

                    val trianglePath = Path().apply {
                        moveTo(x,                    y - apSize)
                        lineTo(x + apSize * 0.85f,   y + apSize * 0.7f)
                        lineTo(x - apSize * 0.85f,   y + apSize * 0.7f)
                        close()
                    }
                    drawPath(trianglePath, color = apColor)
                    drawPath(trianglePath, color = Color.White, style = Stroke(2f))

                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawText(
                            ap.apName,
                            x,
                            y + apSize * 0.7f + 16f,
                            android.graphics.Paint().apply {
                                color     = android.graphics.Color.WHITE
                                textSize  = (18f + apSize * 0.2f).coerceIn(18f, 32f)
                                textAlign = android.graphics.Paint.Align.CENTER
                                setShadowLayer(2f, 1f, 1f, android.graphics.Color.BLACK)
                            },
                        )
                    }
                }
            }
        }
    }
}
