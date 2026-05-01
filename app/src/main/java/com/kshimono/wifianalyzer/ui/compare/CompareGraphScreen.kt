package com.kshimono.wifianalyzer.ui.compare

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import com.kshimono.wifianalyzer.ui.scan.ScanViewModel
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

private val SERIES_COLORS = listOf(
    Color(0xFF1565C0),
    Color(0xFF2E7D32),
    Color(0xFF6A1B9A),
    Color(0xFFE65100),
    Color(0xFFC62828),
    Color(0xFF00695C),
    Color(0xFF4527A0),
    Color(0xFF558B2F),
)

private val compareTsKey = ExtraStore.Key<List<Long>>()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareGraphScreen(
    bssidsArg: String,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val bssids = remember(bssidsArg) {
        bssidsArg.split(",").filter { it.isNotBlank() }
    }
    val rssiHistory by viewModel.rssiHistory.collectAsStateWithLifecycle()
    val scanResults by viewModel.scanResults.collectAsStateWithLifecycle()

    val modelProducer = remember { CartesianChartModelProducer() }

    val yFormatter = remember {
        CartesianValueFormatter { _, value, _ -> "%.0f dBm".format(value) }
    }
    val timeFormatter = remember {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        CartesianValueFormatter { context, value, _ ->
            val timestamps = context.model.extraStore.getOrNull(compareTsKey)
            val ts = timestamps?.getOrNull(value.toInt()) ?: return@CartesianValueFormatter ""
            sdf.format(Date(ts))
        }
    }

    LaunchedEffect(rssiHistory, bssids) {
        val seriesData = bssids.mapNotNull { bssid ->
            rssiHistory[bssid]?.takeIf { it.isNotEmpty() }
        }
        if (seriesData.isNotEmpty()) {
            val maxLen = seriesData.maxOf { it.size }
            val longestTs = seriesData.maxByOrNull { it.size }!!.map { it.timestamp }
            modelProducer.runTransaction {
                lineSeries {
                    seriesData.forEach { hist ->
                        val padded = List(maxLen - hist.size) { hist.first().rssi } + hist.map { it.rssi }
                        series(padded)
                    }
                }
                extras { store -> store.set(compareTsKey, longestTs) }
            }
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("RSSI Comparison") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            val hasData = bssids.any { rssiHistory[it]?.isNotEmpty() == true }
            if (!hasData) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            Icons.Filled.BarChart,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            text  = "No scan history.\nRun a scan first.",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            } else {
                CartesianChartHost(
                    chart = rememberCartesianChart(
                        rememberLineCartesianLayer(),
                        startAxis  = VerticalAxis.rememberStart(valueFormatter = yFormatter),
                        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = timeFormatter),
                    ),
                    modelProducer = modelProducer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                )
                BssidLegend(bssids, scanResults.associate { it.bssid to it.ssid })
            }
        }
    }
}

@Composable
private fun BssidLegend(bssids: List<String>, bssidToSsid: Map<String, String>) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        itemsIndexed(bssids) { index, bssid ->
            val color = SERIES_COLORS.getOrElse(index) { SERIES_COLORS.last() }
            val ssid = bssidToSsid[bssid]?.ifBlank { "(hidden)" } ?: ""
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = MaterialTheme.shapes.extraSmall,
                    color = color,
                    content = {},
                )
                Column {
                    Text(
                        text = bssid,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                    )
                    if (ssid.isNotEmpty()) {
                        Text(
                            text = ssid,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
        }
    }
}
