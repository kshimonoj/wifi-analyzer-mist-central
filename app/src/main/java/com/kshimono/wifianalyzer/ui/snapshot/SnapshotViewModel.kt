package com.kshimono.wifianalyzer.ui.snapshot

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kshimono.wifianalyzer.data.db.SnapshotRepository
import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
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
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

private const val TAG = "SnapshotVM"

@HiltViewModel
class SnapshotViewModel @Inject constructor(
    private val repository:     SnapshotRepository,
    private val exportCsvUseCase: ExportCsvUseCase,
) : ViewModel() {

    private val exceptionHandler = CoroutineExceptionHandler { _, t ->
        Log.e(TAG, "Uncaught exception", t)
        _errorMessage.value = t.message ?: "Unexpected error"
    }

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    val allSnapshots: StateFlow<List<SnapshotEntity>> = repository.getAllSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun saveSnapshot(
        name:          String,
        locationLabel: String,
        floorLabel:    String,
        note:          String,
        observations:  List<BssidSummary>,
    ) {
        viewModelScope.launch {
            repository.saveSnapshot(
                name          = name,
                locationLabel = locationLabel,
                floorLabel    = floorLabel,
                note          = note,
                latitude      = null,
                longitude     = null,
                observations  = observations,
            )
        }
    }

    fun deleteSnapshot(id: Long) {
        viewModelScope.launch(exceptionHandler) {
            repository.deleteSnapshot(id)
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
