package com.kshimono.wifianalyzer.data.aruba

import android.util.Log
import com.kshimono.wifianalyzer.data.db.dao.ArubaApDao
import com.kshimono.wifianalyzer.data.db.entities.ArubaApEntity
import com.kshimono.wifianalyzer.data.settings.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ArubaRepo"

private fun normalizeBssid(raw: String): String =
    raw.trim().lowercase()

@Singleton
class ArubaRepositoryImpl @Inject constructor(
    private val apiClient: ArubaApiClient,
    private val dao: ArubaApDao,
    private val settings: SettingsRepository,
) : ArubaRepository {

    // Normalized BSSID (lowercase with colons) → deviceName
    @Volatile private var bssidCache: Map<String, String> = emptyMap()

    init {
        // Pre-warm cache from DB on startup (no suspend needed in init — fire-and-forget via dao.getAll())
        // We use a blocking collect on first emission in a coroutine; actual cache is loaded lazily
        // but findApNameByBssid() will just return null until cache warms.
    }

    private suspend fun refreshCache() {
        val all = dao.getAll().first()
        bssidCache = all.associate { normalizeBssid(it.bssid) to it.deviceName }
        Log.d(TAG, "Cache refreshed: ${bssidCache.size} BSSIDs")
    }

    override suspend fun testConnection(clientId: String, clientSecret: String, cluster: String): ArubaResult<Int> =
        apiClient.testConnection(clientId, clientSecret, cluster)

    override suspend fun getSites(): ArubaResult<List<ArubaSite>> {
        val clientId     = settings.arubaClientId.first()
        val clientSecret = settings.arubaClientSecret.first()
        val cluster      = settings.arubaCluster.first()
        apiClient.configure(clientId, clientSecret, cluster)
        return apiClient.getSites()
    }

    override suspend fun syncBssids(): ArubaResult<Int> {
        val clientId     = settings.arubaClientId.first()
        val clientSecret = settings.arubaClientSecret.first()
        val cluster      = settings.arubaCluster.first()
        val siteId       = settings.arubaSiteId.first()

        if (clientId.isBlank() || clientSecret.isBlank()) {
            return ArubaResult.Error("Client ID and Secret are required")
        }

        apiClient.configure(clientId, clientSecret, cluster)

        // Collect BSSIDs: by site if specified, else all sites
        val bssidsResult: ArubaResult<List<ArubaBssid>> = if (siteId.isBlank() || siteId == "all") {
            // Fetch all sites, then get BSSIDs per site and merge
            when (val sitesResult = apiClient.getSites()) {
                is ArubaResult.Error   -> ArubaResult.Error(sitesResult.message)
                is ArubaResult.Success -> {
                    val allItems = mutableListOf<ArubaBssid>()
                    for (site in sitesResult.data) {
                        when (val r = apiClient.getBssidsBySite(site.id)) {
                            is ArubaResult.Success -> allItems.addAll(r.data)
                            is ArubaResult.Error   -> Log.w(TAG, "Failed to fetch BSSIDs for site ${site.siteName}: ${r.message}")
                        }
                    }
                    ArubaResult.Success(allItems)
                }
            }
        } else {
            apiClient.getBssidsBySite(siteId)
        }

        return when (bssidsResult) {
            is ArubaResult.Error   -> ArubaResult.Error(bssidsResult.message)
            is ArubaResult.Success -> {
                val now      = System.currentTimeMillis()
                val entities = bssidsResult.data.map { item ->
                    ArubaApEntity(
                        bssid           = normalizeBssid(item.bssid),
                        deviceName      = item.deviceName,
                        wlanName        = item.wlanName,
                        siteName        = item.siteName,
                        serialNumber    = item.serialNumber,
                        radioMacAddress = item.radioMacAddress,
                        lastSynced      = now,
                    )
                }
                dao.deleteAll()
                dao.insertAll(entities)
                refreshCache()
                Log.d(TAG, "Sync complete: ${entities.size} BSSIDs stored")
                ArubaResult.Success(entities.size)
            }
        }
    }

    override fun findApNameByBssid(bssid: String): String? {
        val key  = normalizeBssid(bssid)
        val name = bssidCache[key]
        if (name != null) Log.d(TAG, "BSSID match: $bssid → $name")
        return name
    }

    override fun getAllAps(): Flow<List<ArubaApEntity>> = dao.getAll()

    override suspend fun getApCount(): Int = dao.count()
}
