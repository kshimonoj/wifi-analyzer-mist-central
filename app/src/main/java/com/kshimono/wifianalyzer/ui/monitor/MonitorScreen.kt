package com.kshimono.wifianalyzer.ui.monitor

import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkWifi
import androidx.compose.material.icons.filled.NetworkWifi3Bar
import androidx.compose.material.icons.filled.SignalWifi4Bar
import androidx.compose.material.icons.filled.SignalWifiStatusbarNull
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kshimono.wifianalyzer.data.wifi.ConnectedApInfo
import com.kshimono.wifianalyzer.data.wifi.MonitorEntry
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.stacked
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.Decoration
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ── ExtraStore keys ────────────────────────────────────────────────────────────

private val tsKey             = ExtraStore.Key<List<Long>>()
private val apNameAtRoamingKey = ExtraStore.Key<List<String?>>()
private val roamingIdxKey     = ExtraStore.Key<Set<Int>>()

// ── Roaming helpers ────────────────────────────────────────────────────────────

private fun computeRoamingIndices(history: List<MonitorEntry>): Set<Int> {
    val result = mutableSetOf<Int>()
    for (i in 1 until history.size) {
        if (history[i].connected.bssid != history[i - 1].connected.bssid) result.add(i)
    }
    return result
}

private fun apNamesAtRoaming(history: List<MonitorEntry>, roaming: Set<Int>): List<String?> =
    history.mapIndexed { i, e ->
        if (i in roaming) (e.connected.apName ?: e.connected.bssid) else null
    }

// ── Decorations ────────────────────────────────────────────────────────────────

private class RoamingLineDecoration(private val indices: Set<Int>) : Decoration {
    private val paint by lazy {
        Paint().apply {
            color = 0xFFFF9800.toInt()
            strokeWidth = 3f
            pathEffect = DashPathEffect(floatArrayOf(12f, 6f), 0f)
            style = Paint.Style.STROKE
            isAntiAlias = true
        }
    }

    override fun drawOverLayers(context: CartesianDrawingContext) {
        if (indices.isEmpty()) return
        val bounds = context.layerBounds
        val minX   = context.ranges.minX.toFloat()
        val maxX   = context.ranges.maxX.toFloat()
        val xRange = maxX - minX
        if (xRange == 0f) return
        indices.forEach { idx ->
            val frac   = (idx.toFloat() - minX) / xRange
            val xPixel = bounds.left + frac * bounds.width()
            context.canvas.drawLine(xPixel, bounds.top, xPixel, bounds.bottom, paint)
        }
    }
}

private class SuboptimalRegionDecoration(private val indices: Set<Int>) : Decoration {
    private val paint by lazy {
        Paint().apply {
            color = (50 shl 24) or 0xFF0000
            style = Paint.Style.FILL
        }
    }

    override fun drawUnderLayers(context: CartesianDrawingContext) {
        if (indices.isEmpty()) return
        val bounds = context.layerBounds
        val minX   = context.ranges.minX.toFloat()
        val maxX   = context.ranges.maxX.toFloat()
        val xRange = maxX - minX
        if (xRange == 0f) return
        indices.forEach { idx ->
            val frac1 = ((idx.toFloat() - 0.5f - minX) / xRange).coerceAtLeast(0f)
            val frac2 = ((idx.toFloat() + 0.5f - minX) / xRange).coerceAtMost(1f)
            val px1   = bounds.left + frac1 * bounds.width()
            val px2   = bounds.left + frac2 * bounds.width()
            if (px2 > px1) {
                context.canvas.drawRect(px1, bounds.top, px2, bounds.bottom, paint)
            }
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(viewModel: MonitorViewModel = hiltViewModel()) {
    val history by viewModel.monitorHistory.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Monitor") }) },
    ) { padding ->
        if (history.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Filled.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.outline,
                    )
                    Text(
                        "Not connected to Wi-Fi",
                        color = MaterialTheme.colorScheme.outline,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ConnectedApCard(history.last().connected)
                RssiChartCard(history)
                SpeedChartCard(history)
                ChannelChartCard(history)
                CoChannelInterferenceChartCard(history)
                OptimalApCheckChartCard(history)
            }
        }
    }
}

// ── Connected AP summary card ──────────────────────────────────────────────────

@Composable
private fun ConnectedApCard(info: ConnectedApInfo) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SignalIcon(info.rssi)
                Text(
                    text = info.ssid.ifBlank { "(hidden)" },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                RssiBadge(info.rssi)
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = info.bssid,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.weight(1f),
                )
                BandChip(info.band)
                Text(
                    text = "Ch ${info.channel}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LabeledValue("TX", "${info.linkSpeedMbps} Mbps")
                if (info.rxLinkSpeedMbps >= 0) {
                    LabeledValue("RX", "${info.rxLinkSpeedMbps} Mbps")
                }
                LabeledValue("Freq", "${info.frequencyMhz} MHz")
                if (info.apName != null) {
                    LabeledValue("AP", info.apName)
                }
            }
        }
    }
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun SignalIcon(rssi: Int) {
    val (icon, tint) = when {
        rssi >= -60 -> Icons.Filled.SignalWifi4Bar      to Color(0xFF2E7D32)
        rssi >= -70 -> Icons.Filled.NetworkWifi         to Color(0xFFF9A825)
        rssi >= -80 -> Icons.Filled.NetworkWifi3Bar     to Color(0xFFE65100)
        else        -> Icons.Filled.SignalWifiStatusbarNull to MaterialTheme.colorScheme.error
    }
    Icon(icon, contentDescription = "$rssi dBm", tint = tint, modifier = Modifier.size(24.dp))
}

