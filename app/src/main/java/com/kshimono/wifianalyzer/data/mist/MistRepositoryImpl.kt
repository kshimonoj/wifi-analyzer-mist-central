package com.kshimono.wifianalyzer.data.mist

import android.util.Log
import com.kshimono.wifianalyzer.data.db.dao.MistApDao
import com.kshimono.wifianalyzer.data.db.entities.MistApEntity
import com.kshimono.wifianalyzer.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MistRepo"

private fun normalizeMac(raw: String): String =
    raw.replace(":", "").replace("-", "").trim().lowercase()

@Singleton
class MistRepositoryImpl @Inject constructor(
    private val apiClient: MistApiClient,
    private val dao: MistApDao,
    private val settings: SettingsRepository,
) : MistRepository {

    // 11-char radio MAC prefix → AP name
    @Volatile private var radioPrefixCache: Map<String, String> = emptyMap()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        scope.launch { refreshCache() }
    }

    private suspend fun refreshCache() {
        val all = dao.getAll().first()
        val map = mutableMapOf<String, String>()
        for (entity in all) {
            if (entity.radioPrefixes.isNotEmpty()) {
                entity.radioPrefixes.split(",").forEach { prefix ->
                    if (prefix.isNotEmpty()) map[prefix] = entity.name
                }
            }
        }
        radioPrefixCache = map
        Log.d(TAG, "Cache refreshed: ${map.size} radio prefixes from ${all.size} APs")
    }

    override suspend fun testConnection(token: String, region: String): MistResult<List<MistOrg>> {
        apiClient.configure(token, region)
        return apiClient.getSelf()
    }

    override suspend fun getSites(orgId: String): MistResult<List<MistSite>> {
        val token  = settings.mistToken.first()
        val region = settings.mistRegion.first()
        apiClient.configure(token, region)
        return apiClient.getSites(orgId)
    }

    override suspend fun syncAps(orgId: String, siteId: String?): MistResult<Int> {
        val token  = settings.mistToken.first()
        val region = settings.mistRegion.first()
        apiClient.configure(token, region)

        val apsResult = apiClient.getAps(orgId, siteId)
        if (apsResult is MistResult.Error) return MistResult.Error(apsResult.message)
        val aps = (apsResult as MistResult.Success).data

        // Build ethernet MAC → radio MACs lookup
        val radioLookup: Map<String, List<String>> = when (val r = apiClient.getRadioMacs(orgId)) {
            is MistResult.Success -> r.data.associate {
                normalizeMac(it.mac) to it.radioMac.map { rm -> normalizeMac(rm) }
            }
            is MistResult.Error -> {
                Log.w(TAG, "radio_macs fetch failed: ${r.message}, falling back to empty")
                emptyMap()
            }
        }

        val now = System.currentTimeMillis()
        val entities = aps.map { ap ->
            val normalizedMac  = normalizeMac(ap.mac)
            val macPrefix      = normalizedMac.take(10)
            val radioMacs      = radioLookup[normalizedMac] ?: emptyList()
            val radioPrefixes  = radioMacs.map { it.take(11) }.distinct().joinToString(",")
            Log.d(TAG, "AP sync: name=${ap.name} mac=$normalizedMac macPrefix=$macPrefix radioPrefixes=$radioPrefixes")
            MistApEntity(
                id            = ap.id.ifBlank { normalizedMac },
                name          = ap.name,
                mac           = normalizedMac,
                macPrefix     = macPrefix,
                model         = ap.model,
                serial        = ap.serial,
                siteId        = ap.siteId ?: "",
                siteName      = null,
                orgId         = ap.orgId ?: orgId,
                lastSynced    = now,
                radioPrefixes = radioPrefixes,
            )
        }

        // Only replace DB after successful fetch
        dao.deleteByOrgId(orgId)
        dao.insertAll(entities)
        refreshCache()
        Log.d(TAG, "Sync complete: ${entities.size} APs stored, ${radioPrefixCache.size} radio prefixes cached")
        return MistResult.Success(entities.size)
    }

    override fun findApNameByBssid(bssid: String): String? {
        val prefix = normalizeMac(bssid).take(11)
        val name   = radioPrefixCache[prefix]
        if (name != null) Log.d(TAG, "BSSID match: bssid=$bssid prefix=$prefix → $name")
        return name
    }

    override fun getAllAps(): Flow<List<MistApEntity>> = dao.getAll()

    override suspend fun getApCount(): Int = dao.count()
}
