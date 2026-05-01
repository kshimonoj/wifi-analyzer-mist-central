package com.kshimono.wifianalyzer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kshimono.wifianalyzer.ui.AppNavigation
import com.kshimono.wifianalyzer.ui.theme.WifiAnalyzerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WifiAnalyzerTheme {
                AppNavigation()
            }
        }
    }
}
