package com.kshimono.wifianalyzer.domain.usecase

import com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotEntity
import com.kshimono.wifianalyzer.data.db.entities.SnapshotObservationEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportCsvUseCase @Inject constructor() {

    private val dtf = DateTimeFormatter
        .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        .withZone(ZoneId.systemDefault())

    fun execute(
        snapshot: SnapshotEntity,
        observations: List<SnapshotObservationEntity>,
        floorMap: FloorMapEntity? = null,
    ): String {
        val sb = StringBuilder()
        sb.appendLine(HEADER)
        val ts = dtf.format(Instant.ofEpochMilli(snapshot.timestamp))
        val lat = snapshot.latitude?.toString() ?: ""
        val lon = snapshot.longitude?.toString() ?: ""
        val acc = snapshot.gpsAccuracy?.let { "%.1f".format(it) } ?: ""
        val mapXRel = snapshot.mapX?.toString() ?: ""
        val mapYRel = snapshot.mapY?.toString() ?: ""
        val mapXM = when {
            snapshot.mapX != null && floorMap?.lengthM != null -> (snapshot.mapX * floorMap.lengthM).toString()
            snapshot.mapX != null && floorMap?.widthM  != null -> (snapshot.mapX * floorMap.widthM).toString()
            else -> ""
        }
        val mapYM = when {
            snapshot.mapY != null && floorMap?.breadthM != null -> (snapshot.mapY * floorMap.breadthM).toString()
            snapshot.mapY != null && floorMap?.heightM  != null -> (snapshot.mapY * floorMap.heightM).toString()
            else -> ""
        }
        observations.forEach { obs ->
            sb.appendLine(
                listOf(
                    ts,
                    snapshot.name,
                    snapshot.locationLabel,
                    snapshot.floorLabel,
                    obs.ssid,
                    obs.bssid,
                    obs.vendor,
                    obs.band,
                    obs.rssi.toString(),
                    obs.frequencyMhz.toString(),
                    obs.channel.toString(),
                    obs.channelWidth,
                    obs.security,
                    obs.capabilities,
                    obs.mistApName       ?: "",
                    obs.arubaApName      ?: "",
                    lat,
                    lon,
                    acc,
                    snapshot.connectedSsid   ?: "",
                    snapshot.connectedBssid  ?: "",
                    snapshot.connectedApName ?: "",
                    mapXRel,
                    mapYRel,
                    mapXM,
                    mapYM,
                ).joinToString(",") { field -> escapeCsv(field) }
            )
        }
        return sb.toString()
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(',') || value.contains('"') || value.contains('\n')) {
            "\"${value.replace("\"", "\"\"")}\""
        } else {
            value
        }
    }

    companion object {
        private const val HEADER =
            "timestamp,snapshot_name,location,floor,ssid,bssid,vendor,band," +
            "rssi_dbm,frequency_mhz,channel,channel_width,security,capabilities," +
            "mist_ap_name,aruba_ap_name,latitude,longitude,gps_accuracy_m," +
            "connected_ssid,connected_bssid,connected_ap_name," +
            "map_x_relative,map_y_relative,map_x_meters,map_y_meters"
    }
}
