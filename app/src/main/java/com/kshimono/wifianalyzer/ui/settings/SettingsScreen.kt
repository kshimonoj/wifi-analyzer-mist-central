package com.kshimono.wifianalyzer.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kshimono.wifianalyzer.data.aruba.ArubaBuilding
import com.kshimono.wifianalyzer.data.aruba.ArubaFloor
import com.kshimono.wifianalyzer.data.db.entities.FloorMapEntity
import com.kshimono.wifianalyzer.data.mist.MistMap

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

    val floorMaps        by viewModel.floorMaps.collectAsStateWithLifecycle()
    val mistMaps         by viewModel.mistMaps.collectAsStateWithLifecycle()
    val arubaBuildings   by viewModel.arubaBuildings.collectAsStateWithLifecycle()
    val mapImportStatus  by viewModel.mapImportStatus.collectAsStateWithLifecycle()
    val mistSiteId       by viewModel.mistSiteId.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var fileNameDialogUri by remember { mutableStateOf<Uri?>(null) }
    var pendingFileName   by remember { mutableStateOf("") }

    val fileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingFileName = context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx).substringBeforeLast(".") else null
                    } else null
                } ?: "Floor Map"
            fileNameDialogUri = uri
        }
    }

    if (fileNameDialogUri != null) {
        AlertDialog(
            onDismissRequest = { fileNameDialogUri = null },
            title = { Text("Map Name") },
            text  = {
                OutlinedTextField(
                    value         = pendingFileName,
                    onValueChange = { pendingFileName = it },
                    label         = { Text("Name") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importLocalFile(fileNameDialogUri!!, pendingFileName.ifBlank { "Floor Map" })
                        fileNameDialogUri = null
                    }
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { fileNameDialogUri = null }) { Text("Cancel") }
            },
        )
    }

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
            item {
                FloorMapsCard(
                    floorMaps        = floorMaps,
                    mistMaps         = mistMaps,
                    arubaBuildings   = arubaBuildings,
                    mapImportStatus  = mapImportStatus,
                    mistSiteConfigured  = mistSiteId.isNotBlank() && mistSiteId != "all",
                    arubaSiteConfigured = arubaSiteId.isNotBlank(),
                    arubaSiteId      = arubaSiteId,
                    onLoadMistMaps   = viewModel::loadMistMaps,
                    onLoadArubaBuildings = viewModel::loadArubaBuildings,
                    onImportFromMist    = viewModel::importFromMist,
                    onImportFromAruba   = { siteId, floor, bldName -> viewModel.importFromAruba(siteId, floor, bldName) },
                    onImportLocal       = { fileLauncher.launch(arrayOf("image/png", "image/jpeg", "application/pdf")) },
                    onDelete            = viewModel::deleteFloorMap,
                )
            }
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
            Text("Connect")
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
            Text("Connect")
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
private fun FloorMapsCard(
    floorMaps: List<FloorMapEntity>,
    mistMaps: List<MistMap>,
    arubaBuildings: List<ArubaBuilding>,
    mapImportStatus: MapImportStatus,
    mistSiteConfigured: Boolean,
    arubaSiteConfigured: Boolean,
    arubaSiteId: String,
    onLoadMistMaps: () -> Unit,
    onLoadArubaBuildings: () -> Unit,
    onImportFromMist: (MistMap) -> Unit,
    onImportFromAruba: (String, ArubaFloor, String) -> Unit,
    onImportLocal: () -> Unit,
    onDelete: (Long) -> Unit,
) {
    SectionCard(title = "Floor Maps") {

        // Mist section
        Text("Mist", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick  = onLoadMistMaps,
            enabled  = mistSiteConfigured && mapImportStatus !is MapImportStatus.Loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Load Maps from Mist") }
        if (mistMaps.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            mistMaps.forEach { map ->
                Row(
                    modifier          = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(map.name, style = MaterialTheme.typography.bodySmall)
                        if (map.widthM != null && map.heightM != null) {
                            Text(
                                "%.1fm × %.1fm".format(map.widthM, map.heightM),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                    TextButton(onClick = { onImportFromMist(map) }) { Text("Import") }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // Aruba section
        Text("Aruba Central", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick  = onLoadArubaBuildings,
            enabled  = arubaSiteConfigured && mapImportStatus !is MapImportStatus.Loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Load Maps from Aruba") }
        if (arubaBuildings.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            arubaBuildings.forEach { building ->
                val bldName = building.properties.name ?: "Building"
                Text(bldName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                building.floors.forEach { floor ->
                    Row(
                        modifier          = Modifier.fillMaxWidth().padding(start = 12.dp, top = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            floor.properties.name ?: "Floor ${floor.properties.ordinal ?: ""}",
                            style    = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onImportFromAruba(arubaSiteId, floor, bldName) }) {
                            Text("Import")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Divider()
        Spacer(Modifier.height(12.dp))

        // Local file
        Text("Local File", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        OutlinedButton(
            onClick  = onImportLocal,
            enabled  = mapImportStatus !is MapImportStatus.Loading,
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Import from File (PNG / JPEG / PDF)") }

        // Status
        Spacer(Modifier.height(8.dp))
        when (mapImportStatus) {
            is MapImportStatus.Loading ->
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            is MapImportStatus.Success ->
                Text("✓ Imported: ${mapImportStatus.name}", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
            is MapImportStatus.Error ->
                Text("✗ ${mapImportStatus.message}", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            else -> {}
        }

        // Saved maps list
        if (floorMaps.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Divider()
            Spacer(Modifier.height(8.dp))
            Text("Saved Maps (${floorMaps.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            floorMaps.forEach { map ->
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint     = when (map.source) {
                            "mist"  -> Color(0xFF1565C0)
                            "aruba" -> Color(0xFF2E7D32)
                            else    -> MaterialTheme.colorScheme.outline
                        },
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text     = map.name,
                        style    = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text  = map.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    IconButton(onClick = { onDelete(map.id) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    }
                }
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
