package com.kshimono.wifianalyzer.ui.monitor

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
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val tsKey = ExtraStore.Key<List<Long>>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonitorScreen(viewModel: MonitorViewModel = hiltViewModel()) {
    val history by viewModel.connectedHistory.collectAsStateWithLifecycle()

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
                ConnectedApCard(history.last())
                RssiChartCard(history)
                SpeedChartCard(history)
                ChannelChartCard(history)
            }
        }
    }
}

// ── Connected AP summary card ────────────────────────────────────────────────

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

// ── RSSI chart ───────────────────────────────────────────────────────────────

@Composable
private fun RssiChartCard(history: List<ConnectedApInfo>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val timeFormatter = rememberTimeFormatter()
    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "%.0f dBm".format(value) }
    }

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries { series(history.map { it.rssi }) }
                extras { store -> store.set(tsKey, history.map { it.timestamp }) }
            }
        }
    }

    MonitorChartCard(title = "RSSI (dBm)") {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
    }
}

// ── Speed chart ──────────────────────────────────────────────────────────────

@Composable
private fun SpeedChartCard(history: List<ConnectedApInfo>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val timeFormatter = rememberTimeFormatter()
    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "%.0f Mbps".format(value) }
    }

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries {
                    series(history.map { it.linkSpeedMbps })
                    if (history.any { it.rxLinkSpeedMbps >= 0 }) {
                        series(history.map { it.rxLinkSpeedMbps.coerceAtLeast(0) })
                    }
                }
                extras { store -> store.set(tsKey, history.map { it.timestamp }) }
            }
        }
    }

    MonitorChartCard(title = "Speed (Mbps)  TX / RX") {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
    }
}

// ── Channel chart ────────────────────────────────────────────────────────────

@Composable
private fun ChannelChartCard(history: List<ConnectedApInfo>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    val timeFormatter = rememberTimeFormatter()
    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "Ch %.0f".format(value) }
    }

    LaunchedEffect(history) {
        if (history.isNotEmpty()) {
            modelProducer.runTransaction {
                lineSeries { series(history.map { it.channel }) }
                extras { store -> store.set(tsKey, history.map { it.timestamp }) }
            }
        }
    }

    MonitorChartCard(title = "Channel") {
        CartesianChartHost(
            chart = rememberCartesianChart(
                rememberLineCartesianLayer(),
                startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
            ),
            modelProducer = modelProducer,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        )
    }
}

// ── Shared helpers ────────────────────────────────────────────────────────────

@Composable
private fun MonitorChartCard(title: String, content: @Composable () -> Unit) {
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
        }
    }
}

@Composable
private fun rememberTimeFormatter(): CartesianValueFormatter {
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    return remember {
        CartesianValueFormatter { context, value, _ ->
            val timestamps = context.model.extraStore.getOrNull(tsKey)
            val ts = timestamps?.getOrNull(value.toInt()) ?: return@CartesianValueFormatter ""
            sdf.format(Date(ts))
        }
    }
}
