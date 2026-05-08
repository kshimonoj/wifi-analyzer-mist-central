package com.kshimono.wifianalyzer.data.wifi

data class ConnectedApInfo(
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val channel: Int,
    val band: String,
    val frequencyMhz: Int,
    val linkSpeedMbps: Int,
    val rxLinkSpeedMbps: Int,
    val timestamp: Long,
    val apName: String? = null,
)
