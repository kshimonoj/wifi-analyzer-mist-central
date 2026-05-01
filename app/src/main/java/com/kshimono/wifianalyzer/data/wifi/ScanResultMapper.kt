package com.kshimono.wifianalyzer.data.wifi

import android.net.wifi.ScanResult
import android.os.Build
import com.kshimono.wifianalyzer.domain.model.WifiObservation

object ScanResultMapper {

    fun map(src: ScanResult): WifiObservation = WifiObservation(
        bssid        = src.BSSID.orEmpty(),
        ssid         = src.SSID.orEmpty(),
        rssi         = src.level,
        frequencyMhz = src.frequency,
        channel      = frequencyToChannel(src.frequency),
        channelWidth = channelWidthLabel(src.channelWidth),
        security     = detectSecurity(src.capabilities),
        capabilities = src.capabilities.orEmpty(),
    )

    fun frequencyToChannel(freqMhz: Int): Int = when {
        freqMhz == 2484          -> 14
        freqMhz in 2412..2472    -> (freqMhz - 2412) / 5 + 1
        freqMhz in 5160..5885    -> (freqMhz - 5000) / 5
        freqMhz in 5955..7115    -> (freqMhz - 5955) / 5 + 1  // 6 GHz
        else                     -> 0
    }

    fun channelWidthLabel(width: Int): String = when (width) {
        ScanResult.CHANNEL_WIDTH_20MHZ          -> "20 MHz"
        ScanResult.CHANNEL_WIDTH_40MHZ          -> "40 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ          -> "80 MHz"
        ScanResult.CHANNEL_WIDTH_160MHZ         -> "160 MHz"
        ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> "80+80 MHz"
        else -> if (Build.VERSION.SDK_INT >= 33 &&
            width == ScanResult.CHANNEL_WIDTH_320MHZ) "320 MHz" else "?"
    }

    fun detectSecurity(capabilities: String?): String {
        val cap = capabilities ?: return "Open"
        return when {
            cap.contains("SAE")  -> "WPA3"
            cap.contains("RSN")  -> "WPA2"
            cap.contains("WPA")  -> "WPA"
            cap.contains("WEP")  -> "WEP"
            cap.contains("OWE")  -> "OWE"
            else                 -> "Open"
        }
    }
}
