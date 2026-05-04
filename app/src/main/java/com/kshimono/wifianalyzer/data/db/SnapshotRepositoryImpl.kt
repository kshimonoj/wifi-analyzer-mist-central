package com.kshimono.wifianalyzer.data.db

import com.kshimono.wifianalyzer.data.db.dao.SnapshotDao
import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotObservationEntity
import com.kshimono.wifianalyzer.domain.model.BssidSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnapshotRepositoryImpl @Inject constructor(
    private val dao: SnapshotDao,
) : SnapshotRepository {

    override suspend fun saveSnapshot(
        name: String,
        locationLabel: String,
        floorLabel: String,
        note: String,
        latitude: Double?,
        longitude: Double?,
        gpsAccuracy: Float?,
        connectedSsid: String?,
        connectedBssid: String?,
        connectedApName: String?,
        observations: List<BssidSummary>,
    ): Long {
        val snapshotEntity = SnapshotEntity(
            name            = name,
            timestamp       = System.currentTimeMillis(),
            locationLabel   = locationLabel,
            floorLabel      = floorLabel,
            note            = note,
            latitude        = latitude,
            longitude       = longitude,
            gpsAccuracy     = gpsAccuracy,
            connectedSsid   = connectedSsid,
            connectedBssid  = connectedBssid,
            connectedApName = connectedApName,
            bssidCount      = observations.size,
        )
        val snapshotId = dao.insertSnapshot(snapshotEntity)
        val obsEntities = observations.map { summary ->
            val obs = summary.observation
            SnapshotObservationEntity(
                snapshotId   = snapshotId,
                ssid         = obs.ssid,
                bssid        = obs.bssid,
                rssi         = obs.rssi,
                frequencyMhz = obs.frequencyMhz,
                channel      = obs.channel,
                channelWidth = obs.channelWidth,
                security     = obs.security,
                capabilities = obs.capabilities,
                vendor       = obs.vendor,
                band         = summary.band,
                mistApName   = obs.mistApName,
                arubaApName  = obs.arubaApName,
            )
        }
        dao.insertObservations(obsEntities)
        return snapshotId
    }

    override fun getAllSnapshots(): Flow<List<SnapshotEntity>> =
        dao.getAllSnapshots()

    override suspend fun getSnapshotById(id: Long): SnapshotEntity? =
        dao.getSnapshotById(id)

    override suspend fun getObservations(snapshotId: Long): List<SnapshotObservationEntity> =
        dao.getObservationsBySnapshotId(snapshotId)

    override suspend fun deleteSnapshot(id: Long) =
        dao.deleteSnapshot(id)

    override suspend fun deleteAllSnapshots() =
        dao.deleteAll()
}
