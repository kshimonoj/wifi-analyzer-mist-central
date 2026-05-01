package com.kshimono.wifianalyzer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private val ARUBA_CLUSTERS = listOf(
    "Internal"     to "internal.api.central.arubanetworks.com",
    "EU-1"         to "de1.api.central.arubanetworks.com",
    "EU-Central2"  to "de2.api.central.arubanetworks.com",
    "EU-Central3"  to "de3.api.central.arubanetworks.com",
    "UK"           to "gb1.api.central.arubanetworks.com",
    "US-1"         to "us1.api.central.arubanetworks.com",
    "US-2"         to "us2.api.central.arubanetworks.com",
    "US-WEST-4"    to "us4.api.central.arubanetworks.com",
    "US-WEST-5"    to "us5.api.central.arubanetworks.com",
    "US-East1"     to "us6.api.central.arubanetworks.com",
    "Canada-1"     to "ca1.api.central.arubanetworks.com",
    "APAC-1"       to "in1.api.central.arubanetworks.com",
    "APAC-EAST1"   to "jp1.api.central.arubanetworks.com",
    "APAC-SOUTH1"  to "au1.api.central.arubanetworks.com",
    "UAE"          to "ae1.api.central.arubanetworks.com",
    "China"        to "cn1.api.central.arubanetworks.com.cn",
)

private val REGIONS = listOf(
    "Global 01" to "api.mist.com",
    "Global 02" to "api.gc1.mist.com",
    "Global 03" to "api.ac2.mist.com",
    "Global 04" to "api.gc2.mist.com",
    "EMEA 01"   to "api.eu.mist.com",
    "EMEA 02"   to "api.gc3.mist.com",
    "EMEA 03"   to "api.ac6.mist.com",
    "APAC 01"   to "api.ac5.mist.com",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val token            by viewModel.mistToken.collectAsStateWithLifecycle()
    val region           by viewModel.mistRegion.collectAsStateWithLifecycle()
    val orgId            by viewModel.mistOrgId.collectAsStateWithLifecycle()
    val orgName          by viewModel.mistOrgName.collectAsStateWithLifecycle()
    val siteId           by viewModel.mistSiteId.collectAsStateWithLifecycle()
    val siteName         by viewModel.mistSiteName.collectAsStateWithLifecycle()
    val orgs             by viewModel.orgs.collectAsStateWithLifecycle()
    val sites            by viewModel.sites.collectAsStateWithLifecycle()
    val apCount          by viewModel.apCount.collectAsStateWithLifecycle()
    val syncStatus       by viewModel.syncStatus.collectAsStateWithLifecycle()
    val connectionResult by viewModel.connectionTestResult.collectAsStateWithLifecycle()

    val arubaClientId     by viewModel.arubaClientId.collectAsStateWithLifecycle()
    val arubaClientSecret by viewModel.arubaClientSecret.collectAsStateWithLifecycle()
    val arubaCluster      by viewModel.arubaCluster.collectAsStateWithLifecycle()
    val arubaApCount      by viewModel.arubaApCount.collectAsStateWithLifecycle()
    val arubaSyncStatus   by viewModel.arubaSyncStatus.collectAsStateWithLifecycle()
    val arubaConnResult   by viewModel.arubaConnectionTestResult.collectAsStateWithLifecycle()
    val arubaSites        by viewModel.arubaSites.collectAsStateWithLifecycle()
    val arubaSiteId       by viewModel.arubaSiteId.collectAsStateWithLifecycle()
    val arubaSiteName     by viewModel.arubaSiteName.collectAsStateWithLifecycle()

    LaunchedEffect(orgId) {
        if (orgId.isNotBlank() && sites.isEmpty()) viewModel.loadSites()
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }) }) { padding ->
        LazyColumn(
            modifier       = Modifier.padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MistConfigCard(
                token            = token,
                region           = region,
                orgId            = orgId,
                orgName          = orgName,
                siteId           = siteId,
                siteName         = siteName,
                orgs             = orgs,
                sites            = sites,
                apCount          = apCount,
                syncStatus       = syncStatus,
                connectionResult = connectionResult,
                onTokenChange    = viewModel::updateMistToken,
                onRegionChange   = viewModel::updateMistRegion,
                onTestConnection = viewModel::testConnection,
                onSelectOrg      = viewModel::selectOrg,
                onSelectSite     = { id, name -> viewModel.selectSite(id, name) },
                onSyncAps        = viewModel::syncAps,
            ) }
            item { ArubaCard(
                clientId         = arubaClientId,
                clientSecret     = arubaClientSecret,
                cluster          = arubaCluster,
                sites            = arubaSites,
                siteId           = arubaSiteId,
                siteName         = arubaSiteName,
                apCount          = arubaApCount,
                syncStatus       = arubaSyncStatus,
                connectionResult = arubaConnResult,
                onClientIdChange     = viewModel::updateArubaClientId,
                onClientSecretChange = viewModel::updateArubaClientSecret,
                onClusterChange      = viewModel::updateArubaCluster,
                onTestConnection     = viewModel::testArubaConnection,
                onSelectSite         = { id, name -> viewModel.selectArubaSite(id, name) },
                onSyncAps            = viewModel::syncArubaAps,
            ) }
        }
    }
}

