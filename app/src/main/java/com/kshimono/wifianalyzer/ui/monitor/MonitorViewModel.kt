package com.kshimono.wifianalyzer.ui.monitor

import androidx.lifecycle.ViewModel
import com.kshimono.wifianalyzer.data.wifi.ConnectedApMonitor
import com.kshimono.wifianalyzer.data.wifi.MonitorEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class MonitorViewModel @Inject constructor(
    private val monitor: ConnectedApMonitor,
) : ViewModel() {

    val monitorHistory: StateFlow<List<MonitorEntry>> = monitor.monitorHistory

    init {
        monitor.startMonitoring()
    }

    override fun onCleared() {
        super.onCleared()
        monitor.stopMonitoring()
    }
}
