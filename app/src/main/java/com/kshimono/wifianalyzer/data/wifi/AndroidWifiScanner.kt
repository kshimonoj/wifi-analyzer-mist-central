package com.kshimono.wifianalyzer.data.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
import android.os.Build
import com.kshimono.wifianalyzer.data.aruba.ArubaRepository
import com.kshimono.wifianalyzer.data.mist.MistRepository
import com.kshimono.wifianalyzer.data.oui.OuiVendorRepository
import com.kshimono.wifianalyzer.domain.model.WifiObservation
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val MAX_RSSI_HISTORY = 30

@Singleton
class AndroidWifiScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ouiRepo: OuiVendorRepository,
    private val mistRepository: MistRepository,
    private val arubaRepository: ArubaRepository,
) : WifiScanner {

    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val _scanResults    = MutableStateFlow<List<WifiObservation>>(emptyList())
    private val _isScanning     = MutableStateFlow(false)
    private val _scanThrottled  = MutableStateFlow(false)
    private val historyMap      = HashMap<String, ArrayDeque<RssiHistory>>()
    private val _rssiHistory    = MutableStateFlow<Map<String, List<RssiHistory>>>(emptyMap())
    private val _connectedBssid = MutableStateFlow<String?>(null)
    private val _connectedSsid  = MutableStateFlow<String?>(null)

    override val scanResults:    StateFlow<List<WifiObservation>>          = _scanResults.asStateFlow()
    override val isScanning:     StateFlow<Boolean>                        = _isScanning.asStateFlow()
    override val scanThrottled:  StateFlow<Boolean>                        = _scanThrottled.asStateFlow()
    override val rssiHistory:    StateFlow<Map<String, List<RssiHistory>>> = _rssiHistory.asStateFlow()
    override val connectedBssid: StateFlow<String?>                        = _connectedBssid.asStateFlow()
    override val connectedSsid:  StateFlow<String?>                        = _connectedSsid.asStateFlow()

    private var receiverRegistered = false

    init {
        refreshFromCache(throttled = false)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            if (intent.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
            val updated = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
            _scanThrottled.value = !updated
            _isScanning.value    = false
            refreshFromCache(throttled = !updated)
        }
    }

    override fun getScanResults() { refreshFromCache(throttled = false) }

    override fun startScan() {
        if (_isScanning.value) return
        registerReceiverIfNeeded()
        refreshFromCache(throttled = false)
        @Suppress("DEPRECATION")
        val started = wifiManager.startScan()
        if (started) {
            _isScanning.value    = true
            _scanThrottled.value = false
        } else {
            _isScanning.value    = false
            _scanThrottled.value = true
            refreshFromCache(throttled = true)
        }
    }

    override fun stopScan() {
        _isScanning.value = false
        unregisterReceiverSafely()
    }

    override fun getRssiHistory(bssid: String): List<RssiHistory> =
        historyMap[bssid]?.toList() ?: emptyList()

    override fun getAllRssiHistory(): Map<String, List<RssiHistory>> =
        historyMap.mapValues { it.value.toList() }

    @Suppress("DEPRECATION")
    private fun updateConnectedAp() {
        val info  = wifiManager.connectionInfo
        val bssid = info?.bssid
        if (bssid == null || bssid == "02:00:00:00:00:00") {
            _connectedBssid.value = null
            _connectedSsid.value  = null
        } else {
            _connectedBssid.value = bssid
            _connectedSsid.value  = info.ssid?.removeSurrounding("\"")
        }
    }

    private fun refreshFromCache(throttled: Boolean) {
        updateConnectedAp()
        val mapped = wifiManager.scanResults
            .map { sr ->
                val obs = ScanResultMapper.map(sr)
                obs.copy(
                    vendor      = ouiRepo.getVendor(obs.bssid),
                    mistApName  = mistRepository.findApNameByBssid(obs.bssid),
                    arubaApName = arubaRepository.findApNameByBssid(obs.bssid),
                )
            }
            .sortedByDescending { it.rssi }
        when {
            mapped.isNotEmpty() -> {
                updateHistory(mapped)
                _scanResults.value = mapped
            }
            !throttled -> _scanResults.value = emptyList()
        }
    }

    private fun updateHistory(results: List<WifiObservation>) {
        val now = System.currentTimeMillis()
        results.forEach { obs ->
            val deque = historyMap.getOrPut(obs.bssid) { ArrayDeque() }
            deque.addLast(RssiHistory(now, obs.rssi))
            while (deque.size > MAX_RSSI_HISTORY) deque.removeFirst()
        }
        _rssiHistory.value = historyMap.mapValues { it.value.toList() }
    }

    private fun registerReceiverIfNeeded() {
        if (receiverRegistered) return
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        receiverRegistered = true
    }

    private fun unregisterReceiverSafely() {
        if (!receiverRegistered) return
        try { context.unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
        receiverRegistered = false
    }
}
