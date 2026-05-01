package com.kshimono.wifianalyzer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kshimono.wifianalyzer.data.aruba.ArubaRepository
import com.kshimono.wifianalyzer.data.aruba.ArubaResult
import com.kshimono.wifianalyzer.data.aruba.ArubaSite
import com.kshimono.wifianalyzer.data.mist.MistOrg
import com.kshimono.wifianalyzer.data.mist.MistRepository
import com.kshimono.wifianalyzer.data.mist.MistResult
import com.kshimono.wifianalyzer.data.mist.MistSite
import com.kshimono.wifianalyzer.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    data class Success(val count: Int) : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val mistRepository: MistRepository,
    private val arubaRepository: ArubaRepository,
) : ViewModel() {

    val mistToken    = settings.mistToken   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val mistRegion   = settings.mistRegion  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "api.ac2.mist.com")
    val mistOrgId    = settings.mistOrgId   .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val mistOrgName  = settings.mistOrgName .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val mistSiteId   = settings.mistSiteId  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "all")
    val mistSiteName = settings.mistSiteName.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "All Sites")

    val arubaClientId     = settings.arubaClientId    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val arubaClientSecret = settings.arubaClientSecret.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val arubaCluster      = settings.arubaCluster     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "internal.api.central.arubanetworks.com")
    val arubaSiteId       = settings.arubaSiteId      .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")
    val arubaSiteName     = settings.arubaSiteName    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "All Sites")

    private val _orgs = MutableStateFlow<List<MistOrg>>(emptyList())
    val orgs: StateFlow<List<MistOrg>> = _orgs.asStateFlow()

    private val _sites = MutableStateFlow<List<MistSite>>(emptyList())
    val sites: StateFlow<List<MistSite>> = _sites.asStateFlow()

    private val _apCount = MutableStateFlow(0)
    val apCount: StateFlow<Int> = _apCount.asStateFlow()

    private val _syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _connectionTestResult = MutableStateFlow("")
    val connectionTestResult: StateFlow<String> = _connectionTestResult.asStateFlow()

    private val _arubaApCount = MutableStateFlow(0)
    val arubaApCount: StateFlow<Int> = _arubaApCount.asStateFlow()

    private val _arubaSyncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val arubaSyncStatus: StateFlow<SyncStatus> = _arubaSyncStatus.asStateFlow()

    private val _arubaConnectionTestResult = MutableStateFlow("")
    val arubaConnectionTestResult: StateFlow<String> = _arubaConnectionTestResult.asStateFlow()

    private val _arubaSites = MutableStateFlow<List<ArubaSite>>(emptyList())
    val arubaSites: StateFlow<List<ArubaSite>> = _arubaSites.asStateFlow()

    init {
        viewModelScope.launch { _apCount.value      = mistRepository.getApCount()  }
        viewModelScope.launch { _arubaApCount.value = arubaRepository.getApCount() }
    }

    fun updateMistToken(value: String)       { viewModelScope.launch { settings.setMistToken(value)       } }
    fun updateMistRegion(value: String)      { viewModelScope.launch { settings.setMistRegion(value)      } }
    fun updateArubaClientId(value: String)    { viewModelScope.launch { settings.setArubaClientId(value)    } }
    fun updateArubaClientSecret(value: String){ viewModelScope.launch { settings.setArubaClientSecret(value) } }
    fun updateArubaCluster(value: String)     { viewModelScope.launch { settings.setArubaCluster(value)     } }

    fun testConnection() {
        viewModelScope.launch {
            _connectionTestResult.value = "Testing…"
            _orgs.value = emptyList()
            val token  = mistToken.value
            val region = mistRegion.value
            when (val result = mistRepository.testConnection(token, region)) {
                is MistResult.Success -> {
                    _orgs.value = result.data
                    _connectionTestResult.value = if (result.data.isNotEmpty())
                        "✓ Connected: ${result.data.first().name}"
                    else
                        "✓ Connected (no orgs found)"
                }
                is MistResult.Error -> {
                    _connectionTestResult.value = "✗ Error: ${result.message}"
                }
            }
        }
    }

    fun selectOrg(org: MistOrg) {
        viewModelScope.launch {
            settings.setMistOrgId(org.id)
            settings.setMistOrgName(org.name)
            settings.setMistSiteId("all")
            settings.setMistSiteName("All Sites")
            _sites.value = emptyList()
            loadSites()
        }
    }

    fun loadSites() {
        viewModelScope.launch {
            val orgId = mistOrgId.value
            if (orgId.isBlank()) return@launch
            when (val result = mistRepository.getSites(orgId)) {
                is MistResult.Success -> _sites.value = result.data
                is MistResult.Error   -> { /* keep empty */ }
            }
        }
    }

    fun selectSite(siteId: String, siteName: String) {
        viewModelScope.launch {
            settings.setMistSiteId(siteId)
            settings.setMistSiteName(siteName)
        }
    }

    fun syncAps() {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.Syncing
            val orgId  = mistOrgId.value
            val siteId = mistSiteId.value
            if (orgId.isBlank()) {
                _syncStatus.value = SyncStatus.Error("Org not configured")
                return@launch
            }
            when (val result = mistRepository.syncAps(orgId, siteId)) {
                is MistResult.Success -> {
                    _apCount.value = result.data
                    _syncStatus.value = SyncStatus.Success(result.data)
                }
                is MistResult.Error -> {
                    _syncStatus.value = SyncStatus.Error(result.message)
                }
            }
        }
    }

    fun testArubaConnection() {
        viewModelScope.launch {
            _arubaConnectionTestResult.value = "Testing…"
            _arubaSites.value = emptyList()
            val clientId     = arubaClientId.value
            val clientSecret = arubaClientSecret.value
            val cluster      = arubaCluster.value
            android.util.Log.d("ArubaApi", "testConnection: clientId=${clientId.take(8)} len=${clientId.length}, cluster=$cluster")
            when (val result = arubaRepository.testConnection(clientId, clientSecret, cluster)) {
                is ArubaResult.Success -> {
                    _arubaConnectionTestResult.value = "✓ Connected: ${result.data} BSSIDs found"
                    loadArubaSites()
                }
                is ArubaResult.Error ->
                    _arubaConnectionTestResult.value = "✗ Error: ${result.message}"
            }
        }
    }

    fun loadArubaSites() {
        viewModelScope.launch {
            when (val result = arubaRepository.getSites()) {
                is ArubaResult.Success -> _arubaSites.value = result.data
                is ArubaResult.Error   -> { /* keep empty */ }
            }
        }
    }

    fun selectArubaSite(siteId: String, siteName: String) {
        viewModelScope.launch {
            settings.setArubaSiteId(siteId)
            settings.setArubaSiteName(siteName)
        }
    }

    fun syncArubaAps() {
        viewModelScope.launch {
            _arubaSyncStatus.value = SyncStatus.Syncing
            when (val result = arubaRepository.syncBssids()) {
                is ArubaResult.Success -> {
                    _arubaApCount.value    = result.data
                    _arubaSyncStatus.value = SyncStatus.Success(result.data)
                }
                is ArubaResult.Error -> {
                    _arubaSyncStatus.value = SyncStatus.Error(result.message)
                }
            }
        }
    }
}
