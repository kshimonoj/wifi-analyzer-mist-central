package com.kshimono.wifianalyzer.ui.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kshimono.wifianalyzer.data.wifi.RssiHistory
import com.kshimono.wifianalyzer.data.wifi.WifiScanner
import com.kshimono.wifianalyzer.domain.model.BssidSummary
import com.kshimono.wifianalyzer.domain.model.WifiObservation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanner: WifiScanner,
) : ViewModel() {

    private val _filterEssid    = MutableStateFlow("")
    val filterEssid: StateFlow<String> = _filterEssid.asStateFlow()

    private val _filterBand     = MutableStateFlow("All")
    val filterBand: StateFlow<String> = _filterBand.asStateFlow()

    private val _filterSecurity = MutableStateFlow("All")
    val filterSecurity: StateFlow<String> = _filterSecurity.asStateFlow()

    private val _sortOrder      = MutableStateFlow("RSSI")
    val sortOrder: StateFlow<String> = _sortOrder.asStateFlow()

    private val _autoScanEnabled  = MutableStateFlow(false)
    val autoScanEnabled: StateFlow<Boolean> = _autoScanEnabled.asStateFlow()

    private val _autoScanInterval = MutableStateFlow(10)
    val autoScanInterval: StateFlow<Int> = _autoScanInterval.asStateFlow()

    val scanResults: StateFlow<List<WifiObservation>> = scanner.scanResults
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val isScanning: StateFlow<Boolean> = scanner.isScanning
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val scanThrottled: StateFlow<Boolean> = scanner.scanThrottled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val rssiHistory: StateFlow<Map<String, List<RssiHistory>>> = scanner.rssiHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val filteredResults: StateFlow<List<BssidSummary>> = combine(
        scanner.scanResults,
        _filterEssid,
        _filterBand,
        _filterSecurity,
        _sortOrder,
    ) { results, essid, band, security, sort ->
        results
            .filter { obs ->
                (essid.isEmpty() || obs.ssid.contains(essid, ignoreCase = true)) &&
                (band == "All"     || BssidSummary.toBand(obs.frequencyMhz) == band) &&
                (security == "All" || obs.security.startsWith(security, ignoreCase = true))
            }
            .let { filtered ->
                when (sort) {
                    "SSID"    -> filtered.sortedBy { it.ssid.lowercase() }
                    "Channel" -> filtered.sortedBy { it.channel }
                    else      -> filtered.sortedByDescending { it.rssi }
                }
            }
            .map { obs -> BssidSummary(obs, BssidSummary.toBand(obs.frequencyMhz)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private var autoScanJob: Job? = null

    init {
        scanner.getScanResults()
    }

    fun startScan() = scanner.startScan()

    fun setFilterEssid(value: String)    { _filterEssid.value    = value }
    fun setFilterBand(value: String)     { _filterBand.value     = value }
    fun setFilterSecurity(value: String) { _filterSecurity.value = value }
    fun setSortOrder(value: String)      { _sortOrder.value      = value }

    fun toggleAutoScan() {
        _autoScanEnabled.value = !_autoScanEnabled.value
        if (_autoScanEnabled.value) startAutoScanLoop() else stopAutoScanLoop()
    }

    fun setAutoScanInterval(seconds: Int) {
        _autoScanInterval.value = seconds
        if (_autoScanEnabled.value) {
            stopAutoScanLoop()
            startAutoScanLoop()
        }
    }

    fun getRssiHistoryForBssids(bssids: List<String>): Map<String, List<RssiHistory>> =
        scanner.getAllRssiHistory().filter { it.key in bssids }

    private fun startAutoScanLoop() {
        autoScanJob = viewModelScope.launch {
            while (isActive) {
                scanner.startScan()
                delay(_autoScanInterval.value * 1_000L)
            }
        }
    }

    private fun stopAutoScanLoop() {
        autoScanJob?.cancel()
        autoScanJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoScanLoop()
        scanner.stopScan()
    }
}
