package com.kshimono.wifianalyzer.domain.model

data class WifiObservation(
    val bssid: String,
    val ssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val channel: Int,
    val channelWidth: String,
    val security: String,
    val capabilities: String,
    val vendor: String = "",
    val mistApName: String? = null,
    val arubaApName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
)
