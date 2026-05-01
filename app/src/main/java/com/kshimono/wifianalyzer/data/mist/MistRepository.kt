package com.kshimono.wifianalyzer.data.mist

import com.kshimono.wifianalyzer.data.db.entities.MistApEntity
import kotlinx.coroutines.flow.Flow

interface MistRepository {
    suspend fun testConnection(token: String, region: String): MistResult<List<MistOrg>>
    suspend fun getSites(orgId: String): MistResult<List<MistSite>>
    suspend fun syncAps(orgId: String, siteId: String?): MistResult<Int>
    fun findApNameByBssid(bssid: String): String?
    fun getAllAps(): Flow<List<MistApEntity>>
    suspend fun getApCount(): Int
}
