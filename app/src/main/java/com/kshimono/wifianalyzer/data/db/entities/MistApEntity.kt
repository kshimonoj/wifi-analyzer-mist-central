package com.kshimono.wifianalyzer.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mist_aps")
data class MistApEntity(
    @PrimaryKey val id: String,
    val name: String,
    val mac: String,
    val macPrefix: String,
    val model: String?,
    val serial: String?,
    val siteId: String,
    val siteName: String?,
    val orgId: String,
    val lastSynced: Long,
    val radioPrefixes: String = "",
)
