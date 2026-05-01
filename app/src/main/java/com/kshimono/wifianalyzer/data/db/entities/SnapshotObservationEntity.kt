package com.kshimono.wifianalyzer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshot_observations")
data class SnapshotObservationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val snapshotId: Long,
    val ssid: String,
    val bssid: String,
    val rssi: Int,
    val frequencyMhz: Int,
    val channel: Int,
    val channelWidth: String,
    val security: String,
    val capabilities: String,
    val vendor: String,
    val band: String,
    val mistApName: String?,
    val arubaApName: String? = null,
)
