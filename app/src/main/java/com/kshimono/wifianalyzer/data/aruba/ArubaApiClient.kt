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
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import com.kshimono.wifianalyzer.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
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

@Serializable
data class ArubaFloorProperties(
    val name: String? = null,
    val ordinal: Int? = null,
    val siteId: String? = null,
    val ceilingHeight: Int? = null,
)

@Serializable
data class ArubaFloor(
    val floorId: String,
    val properties: ArubaFloorProperties = ArubaFloorProperties(),
)

@Serializable
data class ArubaBuildingProperties(
    val name: String? = null,
    val siteId: String? = null,
)

@Serializable
data class ArubaBuilding(
    val buildingId: String? = null,
    val properties: ArubaBuildingProperties = ArubaBuildingProperties(),
    val floors: List<ArubaFloor> = emptyList(),
)

@Serializable
data class ArubaBuildingsResponse(
    val count: Int = 0,
    val items: List<ArubaBuilding> = emptyList(),
)

@Serializable
data class ArubaRadio(
    val macAddress: String? = null,
    val channel: String? = null,
    val band: String? = null,
    val power: String? = null,
    val number: Int? = null,
)

@Serializable
data class ArubaApProperties(
    val macAddress: String? = null,
    val model: String? = null,
    val status: String? = null,
    val radios: List<ArubaRadio>? = null,
)

@Serializable
data class ArubaDeployedDevice(
    val deviceName: String? = null,
    val deviceType: String? = null,
    val geometryRelative: List<Double>? = null,
    val accesspointProperties: ArubaApProperties? = null,
)

