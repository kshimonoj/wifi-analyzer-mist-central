package com.kshimono.wifianalyzer.ui.map

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kshimono.wifianalyzer.data.db.dao.ApLocationDao
import com.kshimono.wifianalyzer.data.db.dao.SnapshotDao
import com.kshimono.wifianalyzer.data.db.entities.ApLocationEntity
import com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
import com.kshimono.wifianalyzer.data.db.SnapshotRepository
import com.kshimono.wifianalyzer.data.floormap.FloorMapRepository
import com.kshimono.wifianalyzer.domain.usecase.ExportSurveyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ExportStatus {
    object Idle    : ExportStatus()
    object Working : ExportStatus()
    data class Done(val uri: Uri)        : ExportStatus()
    data class Fail(val message: String) : ExportStatus()
}

data class SnapshotWithPosition(
    val snapshot: SnapshotEntity,
    val mapX: Float,
    val mapY: Float,
    val maxRssi: Int,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FloorMapViewModel @Inject constructor(
    private val floorMapRepository:  FloorMapRepository,
    private val snapshotRepository:  SnapshotRepository,
    private val snapshotDao:         SnapshotDao,
    private val apLocationDao:       ApLocationDao,
    private val exportSurveyUseCase: ExportSurveyUseCase,
) : ViewModel() {

    val floorMaps: StateFlow<List<FloorMapEntity>> = floorMapRepository.getAllMaps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _selectedMap = MutableStateFlow<FloorMapEntity?>(null)
    val selectedMap: StateFlow<FloorMapEntity?> = _selectedMap.asStateFlow()

    private val _selectedForPlacement = MutableStateFlow<SnapshotEntity?>(null)
    val selectedForPlacement: StateFlow<SnapshotEntity?> = _selectedForPlacement.asStateFlow()

    private val _heatmapEnabled = MutableStateFlow(false)
    val heatmapEnabled: StateFlow<Boolean> = _heatmapEnabled.asStateFlow()

    private val _showAps = MutableStateFlow(true)
    val showAps: StateFlow<Boolean> = _showAps.asStateFlow()

    private val _showConnectionLines = MutableStateFlow(false)
    val showConnectionLines: StateFlow<Boolean> = _showConnectionLines.asStateFlow()

    private val _apSyncStatus = MutableStateFlow("")
    val apSyncStatus: StateFlow<String> = _apSyncStatus.asStateFlow()

    private val _exportStatus = MutableStateFlow<ExportStatus>(ExportStatus.Idle)
    val exportStatus: StateFlow<ExportStatus> = _exportStatus.asStateFlow()

    val placedSnapshots: StateFlow<List<SnapshotWithPosition>> = _selectedMap
        .flatMapLatest { map ->
            if (map == null) flowOf(emptyList())
            else snapshotDao.getSnapshotsByMapId(map.id).mapLatest { snapshots ->
                snapshots.mapNotNull { s ->
                    val mx = s.mapX ?: return@mapNotNull null
                    val my = s.mapY ?: return@mapNotNull null
                    SnapshotWithPosition(s, mx, my, snapshotDao.getMaxRssi(s.id) ?: -100)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unplacedSnapshots: StateFlow<List<SnapshotEntity>> = combine(
        snapshotRepository.getAllSnapshots(),
        _selectedMap,
    ) { allSnapshots, map ->
        val mapId = map?.id ?: return@combine emptyList()
        allSnapshots.filter { it.floorMapId != mapId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val apLocations: StateFlow<List<ApLocationEntity>> = _selectedMap
        .flatMapLatest { map ->
            if (map != null) apLocationDao.getByFloorMapId(map.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val snapshotsWithConnection: StateFlow<List<SnapshotWithPosition>> = _selectedMap
        .flatMapLatest { map ->
            if (map == null) flowOf(emptyList())
            else snapshotDao.getSnapshotsWithConnection(map.id).mapLatest { snapshots ->
                snapshots.mapNotNull { s ->
                    val mx = s.mapX ?: return@mapNotNull null
                    val my = s.mapY ?: return@mapNotNull null
                    SnapshotWithPosition(s, mx, my, snapshotDao.getMaxRssi(s.id) ?: -100)
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectMap(id: Long) {
        viewModelScope.launch {
            _selectedMap.value = floorMapRepository.getById(id)
            _selectedForPlacement.value = null
            _apSyncStatus.value = ""
        }
    }

    fun reloadSelectedMap() {
        viewModelScope.launch {
            val current = _selectedMap.value ?: return@launch
            _selectedMap.value = floorMapRepository.getById(current.id)
        }
    }

    fun selectForPlacement(snapshot: SnapshotEntity?) {
        _selectedForPlacement.value = snapshot
    }

    fun placeSnapshot(mapX: Float, mapY: Float) {
        val snapshot = _selectedForPlacement.value ?: return
        val mapId    = _selectedMap.value?.id ?: return
        viewModelScope.launch {
            snapshotDao.updateMapPosition(snapshot.id, mapId, mapX, mapY)
            _selectedForPlacement.value = null
        }
    }

    fun toggleHeatmap() {
        _heatmapEnabled.value = !_heatmapEnabled.value
    }

    fun toggleShowAps() {
        _showAps.value = !_showAps.value
    }

    fun toggleConnectionLines() {
        _showConnectionLines.value = !_showConnectionLines.value
    }

    fun syncApLocations() {
        viewModelScope.launch {
            val mapId = _selectedMap.value?.id ?: return@launch
            val map = floorMapRepository.getById(mapId) ?: return@launch
            _apSyncStatus.value = "Syncing..."
            floorMapRepository.syncApLocations(map)
                .onSuccess { count -> _apSyncStatus.value = "✓ $count APs" }
                .onFailure { e -> _apSyncStatus.value = "✗ ${e.message}" }
        }
    }

    fun removeSnapshotFromMap(snapshotId: Long) {
        viewModelScope.launch { floorMapRepository.removeSnapshotFromMap(snapshotId) }
    }

    fun removeAllSnapshotsFromMap() {
        val mapId = _selectedMap.value?.id ?: return
        viewModelScope.launch { floorMapRepository.removeAllSnapshotsFromMap(mapId) }
    }

    fun updateSnapshotPosition(snapshotId: Long, mapX: Float, mapY: Float) {
        val mapId = _selectedMap.value?.id ?: return
        viewModelScope.launch { snapshotDao.updateMapPosition(snapshotId, mapId, mapX, mapY) }
    }

    fun exportSurvey() {
        val map = _selectedMap.value ?: return
        viewModelScope.launch {
            _exportStatus.value = ExportStatus.Working
            _exportStatus.value = exportSurveyUseCase.execute(
                floorMap        = map,
                apLocations     = apLocations.value,
                placedSnapshots = placedSnapshots.value,
            ).fold(
                onSuccess = { uri -> ExportStatus.Done(uri) },
                onFailure = { e  -> ExportStatus.Fail(e.message ?: "Export failed") },
            )
        }
    }

    fun clearExportStatus() {
        _exportStatus.value = ExportStatus.Idle
    }
}
