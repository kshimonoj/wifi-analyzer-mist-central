package com.kshimono.wifianalyzer.domain.usecase

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
    ): String {
        val sb = StringBuilder()
        sb.appendLine(HEADER)
        val ts = dtf.format(Instant.ofEpochMilli(snapshot.timestamp))
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
                    obs.mistApName  ?: "",
                    obs.arubaApName ?: "",
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
            "rssi_dbm,frequency_mhz,channel,channel_width,security,capabilities,mist_ap_name,aruba_ap_name"
    }
}
