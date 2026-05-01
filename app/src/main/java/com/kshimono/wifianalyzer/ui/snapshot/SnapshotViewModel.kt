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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class SnapshotViewModel @Inject constructor(
    private val repository:     SnapshotRepository,
    private val exportCsvUseCase: ExportCsvUseCase,
) : ViewModel() {

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
        viewModelScope.launch {
            repository.deleteSnapshot(id)
        }
    }

    fun exportCsv(context: Context, snapshotId: Long) {
        viewModelScope.launch {
            val snapshot     = repository.getSnapshotById(snapshotId) ?: return@launch
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
                Intent.createChooser(shareIntent, "CSVをエクスポート")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}
