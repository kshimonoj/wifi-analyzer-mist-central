package com.kshimono.wifianalyzer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ap_locations")
data class ApLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val floorMapId: Long,
    val apName: String,
    val macAddress: String?,
    val model: String?,
    val source: String,        // "mist" / "aruba"
    val mapX: Float,
    val mapY: Float,
    val status: String?,
    val radiosJson: String?,
    val lastSynced: Long = System.currentTimeMillis(),
)
