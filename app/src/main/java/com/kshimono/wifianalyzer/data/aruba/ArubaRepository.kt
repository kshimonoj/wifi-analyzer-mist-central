package com.kshimono.wifianalyzer.data.aruba

import com.kshimono.wifianalyzer.data.db.entities.ArubaApEntity
import kotlinx.coroutines.flow.Flow

interface ArubaRepository {
    suspend fun testConnection(clientId: String, clientSecret: String, cluster: String): ArubaResult<Int>
    suspend fun getSites(): ArubaResult<List<ArubaSite>>
    suspend fun syncBssids(): ArubaResult<Int>
    fun findApNameByBssid(bssid: String): String?
    fun getAllAps(): Flow<List<ArubaApEntity>>
    suspend fun getApCount(): Int
}
