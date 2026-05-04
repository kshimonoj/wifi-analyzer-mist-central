package com.kshimono.wifianalyzer.data.floormap

import android.net.Uri
import com.kshimono.wifianalyzer.data.aruba.ArubaFloor
import com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity
import com.kshimono.wifianalyzer.data.mist.MistMap
import kotlinx.coroutines.flow.Flow

interface FloorMapRepository {
    fun getAllMaps(): Flow<List<FloorMapEntity>>
    suspend fun getById(id: Long): FloorMapEntity?
    suspend fun deleteMap(id: Long)
    suspend fun importLocalFile(uri: Uri, name: String): Result<Long>
    suspend fun importFromMist(map: MistMap, token: String, region: String): Result<Long>
    suspend fun importFromAruba(
        siteId: String,
        floor: ArubaFloor,
        buildingName: String,
    ): Result<Long>
    suspend fun syncApLocations(floorMap: FloorMapEntity): Result<Int>
    suspend fun removeSnapshotFromMap(snapshotId: Long)
    suspend fun removeAllSnapshotsFromMap(floorMapId: Long)
}
