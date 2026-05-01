package com.kshimono.wifianalyzer.data.wifi

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_CONNECTED_HISTORY = 60

@Singleton
class ConnectedApMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _connectedHistory = MutableStateFlow<List<ConnectedApInfo>>(emptyList())
    val connectedHistory: StateFlow<List<ConnectedApInfo>> = _connectedHistory.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = scope.launch {
            while (isActive) {
                poll()
                delay(1_000L)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    @Suppress("DEPRECATION")
    private fun poll() {
        val info = wifiManager.connectionInfo ?: return
        val bssid = info.bssid ?: return
        if (bssid == "02:00:00:00:00:00") return
        val freq = info.frequency
        val channel = ScanResultMapper.frequencyToChannel(freq)
        val band = when {
            freq in 2400..2500 -> "2.4 GHz"
            freq in 5000..5900 -> "5 GHz"
            freq in 5925..7125 -> "6 GHz"
            else               -> "?"
        }
        val rxSpeed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
            info.rxLinkSpeedMbps else -1
        val entry = ConnectedApInfo(
            ssid            = info.ssid?.removeSurrounding("\"") ?: "",
            bssid           = bssid,
            rssi            = info.rssi,
            channel         = channel,
            band            = band,
            frequencyMhz    = freq,
            linkSpeedMbps   = info.linkSpeed,
            rxLinkSpeedMbps = rxSpeed,
            timestamp       = System.currentTimeMillis(),
        )
        val current = _connectedHistory.value.toMutableList()
        current.add(entry)
        if (current.size > MAX_CONNECTED_HISTORY) current.removeAt(0)
        _connectedHistory.value = current
    }
}
