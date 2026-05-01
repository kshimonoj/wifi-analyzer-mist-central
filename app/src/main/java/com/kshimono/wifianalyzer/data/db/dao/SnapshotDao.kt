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
}
