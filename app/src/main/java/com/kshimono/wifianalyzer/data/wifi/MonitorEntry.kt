package com.kshimono.wifianalyzer.data.wifi

import com.kshimono.wifianalyzer.domain.model.WifiObservation

data class MonitorEntry(
    val connected: ConnectedApInfo,
    val scanResults: List<WifiObservation>,
)
