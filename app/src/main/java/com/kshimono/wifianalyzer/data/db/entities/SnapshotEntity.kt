package com.kshimono.wifianalyzer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snapshots")
data class SnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val timestamp: Long,
    val locationLabel: String,
    val floorLabel: String,
    val note: String,
    val latitude: Double?,
    val longitude: Double?,
    val gpsAccuracy: Float? = null,
    val bssidCount: Int,
    val floorMapId: Long? = null,
    val mapX: Float? = null,
    val mapY: Float? = null,
    val connectedSsid: String? = null,
    val connectedBssid: String? = null,
    val connectedApName: String? = null,
)
