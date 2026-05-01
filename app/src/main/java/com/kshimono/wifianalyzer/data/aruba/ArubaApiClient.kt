package com.kshimono.wifianalyzer.data.aruba

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "ArubaApi"
private const val TOKEN_URL = "https://sso.common.cloud.hpe.com/as/token.oauth2"

@Serializable
data class ArubaSite(
    val id: String,
    val siteName: String,
)

@Serializable
data class ArubaSiteResponse(
    val items: List<ArubaSite> = emptyList(),
    val total: Int? = null,
)

@Serializable
data class ArubaBssid(
    val bssid: String,
    val deviceName: String,
    val wlanName: String,
    val siteName: String? = null,
    val siteId: String? = null,
    val serialNumber: String? = null,
    val radioMacAddress: String? = null,
    val macAddress: String? = null,
    val radioNumber: Int? = null,
    val clientCount: Int? = null,
)

@Serializable
data class ArubaBssidResponse(
    val total: Int = 0,
    val count: Int = 0,
    val next: String? = null,
    val items: List<ArubaBssid> = emptyList(),
)

sealed class ArubaResult<out T> {
    data class Success<T>(val data: T) : ArubaResult<T>()
    data class Error<T>(val message: String) : ArubaResult<T>()
}

@Singleton
class ArubaApiClient @Inject constructor() {

    private var clientId: String = ""
    private var clientSecret: String = ""
    private var baseUrl: String = ""
    private var accessToken: String = ""
    private var tokenExpiresAt: Long = 0L

    // Used for BSSID API calls (JSON responses)
    private val http = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
        install(Logging) { level = LogLevel.HEADERS }
    }


    fun configure(clientId: String, clientSecret: String, cluster: String) {
        if (this.clientId != clientId || this.clientSecret != clientSecret || this.baseUrl != "https://$cluster") {
            // Credentials changed — invalidate token
            tokenExpiresAt = 0L
            accessToken = ""
        }
        this.clientId = clientId
        this.clientSecret = clientSecret
        this.baseUrl = "https://$cluster"
    }

    private suspend fun ensureValidToken() {
        if (accessToken.isNotEmpty() && System.currentTimeMillis() < tokenExpiresAt - 60_000L) return
        Log.d(TAG, "Fetching new token for clientId: ${clientId.trim().take(8)}...")
        val body = "grant_type=client_credentials" +
                   "&client_id=${clientId.trim()}" +
                   "&client_secret=${clientSecret.trim()}"
        val bodyBytes = body.toByteArray(Charsets.UTF_8)
        val responseText = withContext(Dispatchers.IO) {
            val conn = URL(TOKEN_URL).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("Content-Length", bodyBytes.size.toString())
                conn.doOutput = true
                conn.connectTimeout = 15_000
                conn.readTimeout    = 15_000
                conn.outputStream.use { it.write(bodyBytes) }
                val status = conn.responseCode
                val text = (if (status < 400) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.readText() ?: ""
                Log.d(TAG, "Token status: $status")
                Log.d(TAG, "Token body: ${text.take(300)}")
                if (status != 200) throw Exception("Token request failed ($status): $text")
                text
            } finally {
                conn.disconnect()
            }
        }
        val jsonObj = JSONObject(responseText)
        if (!jsonObj.has("access_token")) {
            throw Exception("Token response missing access_token: $responseText")
        }
        accessToken    = jsonObj.getString("access_token")
        tokenExpiresAt = System.currentTimeMillis() + jsonObj.getLong("expires_in") * 1000L
        Log.d(TAG, "Token obtained: ${accessToken.take(20)}...")
    }

    suspend fun getSites(): ArubaResult<List<ArubaSite>> = runCatching {
        ensureValidToken()
        val resp = http.get("$baseUrl/network-monitoring/v1/sites-health") {
            header("Authorization", "Bearer $accessToken")
            parameter("limit", 1000)
        }.body<ArubaSiteResponse>()
        Log.d(TAG, "Fetched ${resp.items.size} sites")
        ArubaResult.Success(resp.items)
    }.getOrElse { e -> ArubaResult.Error(e.message ?: "Failed to fetch sites") }

    suspend fun getBssidsBySite(siteId: String): ArubaResult<List<ArubaBssid>> = runCatching {
        ensureValidToken()
        val allItems = mutableListOf<ArubaBssid>()
        var offset = 0
        val limit  = 1000
        while (true) {
            val data = http.get("$baseUrl/network-monitoring/v1/bssids") {
                header("Authorization", "Bearer $accessToken")
                parameter("filter", "siteId eq $siteId")
                parameter("limit",  limit)
                parameter("offset", offset)
            }.body<ArubaBssidResponse>()
            allItems.addAll(data.items)
            Log.d(TAG, "Fetched ${data.items.size} BSSIDs for siteId=$siteId (offset=$offset, total=${data.total})")
            if (data.next == null || allItems.size >= data.total) break
            offset += limit
        }
        ArubaResult.Success(allItems)
    }.getOrElse { e -> ArubaResult.Error(e.message ?: "Failed to fetch BSSIDs for site") }

    suspend fun testConnection(clientId: String, clientSecret: String, cluster: String): ArubaResult<Int> =
        runCatching {
            configure(clientId, clientSecret, cluster)
            ensureValidToken()
            when (val result = getAllBssids()) {
                is ArubaResult.Success -> ArubaResult.Success(result.data.size)
                is ArubaResult.Error   -> ArubaResult.Error(result.message)
            }
        }.getOrElse { e -> ArubaResult.Error(e.message ?: "Connection failed") }

    suspend fun getAllBssids(): ArubaResult<List<ArubaBssid>> = runCatching {
        ensureValidToken()
        val allItems = mutableListOf<ArubaBssid>()
        var offset = 0
        val limit  = 1000
        while (true) {
            val data = http.get("$baseUrl/network-monitoring/v1/bssids") {
                header("Authorization", "Bearer $accessToken")
                parameter("limit",  limit)
                parameter("offset", offset)
            }.body<ArubaBssidResponse>()
            allItems.addAll(data.items)
            Log.d(TAG, "Fetched ${data.items.size} BSSIDs (offset=$offset, total=${data.total})")
            if (data.next == null || allItems.size >= data.total) break
            offset += limit
        }
        ArubaResult.Success(allItems)
    }.getOrElse { e -> ArubaResult.Error(e.message ?: "Failed to fetch BSSIDs") }
}
