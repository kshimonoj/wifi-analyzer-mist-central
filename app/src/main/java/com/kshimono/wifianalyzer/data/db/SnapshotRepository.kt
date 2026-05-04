package com.kshimono.wifianalyzer.data.db

import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotObservationEntity
import com.kshimono.wifianalyzer.domain.model.BssidSummary
import kotlinx.coroutines.flow.Flow

interface SnapshotRepository {
    suspend fun saveSnapshot(
        name: String,
        locationLabel: String,
        floorLabel: String,
        note: String,
        latitude: Double?,
        longitude: Double?,
        gpsAccuracy: Float? = null,
        connectedSsid: String? = null,
        connectedBssid: String? = null,
        connectedApName: String? = null,
        observations: List<BssidSummary>,
    ): Long

    fun getAllSnapshots(): Flow<List<SnapshotEntity>>

    suspend fun getSnapshotById(id: Long): SnapshotEntity?

    suspend fun getObservations(snapshotId: Long): List<SnapshotObservationEntity>

    suspend fun deleteSnapshot(id: Long)

    suspend fun deleteAllSnapshots()
}
