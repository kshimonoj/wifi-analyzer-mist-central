package com.kshimono.wifianalyzer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "aruba_aps")
data class ArubaApEntity(
    @PrimaryKey val bssid: String,
    val deviceName: String,
    val wlanName: String,
    val siteName: String?,
    val serialNumber: String?,
    val radioMacAddress: String?,
    val lastSynced: Long,
)
