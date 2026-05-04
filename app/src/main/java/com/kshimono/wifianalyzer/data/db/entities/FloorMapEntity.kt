package com.kshimono.wifianalyzer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "floor_maps")
data class FloorMapEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val source: String,               // "mist" / "aruba" / "local"
    val siteId: String?,
    val floorId: String?,
    val imageUri: String,             // file:// パス (内部ストレージ)
    val widthPx: Int,
    val heightPx: Int,
    val widthM: Double?,
    val heightM: Double?,
    val scalePixelsPerMeter: Double?,
    val createdAt: Long = System.currentTimeMillis(),
    val lengthM: Double? = null,      // Aruba: length (幅方向, メートル)
    val breadthM: Double? = null,     // Aruba: breadth (高さ方向, メートル)
)
