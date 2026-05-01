package com.kshimono.wifianalyzer.domain.model

data class BssidSummary(
    val observation: WifiObservation,
    val band: String,
) {
    companion object {
        fun toBand(frequencyMhz: Int): String = when (frequencyMhz) {
            in 2400..2500 -> "2.4 GHz"
            in 5000..5900 -> "5 GHz"
            in 5925..7125 -> "6 GHz"
            else          -> "Unknown"
        }
    }
}