@Composable
private fun RssiBadge(rssi: Int) {
    val color = when {
        rssi >= -60 -> Color(0xFF2E7D32)
        rssi >= -75 -> MaterialTheme.colorScheme.secondary
        else        -> MaterialTheme.colorScheme.error
    }
    Text(
        text = "$rssi dBm",
        style = MaterialTheme.typography.titleSmall,
        color = color,
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
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = bg,
    ) {
        Text(
            text = band,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

// ── RSSI chart ─────────────────────────────────────────────────────────────────

@Composable
private fun RssiChartCard(history: List<MonitorEntry>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val timeFormatter = rememberTimeWithApFormatter()
    val yFormatter    = remember { CartesianValueFormatter { _, v, _ -> "%.0f dBm".format(v) } }
    val roaming       = remember(history) { computeRoamingIndices(history) }
    val decoration    = remember(roaming) { RoamingLineDecoration(roaming) }

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            val names = apNamesAtRoaming(history, roaming)
            modelProducer.runTransaction {
                lineSeries { series(history.map { it.connected.rssi }) }
                extras { s ->
                    s.set(tsKey, history.map { it.connected.timestamp })
                    s.set(roamingIdxKey, roaming)
                    s.set(apNameAtRoamingKey, names)
                }
            }
        }
    }

    MonitorChartCard(title = "RSSI (dBm)") {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
                decorations = listOf(decoration),
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

// ── Speed chart ────────────────────────────────────────────────────────────────

@Composable
private fun SpeedChartCard(history: List<MonitorEntry>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val timeFormatter = rememberTimeWithApFormatter()
    val yFormatter    = remember { CartesianValueFormatter { _, v, _ -> "%.0f Mbps".format(v) } }
    val roaming       = remember(history) { computeRoamingIndices(history) }
    val decoration    = remember(roaming) { RoamingLineDecoration(roaming) }

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            val names = apNamesAtRoaming(history, roaming)
            modelProducer.runTransaction {
                lineSeries {
                    series(history.map { it.connected.linkSpeedMbps })
                    if (history.any { it.connected.rxLinkSpeedMbps >= 0 }) {
                        series(history.map { it.connected.rxLinkSpeedMbps.coerceAtLeast(0) })
                    }
                }
                extras { s ->
                    s.set(tsKey, history.map { it.connected.timestamp })
                    s.set(roamingIdxKey, roaming)
                    s.set(apNameAtRoamingKey, names)
                }
            }
        }
    }

    MonitorChartCard(title = "Speed (Mbps)  TX / RX") {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
                decorations = listOf(decoration),
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

// ── Channel chart ──────────────────────────────────────────────────────────────

@Composable
private fun ChannelChartCard(history: List<MonitorEntry>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val timeFormatter = rememberTimeWithApFormatter()
    val yFormatter    = remember { CartesianValueFormatter { _, v, _ -> "Ch %.0f".format(v) } }
    val roaming       = remember(history) { computeRoamingIndices(history) }
    val decoration    = remember(roaming) { RoamingLineDecoration(roaming) }

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            val names = apNamesAtRoaming(history, roaming)
            modelProducer.runTransaction {
                lineSeries { series(history.map { it.connected.channel }) }
                extras { s ->
                    s.set(tsKey, history.map { it.connected.timestamp })
                    s.set(roamingIdxKey, roaming)
                    s.set(apNameAtRoamingKey, names)
                }
            }
        }
    }

    MonitorChartCard(title = "Channel") {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
                decorations = listOf(decoration),
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

// ── Co-Channel Interference chart ─────────────────────────────────────────────

@Composable
private fun CoChannelInterferenceChartCard(history: List<MonitorEntry>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val timeFormatter = rememberSimpleTimeFormatter()
    val yFormatter    = remember { CartesianValueFormatter { _, v, _ -> "%.0f".format(v) } }

    val managedCol   = rememberLineComponent(fill = fill(Color(0xFF4CAF50)), thickness = 8.dp)
    val unmanagedCol = rememberLineComponent(fill = fill(Color(0xFFF44336)), thickness = 8.dp)

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            val managed   = history.map { e ->
                val ch          = e.connected.channel
                val connBssidLc = e.connected.bssid.lowercase()
                e.scanResults
                    .filter { obs ->
                        obs.channel == ch &&
                        obs.bssid.lowercase() != connBssidLc &&
                        (obs.mistApName != null || obs.arubaApName != null)
                    }
                    .mapNotNull { it.mistApName ?: it.arubaApName }
                    .toSet()
                    .size
            }
            val unmanaged = history.map { e ->
                val ch          = e.connected.channel
                val connBssidLc = e.connected.bssid.lowercase()
                e.scanResults.count { obs ->
                    obs.channel == ch &&
                    obs.bssid.lowercase() != connBssidLc &&
                    obs.mistApName == null && obs.arubaApName == null
                }
            }
            modelProducer.runTransaction {
                columnSeries {
                    series(managed)
                    series(unmanaged)
                }
                extras { s -> s.set(tsKey, history.map { it.connected.timestamp }) }
            }
        }
    }

    MonitorChartCard(
        title = "Co-Channel Interference",
        legend = {
            LegendRow(listOf(
                Color(0xFF4CAF50) to "Managed AP",
                Color(0xFFF44336) to "Unmanaged AP",
            ))
        },
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    columnProvider = ColumnCartesianLayer.ColumnProvider.series(managedCol, unmanagedCol),
                    mergeMode      = { ColumnCartesianLayer.MergeMode.stacked() },
                ),
                startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

// ── Optimal AP Selection Check chart ──────────────────────────────────────────

@Composable
private fun OptimalApCheckChartCard(history: List<MonitorEntry>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val timeFormatter = rememberSimpleTimeFormatter()
    val yFormatter    = remember { CartesianValueFormatter { _, v, _ -> "%.0f dBm".format(v) } }

    val connectedLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(Color(0xFF1565C0))),
    )
    val bestLine = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(fill(Color(0xFF2E7D32))),
    )

    val suboptimalIndices = remember(history) {
        history.mapIndexedNotNull { i, e ->
            val connRssi = e.connected.rssi
            val best = e.scanResults
                .filter { it.ssid == e.connected.ssid && it.bssid != e.connected.bssid }
                .maxOfOrNull { it.rssi }
            if (best != null && best > connRssi + 5) i else null
        }.toSet()
    }
    val suboptimalDecoration = remember(suboptimalIndices) { SuboptimalRegionDecoration(suboptimalIndices) }

    val bestApName = remember(history) {
        val latest = history.lastOrNull() ?: return@remember null
        latest.scanResults
            .filter {
                it.ssid == latest.connected.ssid &&
                it.bssid.lowercase() != latest.connected.bssid.lowercase() &&
                (it.mistApName != null || it.arubaApName != null)
            }
            .maxByOrNull { it.rssi }
            ?.let { it.mistApName ?: it.arubaApName }
    }

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            val connRssi = history.map { it.connected.rssi.toFloat() }
            val bestRssi = history.map { e ->
                e.scanResults
                    .filter { it.ssid == e.connected.ssid }
                    .maxOfOrNull { it.rssi }
                    ?.toFloat()
                    ?: e.connected.rssi.toFloat()
            }
            modelProducer.runTransaction {
                lineSeries {
                    series(connRssi)
                    series(bestRssi)
                }
                extras { s -> s.set(tsKey, history.map { it.connected.timestamp }) }
            }
        }
    }

    MonitorChartCard(
        title = "Optimal AP Check",
        legend = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                LegendRow(listOf(
                    Color(0xFF1565C0) to "Connected AP",
                    Color(0xFF2E7D32) to "Best Available AP",
                ))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 2.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(Color(0x32FF0000), RoundedCornerShape(2.dp)),
                    )
                    Text(
                        text  = "Suboptimal zone (>5 dBm gap)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (bestApName != null) {
                    Text(
                        text  = "Best: $bestApName",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        },
    ) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(
                    lineProvider = LineCartesianLayer.LineProvider.series(connectedLine, bestLine),
                ),
                startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
                decorations = listOf(suboptimalDecoration),
            ),
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxWidth().height(180.dp),
        )
    }
}