@Serializable
data class ArubaDeployedDevicesResponse(
    val items: List<ArubaDeployedDevice> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class ArubaFloorDetailProperties(
    val length: Double? = null,
    val breadth: Double? = null,
    val name: String? = null,
)

@Serializable
data class ArubaFloorDetail(
    val properties: ArubaFloorDetailProperties? = null,
)

sealed class ArubaResult<out T> {
    data class Success<T>(val data: T) : ArubaResult<T>()
    data class Error<T>(val message: String) : ArubaResult<T>()
}

@Singleton
class ArubaApiClient @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

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


    private fun tokenHttpError(status: Int, body: String): String {
        val errorCode = runCatching { JSONObject(body).optString("error") }.getOrDefault("")
        return when {
            errorCode == "unauthorized_request" -> "Invalid Client ID or Secret"
            status == 401 -> "Authentication failed (401)"
            status == 403 -> "Access forbidden (403)"
            status == 429 -> "Rate limit exceeded. Please wait."
            status >= 500 -> "HPE SSO server error (HTTP $status)"
            else -> "Token request failed (HTTP $status)"
        }
    }

    private fun apiHttpError(status: Int): String = when (status) {
        401  -> "Authentication failed. Re-check credentials."
        403  -> "Insufficient permissions"
        404  -> "Resource not found"
        429  -> "Rate limit exceeded. Please wait."
        else -> if (status >= 500) "Aruba server error (HTTP $status)" else "HTTP error $status"
    }

    private fun networkError(e: Throwable): String = when (e) {
        is SocketTimeoutException -> "Connection timeout. Check network."
        else -> "Network error: ${e.message}"
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
        // In-memory fast path
        if (accessToken.isNotEmpty() && System.currentTimeMillis() < tokenExpiresAt - 60_000L) return

        // DataStore fallback — valid across app restarts
        val savedToken  = settingsRepository.arubaAccessToken.first()
        val savedExpiry = settingsRepository.arubaTokenExpiresAt.first()
        if (savedToken.isNotEmpty() && System.currentTimeMillis() < savedExpiry - 60_000L) {
            accessToken    = savedToken
            tokenExpiresAt = savedExpiry
            Log.d(TAG, "Using persisted token")
            return
        }

        // Network fetch
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
                if (status != 200) throw Exception(tokenHttpError(status, text))
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

        // Persist for next app session
        settingsRepository.setArubaAccessToken(accessToken)
        settingsRepository.setArubaTokenExpiresAt(tokenExpiresAt)
    }

    suspend fun forceRefreshToken() {
        accessToken    = ""
        tokenExpiresAt = 0L
        settingsRepository.setArubaTokenExpiresAt(0L)
        ensureValidToken()
    }

    suspend fun getSites(): ArubaResult<List<ArubaSite>> = runCatching {
        ensureValidToken()
        val resp: HttpResponse = http.get("$baseUrl/network-monitoring/v1/sites-health") {
            header("Authorization", "Bearer $accessToken")
            parameter("limit", 1000)
        }
        if (!resp.status.isSuccess()) return ArubaResult.Error(apiHttpError(resp.status.value))
        val body = resp.body<ArubaSiteResponse>()
        Log.d(TAG, "Fetched ${body.items.size} sites")
        ArubaResult.Success(body.items)
    }.getOrElse { e -> ArubaResult.Error(networkError(e)) }

    suspend fun getBssidsBySite(siteId: String): ArubaResult<List<ArubaBssid>> = runCatching {
        ensureValidToken()
        val allItems = mutableListOf<ArubaBssid>()
        var offset = 0
        val limit  = 1000
        while (true) {
            val resp: HttpResponse = http.get("$baseUrl/network-monitoring/v1/bssids") {
                header("Authorization", "Bearer $accessToken")
                parameter("filter", "siteId eq $siteId")
                parameter("limit",  limit)
                parameter("offset", offset)
            }
            if (!resp.status.isSuccess()) return ArubaResult.Error(apiHttpError(resp.status.value))
            val data = resp.body<ArubaBssidResponse>()
            allItems.addAll(data.items)
            Log.d(TAG, "Fetched ${data.items.size} BSSIDs for siteId=$siteId (offset=$offset, total=${data.total})")
            if (data.next == null || allItems.size >= data.total) break
            offset += limit
        }
        ArubaResult.Success(allItems)
    }.getOrElse { e -> ArubaResult.Error(networkError(e)) }

    suspend fun getBuildings(siteId: String): ArubaResult<List<ArubaBuilding>> = runCatching {
        ensureValidToken()
        val resp: HttpResponse = http.get("$baseUrl/network-monitoring/v1/sitemaps/$siteId/buildings") {
            header("Authorization", "Bearer $accessToken")
        }
        if (!resp.status.isSuccess()) return ArubaResult.Error(apiHttpError(resp.status.value))
        val body = resp.body<ArubaBuildingsResponse>()
        Log.d(TAG, "Fetched ${body.items.size} buildings for siteId=$siteId")
        ArubaResult.Success(body.items)
    }.getOrElse { e -> ArubaResult.Error(networkError(e)) }

    suspend fun getDeployedDevices(siteId: String, floorId: String): ArubaResult<List<ArubaDeployedDevice>> = runCatching {
        ensureValidToken()
        val resp: HttpResponse = http.get("$baseUrl/network-monitoring/v1/sitemaps/$siteId/network-devices-deployed") {
            header("Authorization", "Bearer $accessToken")
            parameter("filter", "floorId eq '$floorId'")
        }
        if (!resp.status.isSuccess()) return ArubaResult.Error(apiHttpError(resp.status.value))
        val body = resp.body<ArubaDeployedDevicesResponse>()
        Log.d(TAG, "Fetched ${body.items.size} devices for siteId=$siteId floorId=$floorId")
        ArubaResult.Success(body.items)
    }.getOrElse { e -> ArubaResult.Error(networkError(e)) }

    suspend fun getFloorDetail(siteId: String, floorId: String): ArubaResult<ArubaFloorDetail> = runCatching {
        ensureValidToken()
        val resp: HttpResponse = http.get("$baseUrl/network-monitoring/v1/sitemaps/$siteId/floors/$floorId") {
            header("Authorization", "Bearer $accessToken")
        }
        if (!resp.status.isSuccess()) return ArubaResult.Error(apiHttpError(resp.status.value))
        ArubaResult.Success(resp.body<ArubaFloorDetail>())
    }.getOrElse { e -> ArubaResult.Error(networkError(e)) }

    suspend fun downloadFloorImage(siteId: String, floorId: String): ArubaResult<ByteArray> = runCatching {
        ensureValidToken()
        val token = accessToken
        val bytes = withContext(Dispatchers.IO) {
            val url = "$baseUrl/network-monitoring/v1/sitemaps/$siteId/floors/$floorId/image?raster=true"
            val conn = (java.net.URL(url).openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $token")
                connectTimeout = 15_000
                readTimeout    = 60_000
            }
            try {
                val status = conn.responseCode
                if (status != 200) throw Exception(apiHttpError(status))
                conn.inputStream.use { it.readBytes() }
            } finally {
                conn.disconnect()
            }
        }
        ArubaResult.Success(bytes)
    }.getOrElse { e -> ArubaResult.Error(networkError(e)) }

    suspend fun testConnection(clientId: String, clientSecret: String, cluster: String): ArubaResult<Int> =
        runCatching {
            configure(clientId, clientSecret, cluster)
            ensureValidToken()
            when (val result = getAllBssids()) {
                is ArubaResult.Success -> ArubaResult.Success(result.data.size)
                is ArubaResult.Error   -> ArubaResult.Error(result.message)
            }
        }.getOrElse { e -> ArubaResult.Error(networkError(e)) }

    suspend fun getAllBssids(): ArubaResult<List<ArubaBssid>> = runCatching {
        ensureValidToken()
        val allItems = mutableListOf<ArubaBssid>()
        var offset = 0
        val limit  = 1000
        while (true) {
            val resp: HttpResponse = http.get("$baseUrl/network-monitoring/v1/bssids") {
                header("Authorization", "Bearer $accessToken")
                parameter("limit",  limit)
                parameter("offset", offset)
            }
            if (!resp.status.isSuccess()) return ArubaResult.Error(apiHttpError(resp.status.value))
            val data = resp.body<ArubaBssidResponse>()
            allItems.addAll(data.items)
            Log.d(TAG, "Fetched ${data.items.size} BSSIDs (offset=$offset, total=${data.total})")
            if (data.next == null || allItems.size >= data.total) break
            offset += limit
        }
        ArubaResult.Success(allItems)
    }.getOrElse { e -> ArubaResult.Error(networkError(e)) }
}
