package com.kshimono.wifianalyzer.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private object Keys {
    val MIST_TOKEN         = stringPreferencesKey("mist_token")
    val MIST_REGION        = stringPreferencesKey("mist_region")
    val MIST_ORG_ID        = stringPreferencesKey("mist_org_id")
    val MIST_ORG_NAME      = stringPreferencesKey("mist_org_name")
    val MIST_SITE_ID       = stringPreferencesKey("mist_site_id")
    val MIST_SITE_NAME     = stringPreferencesKey("mist_site_name")
    val ARUBA_CLIENT_ID     = stringPreferencesKey("aruba_client_id")
    val ARUBA_CLIENT_SECRET = stringPreferencesKey("aruba_client_secret")
    val ARUBA_CLUSTER       = stringPreferencesKey("aruba_cluster")
    val ARUBA_SITE_ID       = stringPreferencesKey("aruba_site_id")
    val ARUBA_SITE_NAME     = stringPreferencesKey("aruba_site_name")
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    override val mistToken: Flow<String>
        get() = dataStore.data.map { it[Keys.MIST_TOKEN] ?: "" }
    override val mistRegion: Flow<String>
        get() = dataStore.data.map { it[Keys.MIST_REGION] ?: "api.ac2.mist.com" }
    override val mistOrgId: Flow<String>
        get() = dataStore.data.map { it[Keys.MIST_ORG_ID] ?: "" }
    override val mistOrgName: Flow<String>
        get() = dataStore.data.map { it[Keys.MIST_ORG_NAME] ?: "" }
    override val mistSiteId: Flow<String>
        get() = dataStore.data.map { it[Keys.MIST_SITE_ID] ?: "all" }
    override val mistSiteName: Flow<String>
        get() = dataStore.data.map { it[Keys.MIST_SITE_NAME] ?: "All Sites" }
    override val arubaClientId: Flow<String>
        get() = dataStore.data.map { it[Keys.ARUBA_CLIENT_ID] ?: "" }
    override val arubaClientSecret: Flow<String>
        get() = dataStore.data.map { it[Keys.ARUBA_CLIENT_SECRET] ?: "" }
    override val arubaCluster: Flow<String>
        get() = dataStore.data.map { it[Keys.ARUBA_CLUSTER] ?: "internal.api.central.arubanetworks.com" }
    override val arubaSiteId: Flow<String>
        get() = dataStore.data.map { it[Keys.ARUBA_SITE_ID] ?: "" }
    override val arubaSiteName: Flow<String>
        get() = dataStore.data.map { it[Keys.ARUBA_SITE_NAME] ?: "All Sites" }

    override suspend fun setMistToken(value: String)        { dataStore.edit { it[Keys.MIST_TOKEN] = value } }
    override suspend fun setMistRegion(value: String)       { dataStore.edit { it[Keys.MIST_REGION] = value } }
    override suspend fun setMistOrgId(value: String)        { dataStore.edit { it[Keys.MIST_ORG_ID] = value } }
    override suspend fun setMistOrgName(value: String)      { dataStore.edit { it[Keys.MIST_ORG_NAME] = value } }
    override suspend fun setMistSiteId(value: String)       { dataStore.edit { it[Keys.MIST_SITE_ID] = value } }
    override suspend fun setMistSiteName(value: String)     { dataStore.edit { it[Keys.MIST_SITE_NAME] = value } }
    override suspend fun setArubaClientId(value: String)    { dataStore.edit { it[Keys.ARUBA_CLIENT_ID] = value } }
    override suspend fun setArubaClientSecret(value: String){ dataStore.edit { it[Keys.ARUBA_CLIENT_SECRET] = value } }
    override suspend fun setArubaCluster(value: String)     { dataStore.edit { it[Keys.ARUBA_CLUSTER]   = value } }
    override suspend fun setArubaSiteId(value: String)      { dataStore.edit { it[Keys.ARUBA_SITE_ID]   = value } }
    override suspend fun setArubaSiteName(value: String)    { dataStore.edit { it[Keys.ARUBA_SITE_NAME] = value } }
}