// ── Shared helpers ─────────────────────────────────────────────────────────────

@Composable
private fun MonitorChartCard(
    title: String,
    legend: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(
        modifier  = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text  = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            content()
            legend?.invoke()
        }
    }
}

@Composable
private fun LegendRow(items: List<Pair<Color, String>>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 6.dp, bottom = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items.forEach { (color, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, RoundedCornerShape(2.dp)),
                )
                Text(
                    text  = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun rememberTimeWithApFormatter(): CartesianValueFormatter {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    return remember {
        CartesianValueFormatter { context, value, _ ->
            val idx = value.toInt()
            val timestamps = context.model.extraStore.getOrNull(tsKey) ?: return@CartesianValueFormatter ""
            val ts = timestamps.getOrNull(idx) ?: return@CartesianValueFormatter ""
            val time = sdf.format(Date(ts))
            val apName = context.model.extraStore.getOrNull(apNameAtRoamingKey)?.getOrNull(idx)
            if (apName != null) "$time\n[$apName]" else time
        }
    }
}

@Composable
private fun rememberSimpleTimeFormatter(): CartesianValueFormatter {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    return remember {
        CartesianValueFormatter { context, value, _ ->
            val timestamps = context.model.extraStore.getOrNull(tsKey)
            val ts = timestamps?.getOrNull(value.toInt()) ?: return@CartesianValueFormatter ""
            sdf.format(Date(ts))
        }
    }
}
