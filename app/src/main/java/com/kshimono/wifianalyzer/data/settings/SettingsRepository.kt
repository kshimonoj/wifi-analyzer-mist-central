package com.kshimono.wifianalyzer.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val mistToken: Flow<String>
    val mistRegion: Flow<String>
    val mistOrgId: Flow<String>
    val mistOrgName: Flow<String>
    val mistSiteId: Flow<String>
    val mistSiteName: Flow<String>
    val arubaClientId: Flow<String>
    val arubaClientSecret: Flow<String>
    val arubaCluster: Flow<String>
    val arubaSiteId: Flow<String>
    val arubaSiteName: Flow<String>

    suspend fun setMistToken(value: String)
    suspend fun setMistRegion(value: String)
    suspend fun setMistOrgId(value: String)
    suspend fun setMistOrgName(value: String)
    suspend fun setMistSiteId(value: String)
    suspend fun setMistSiteName(value: String)
    suspend fun setArubaClientId(value: String)
    suspend fun setArubaClientSecret(value: String)
    suspend fun setArubaCluster(value: String)
    suspend fun setArubaSiteId(value: String)
    suspend fun setArubaSiteName(value: String)
}
