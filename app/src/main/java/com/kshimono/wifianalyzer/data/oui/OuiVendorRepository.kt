package com.kshimono.wifianalyzer.data.oui

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OuiVendorRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val ouiMap: Map<String, String> by lazy {
        try { loadOuiMap() } catch (_: Exception) { emptyMap() }
    }

    fun getVendor(bssid: String): String {
        val prefix = bssid.replace(":", "").take(6).uppercase()
        return ouiMap[prefix] ?: "Unknown"
    }

    private fun loadOuiMap(): Map<String, String> {
        val map = HashMap<String, String>(40_000)
        context.assets.open("oui.csv").bufferedReader().use { reader ->
            var firstLine = true
            reader.forEachLine { raw ->
                val line = raw.trimEnd()
                if (firstLine) { firstLine = false; return@forEachLine }
                val parts = parseCsvLine(line)
                if (parts.size >= 3) {
                    val registry   = parts[0].trim()
                    val assignment = parts[1].trim().uppercase()
                    val orgName    = parts[2].trim()
                    // MA-L (6-char hex) = OUI prefix for the first 3 octets
                    if (registry == "MA-L" && assignment.length == 6) {
                        map[assignment] = orgName
                    }
                }
            }
        }
        return map
    }

    private fun parseCsvLine(line: String): List<String> {
        val result  = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        for (ch in line) {
            when {
                ch == '"'            -> inQuotes = !inQuotes
                ch == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }
                else                 -> current.append(ch)
            }
        }
        result.add(current.toString())
        return result
    }
}
