package com.kshimono.wifianalyzer.data.wifi

import com.kshimono.wifianalyzer.domain.model.WifiObservation
import kotlinx.coroutines.flow.StateFlow

interface WifiScanner {
    val scanResults: StateFlow<List<WifiObservation>>
    val isScanning: StateFlow<Boolean>
    val scanThrottled: StateFlow<Boolean>
    val rssiHistory: StateFlow<Map<String, List<RssiHistory>>>
    val connectedBssid: StateFlow<String?>
    val connectedSsid: StateFlow<String?>
    fun getScanResults()
    fun startScan()
    fun stopScan()
    fun getRssiHistory(bssid: String): List<RssiHistory>
    fun getAllRssiHistory(): Map<String, List<RssiHistory>>
}
