package com.kshimono.wifianalyzer.ui

import android.net.Uri
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kshimono.wifianalyzer.ui.compare.CompareGraphScreen
import com.kshimono.wifianalyzer.ui.monitor.MonitorScreen
import com.kshimono.wifianalyzer.ui.scan.ScanScreen
import com.kshimono.wifianalyzer.ui.settings.SettingsScreen
import com.kshimono.wifianalyzer.ui.snapshot.SnapshotListScreen

private const val ROUTE_SCAN      = "scan"
private const val ROUTE_SNAPSHOTS = "snapshots"
private const val ROUTE_MONITOR   = "monitor"
private const val ROUTE_SETTINGS  = "settings"
private const val ROUTE_COMPARE   = "compare/{bssids}"

private val TOP_LEVEL_ROUTES = setOf(ROUTE_SCAN, ROUTE_SNAPSHOTS, ROUTE_MONITOR, ROUTE_SETTINGS)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    val showBottomBar = currentRoute in TOP_LEVEL_ROUTES || currentRoute == null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    listOf(
                        Triple(ROUTE_SCAN,      Icons.Filled.Wifi,      "Scan"),
                        Triple(ROUTE_SNAPSHOTS, Icons.Filled.Save,      "Snapshots"),
                        Triple(ROUTE_MONITOR,   Icons.Filled.ShowChart, "Monitor"),
                        Triple(ROUTE_SETTINGS,  Icons.Filled.Settings,  "Settings"),
                    ).forEach { (route, icon, label) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick  = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = ROUTE_SCAN,
            modifier         = Modifier.padding(padding),
        ) {
            composable(ROUTE_SCAN) {
                ScanScreen(
                    onNavigateToCompare = { bssidsRaw ->
                        val encoded = Uri.encode(bssidsRaw)
                        navController.navigate("compare/$encoded")
                    },
                )
            }
            composable(ROUTE_SNAPSHOTS) { SnapshotListScreen() }
            composable(ROUTE_MONITOR)   { MonitorScreen() }
            composable(ROUTE_SETTINGS)  { SettingsScreen() }
            composable(
                route     = ROUTE_COMPARE,
                arguments = listOf(navArgument("bssids") { type = NavType.StringType }),
            ) { backStackEntry ->
                val encoded = backStackEntry.arguments?.getString("bssids") ?: ""
                val decoded = Uri.decode(encoded)
                CompareGraphScreen(bssidsArg = decoded)
            }
        }
    }
}
