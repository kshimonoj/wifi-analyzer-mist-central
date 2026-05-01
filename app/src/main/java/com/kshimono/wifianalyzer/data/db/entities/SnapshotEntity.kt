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
    val bssidCount: Int,
)
