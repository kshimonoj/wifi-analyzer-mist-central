package com.kshimono.wifianalyzer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotObservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SnapshotDao {

    @Insert
    suspend fun insertSnapshot(entity: SnapshotEntity): Long

    @Insert
    suspend fun insertObservations(entities: List<SnapshotObservationEntity>)

    @Query("SELECT * FROM snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    @Query("SELECT * FROM snapshots WHERE id = :id LIMIT 1")
    suspend fun getSnapshotById(id: Long): SnapshotEntity?

    @Query("SELECT * FROM snapshot_observations WHERE snapshotId = :snapshotId")
    suspend fun getObservationsBySnapshotId(snapshotId: Long): List<SnapshotObservationEntity>

    @Transaction
    suspend fun deleteSnapshot(id: Long) {
        deleteObservationsBySnapshotId(id)
        deleteSnapshotById(id)
    }

    @Query("DELETE FROM snapshot_observations WHERE snapshotId = :snapshotId")
    suspend fun deleteObservationsBySnapshotId(snapshotId: Long)

    @Query("DELETE FROM snapshots WHERE id = :id")
    suspend fun deleteSnapshotById(id: Long)

    @Query("UPDATE snapshots SET floorMapId=:floorMapId, mapX=:mapX, mapY=:mapY WHERE id=:id")
    suspend fun updateMapPosition(id: Long, floorMapId: Long, mapX: Float, mapY: Float)

    @Query("SELECT * FROM snapshots WHERE floorMapId=:floorMapId ORDER BY timestamp DESC")
    fun getSnapshotsByMapId(floorMapId: Long): Flow<List<SnapshotEntity>>

    @Query("UPDATE snapshots SET floorMapId=NULL, mapX=NULL, mapY=NULL WHERE id=:id")
    suspend fun removeMapPosition(id: Long)

    @Query("SELECT MAX(rssi) FROM snapshot_observations WHERE snapshotId=:snapshotId")
    suspend fun getMaxRssi(snapshotId: Long): Int?

    @Query("SELECT * FROM snapshots WHERE floorMapId=:floorMapId AND connectedBssid IS NOT NULL ORDER BY timestamp DESC")
    fun getSnapshotsWithConnection(floorMapId: Long): Flow<List<SnapshotEntity>>

    @Transaction
    suspend fun deleteAll() {
        deleteAllObservations()
        deleteAllSnapshots()
    }

    @Query("DELETE FROM snapshot_observations")
    suspend fun deleteAllObservations()

    @Query("DELETE FROM snapshots")
    suspend fun deleteAllSnapshots()

    @Query("UPDATE snapshots SET floorMapId=NULL, mapX=NULL, mapY=NULL WHERE floorMapId=:floorMapId")
    suspend fun removeAllFromMap(floorMapId: Long)
}
