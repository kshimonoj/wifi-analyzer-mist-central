package com.kshimono.wifianalyzer.ui.snapshot

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kshimono.wifianalyzer.data.db.SnapshotRepository
import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
import com.kshimono.wifianalyzer.data.location.LocationRepository
import com.kshimono.wifianalyzer.data.location.LocationResult
import com.kshimono.wifianalyzer.data.wifi.WifiScanner
import com.kshimono.wifianalyzer.domain.model.BssidSummary
import com.kshimono.wifianalyzer.domain.usecase.ExportCsvUseCase
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "SnapshotVM"

@HiltViewModel
class SnapshotViewModel @Inject constructor(
    private val repository:         SnapshotRepository,
    private val exportCsvUseCase:   ExportCsvUseCase,
    private val locationRepository: LocationRepository,
    private val scanner:            WifiScanner,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        Log.e(TAG, "Uncaught exception", t)
        _errorMessage.value = t.message ?: "Unexpected error"
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allSnapshots: StateFlow<List<SnapshotEntity>> = repository.getAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val connectedBssid: StateFlow<String?> = scanner.connectedBssid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val connectedSsid: StateFlow<String?> = scanner.connectedSsid
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // GPS state
    private val _gpsLocation = MutableStateFlow<LocationResult?>(null)
    val gpsLocation: StateFlow<LocationResult?> = _gpsLocation.asStateFlow()

    private val _gpsLoading = MutableStateFlow(false)
    val gpsLoading: StateFlow<Boolean> = _gpsLoading.asStateFlow()

    private val _gpsError = MutableStateFlow<String?>(null)
    val gpsError: StateFlow<String?> = _gpsError.asStateFlow()

    fun fetchGpsLocation() {
        viewModelScope.launch {
            _gpsLoading.value = true
            _gpsError.value = null
            try {
                _gpsLocation.value = locationRepository.getCurrentLocation()
            } catch (e: SecurityException) {
                _gpsError.value = "Location permission denied"
            } catch (e: Exception) {
                val msg = e.message ?: "Location unavailable"
                _gpsError.value = when {
                    msg.contains("disabled", ignoreCase = true) -> "Location services disabled"
                    msg.contains("timeout", ignoreCase = true)  -> "Location timeout"
                    else -> msg
                }
            } finally {
                _gpsLoading.value = false
            }
        }
    }

    fun clearGpsState() {
        _gpsLocation.value = null
        _gpsError.value    = null
        _gpsLoading.value  = false
    }

    fun saveSnapshot(
        name:          String,
        locationLabel: String,
        floorLabel:    String,
        note:          String,
        observations:  List<BssidSummary>,
    ) {
        val gps        = _gpsLocation.value
        val connBssid  = scanner.connectedBssid.value
        val connSsid   = scanner.connectedSsid.value
        val connApName = if (connBssid != null) {
            observations.firstOrNull { it.observation.bssid.equals(connBssid, ignoreCase = true) }
                ?.let { it.observation.mistApName ?: it.observation.arubaApName }
        } else null
        viewModelScope.launch {
            repository.saveSnapshot(
                name            = name,
                locationLabel   = locationLabel,
                floorLabel      = floorLabel,
                note            = note,
                latitude        = gps?.latitude,
                longitude       = gps?.longitude,
                gpsAccuracy     = gps?.accuracy,
                connectedSsid   = connSsid,
                connectedBssid  = connBssid,
                connectedApName = connApName,
                observations    = observations,
            )
        }
    }

    fun deleteSnapshot(id: Long) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteSnapshot(id)
        }
    }

    fun deleteAllSnapshots() {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteAllSnapshots()
        }
    }

    fun clearError() { _errorMessage.value = null }

    fun exportCsv(context: Context, snapshotId: Long) {
        viewModelScope.launch(exceptionHandler) {
            runCatching {
                val snapshot     = repository.getSnapshotById(snapshotId)
                    ?: throw Exception("Snapshot not found")
                val observations = repository.getObservations(snapshotId)
                val csv          = exportCsvUseCase.execute(snapshot, observations)

                val csvDir = File(context.cacheDir, "csv").also { it.mkdirs() }
                val file   = File(csvDir, "wifi_snapshot_$snapshotId.csv")
                file.writeText(csv)

                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file,
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type     = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Wi-Fi Snapshot: ${snapshot.name}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "Export CSV")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }.onFailure { e ->
                Log.e(TAG, "CSV export failed for snapshotId=$snapshotId", e)
                _errorMessage.value = "Export failed: ${e.message}"
            }
        }
    }
}