@Composable
private fun MistConfigCard(
    token: String, region: String, orgId: String, orgName: String,
    siteId: String, siteName: String,
    orgs: List<com.kshimono.wifianalyzer.data.mist.MistOrg>,
    sites: List<com.kshimono.wifianalyzer.data.mist.MistSite>,
    apCount: Int, syncStatus: SyncStatus, connectionResult: String,
    onTokenChange: (String) -> Unit, onRegionChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSelectOrg: (com.kshimono.wifianalyzer.data.mist.MistOrg) -> Unit,
    onSelectSite: (String, String) -> Unit,
    onSyncAps: () -> Unit,
) {
    SectionCard(title = "Mist API Configuration") {
        // Region selector
        RegionDropdown(current = region, onSelect = onRegionChange)

        Spacer(Modifier.height(8.dp))

        // Token field
        var showToken by remember { mutableStateOf(false) }
        OutlinedTextField(
            value         = token,
            onValueChange = onTokenChange,
            label         = { Text("API Token") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            visualTransformation = if (showToken) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            trailingIcon  = {
                IconButton(onClick = { showToken = !showToken }) {
                    Icon(
                        if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showToken) "Hide" else "Show",
                    )
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        // Test connection button + result
        Button(onClick = onTestConnection, modifier = Modifier.fillMaxWidth()) {
            Text("Test Connection")
        }

        if (connectionResult.isNotEmpty()) {
            val color = when {
                connectionResult.startsWith("✓") -> Color(0xFF2E7D32)
                connectionResult.startsWith("✗") -> MaterialTheme.colorScheme.error
                else                              -> MaterialTheme.colorScheme.outline
            }
            Text(
                text  = connectionResult,
                color = color,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Org selector (shown after successful connection test)
        if (orgs.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            SimpleDropdown(
                label    = "Organization",
                current  = if (orgName.isNotBlank()) orgName else orgId,
                options  = orgs.map { it.name },
                onSelect = { name -> orgs.firstOrNull { it.name == name }?.let(onSelectOrg) },
            )
        } else if (orgId.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Org: $orgName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        // Site selector
        if (sites.isNotEmpty() || orgId.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            val siteOptions = listOf("All Sites") + sites.map { it.name }
            SimpleDropdown(
                label    = "Site",
                current  = siteName,
                options  = siteOptions,
                onSelect = { name ->
                    if (name == "All Sites") onSelectSite("all", "All Sites")
                    else sites.firstOrNull { it.name == name }
                        ?.let { onSelectSite(it.id, it.name) }
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        // Sync button + status
        when (syncStatus) {
            is SyncStatus.Syncing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Syncing APs…",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            else -> {
                OutlinedButton(onClick = onSyncAps, modifier = Modifier.fillMaxWidth()) {
                    Text("Sync APs")
                }
                when (syncStatus) {
                    is SyncStatus.Success ->
                        Text("✓ Synced ${syncStatus.count} APs",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp))
                    is SyncStatus.Error ->
                        Text("✗ ${syncStatus.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp))
                    else -> {}
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Cached APs: $apCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun RegionDropdown(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = REGIONS.firstOrNull { it.second == current }?.first ?: current
    Box {
        OutlinedButton(
            onClick        = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier       = Modifier.fillMaxWidth(),
        ) {
            Text(
                text     = "Region: $label",
                modifier = Modifier.weight(1f),
                style    = MaterialTheme.typography.bodyMedium,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            REGIONS.forEach { (displayName, host) ->
                DropdownMenuItem(
                    text    = { Text("$displayName  ($host)") },
                    onClick = { onSelect(host); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SimpleDropdown(
    label: String, current: String, options: List<String>, onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick        = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier       = Modifier.fillMaxWidth(),
        ) {
            Text(
                text     = "$label: $current",
                modifier = Modifier.weight(1f),
                style    = MaterialTheme.typography.bodyMedium,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text    = { Text(opt) },
                    onClick = { onSelect(opt); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun ArubaCard(
    clientId: String, clientSecret: String, cluster: String,
    sites: List<com.kshimono.wifianalyzer.data.aruba.ArubaSite>,
    siteId: String, siteName: String,
    apCount: Int, syncStatus: SyncStatus, connectionResult: String,
    onClientIdChange: (String) -> Unit, onClientSecretChange: (String) -> Unit,
    onClusterChange: (String) -> Unit,
    onTestConnection: () -> Unit,
    onSelectSite: (String, String) -> Unit,
    onSyncAps: () -> Unit,
) {
    SectionCard(title = "Aruba Central API Configuration") {
        // Cluster dropdown
        ArubaClusterDropdown(current = cluster, onSelect = onClusterChange)

        Spacer(Modifier.height(8.dp))

        // Client ID
        OutlinedTextField(
            value         = clientId,
            onValueChange = onClientIdChange,
            label         = { Text("Client ID") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(8.dp))

        // Client Secret
        var showSecret by remember { mutableStateOf(false) }
        OutlinedTextField(
            value         = clientSecret,
            onValueChange = onClientSecretChange,
            label         = { Text("Client Secret") },
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth(),
            visualTransformation = if (showSecret) VisualTransformation.None
                                   else PasswordVisualTransformation(),
            trailingIcon  = {
                IconButton(onClick = { showSecret = !showSecret }) {
                    Icon(
                        if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (showSecret) "Hide" else "Show",
                    )
                }
            },
        )

        Spacer(Modifier.height(8.dp))

        Button(onClick = onTestConnection, modifier = Modifier.fillMaxWidth()) {
            Text("Test Connection")
        }

        if (connectionResult.isNotEmpty()) {
            val color = when {
                connectionResult.startsWith("✓") -> Color(0xFF2E7D32)
                connectionResult.startsWith("✗") -> MaterialTheme.colorScheme.error
                else                              -> MaterialTheme.colorScheme.outline
            }
            Text(
                text  = connectionResult,
                color = color,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        // Site selector (shown after successful connection)
        if (sites.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            val siteOptions = listOf("All Sites") + sites.map { it.siteName }
            SimpleDropdown(
                label    = "Site",
                current  = siteName,
                options  = siteOptions,
                onSelect = { name ->
                    if (name == "All Sites") onSelectSite("all", "All Sites")
                    else sites.firstOrNull { it.siteName == name }
                        ?.let { onSelectSite(it.id, it.siteName) }
                },
            )
        } else if (siteId.isNotBlank() && siteId != "all") {
            Spacer(Modifier.height(8.dp))
            Text(
                text  = "Site: $siteName",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }

        Spacer(Modifier.height(12.dp))

        when (syncStatus) {
            is SyncStatus.Syncing -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    "Syncing BSSIDs…",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            else -> {
                OutlinedButton(onClick = onSyncAps, modifier = Modifier.fillMaxWidth()) {
                    Text("Sync APs")
                }
                when (syncStatus) {
                    is SyncStatus.Success ->
                        Text("✓ Synced ${syncStatus.count} BSSIDs",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp))
                    is SyncStatus.Error ->
                        Text("✗ ${syncStatus.message}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp))
                    else -> {}
                }
            }
        }

        Spacer(Modifier.height(4.dp))
        Text(
            text  = "Cached BSSIDs: $apCount",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun ArubaClusterDropdown(current: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = ARUBA_CLUSTERS.firstOrNull { it.second == current }?.first ?: current
    Box {
        OutlinedButton(
            onClick        = { expanded = true },
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier       = Modifier.fillMaxWidth(),
        ) {
            Text(
                text     = "Cluster: $label",
                modifier = Modifier.weight(1f),
                style    = MaterialTheme.typography.bodyMedium,
            )
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ARUBA_CLUSTERS.forEach { (displayName, host) ->
                DropdownMenuItem(
                    text    = { Text("$displayName  ($host)") },
                    onClick = { onSelect(host); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text       = title,
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
