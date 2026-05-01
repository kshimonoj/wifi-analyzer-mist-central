package com.kshimono.wifianalyzer.ui.monitor

import androidx.lifecycle.ViewModel
import com.kshimono.wifianalyzer.data.wifi.ConnectedApInfo
import com.kshimono.wifianalyzer.data.wifi.ConnectedApMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val monitor: ConnectedApMonitor,
) : ViewModel() {

    val connectedHistory: StateFlow<List<ConnectedApInfo>> = monitor.connectedHistory

    init {
        monitor.startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        monitor.stopMonitoring()
    }
}
