package com.kshimono.wifianalyzer.data.mist

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class MistRadioStat(
    val channel: Int? = null,
    val bandwidth: Int? = null,
    val power: Int? = null,
    val mac: String? = null,
    val usage: String? = null,
)

@Serializable
data class MistApStats(
    val id: String? = null,
    val name: String? = null,
    val mac: String? = null,
    val model: String? = null,
    val x: Double? = null,
    val y: Double? = null,
    @SerialName("map_id") val mapId: String? = null,
    val status: String? = null,
    @SerialName("radio_stat") val radioStat: Map<String, MistRadioStat>? = null,
)

@Serializable
data class MistMap(
    val id: String,
    val name: String,
    val type: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    @SerialName("width_m")  val widthM:  Double? = null,
    @SerialName("height_m") val heightM: Double? = null,
    @SerialName("site_id")  val siteId:  String? = null,
    val url: String? = null,
)

private const val TAG = "MistApi"

@Singleton
class MistApiClient @Inject constructor() {

    private var token: String = ""
    private var region: String = ""

    private val http = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                coerceInputValues = true
            })
        }
    }

    fun configure(token: String, region: String) {
        this.token = token
        this.region = region
    }

    private fun baseUrl() = "https://$region/api/v1"
    private fun authHeader() = "Token $token"

    private fun httpError(status: Int): String = when (status) {
        401  -> "Invalid API Token"
        403  -> "Insufficient permissions"
        404  -> "Resource not found"
        429  -> "Rate limit exceeded. Please wait."
        else -> if (status >= 500) "Mist server error (HTTP $status)" else "HTTP error $status"
    }

    private fun networkError(e: Throwable): String = when (e) {
        is SocketTimeoutException -> "Connection timeout. Check network."
        else -> "Network error: ${e.message}"
    }

    private suspend inline fun <reified T> safeGet(url: String): MistResult<T> =
        runCatching {
            val resp: HttpResponse = http.get(url) { header("Authorization", authHeader()) }
            Log.d(TAG, "GET $url → ${resp.status.value}")
            if (!resp.status.isSuccess()) return MistResult.Error(httpError(resp.status.value))
            MistResult.Success(resp.body<T>())
        }.getOrElse { e -> MistResult.Error(networkError(e)) }

    /**
     * limit=1000 + page でリスト系エンドポイントを全件取得する。
     * 1ページの件数が limit 未満になったら最終ページとみなして打ち切る。
     */
    private suspend inline fun <reified T> safeGetAllPages(
        url: String,
        limit: Int = 1000,
        maxPages: Int = 100,
    ): MistResult<List<T>> =
        runCatching {
            val all = mutableListOf<T>()
            var page = 1
            while (page <= maxPages) {
                val resp: HttpResponse = http.get(url) {
                    header("Authorization", authHeader())
                    parameter("limit", limit)
                    parameter("page", page)
                }
                Log.d(TAG, "GET $url (page=$page) → ${resp.status.value}")
                if (!resp.status.isSuccess()) return MistResult.Error(httpError(resp.status.value))
                val pageData = resp.body<List<T>>()
                all.addAll(pageData)
                if (pageData.size < limit) break
                page++
            }
            Log.d(TAG, "GET $url fetched ${all.size} items over $page page(s)")
            MistResult.Success<List<T>>(all)
        }.getOrElse { e -> MistResult.Error(networkError(e)) }

    suspend fun getSelf(): MistResult<List<MistOrg>> =
        when (val r = safeGet<MistSelfResponse>("${baseUrl()}/self")) {
            is MistResult.Error   -> MistResult.Error(r.message)
            is MistResult.Success -> {
                val orgs = r.data.privileges
                    .filter { it.scope == "org" && it.orgId != null }
                    .map { MistOrg(id = it.orgId!!, name = it.name ?: it.orgId) }
                MistResult.Success(orgs)
            }
        }

    suspend fun getSites(orgId: String): MistResult<List<MistSite>> =
        safeGet("${baseUrl()}/orgs/$orgId/sites")

    suspend fun getAps(orgId: String, siteId: String?): MistResult<List<MistApDevice>> =
        when (val r = safeGetAllPages<MistApDevice>("${baseUrl()}/orgs/$orgId/inventory?type=ap")) {
            is MistResult.Error   -> MistResult.Error(r.message)
            is MistResult.Success -> {
                val filtered = if (siteId != null && siteId != "all")
                    r.data.filter { it.siteId == siteId } else r.data
                MistResult.Success(filtered)
            }
        }

    suspend fun getRadioMacs(orgId: String): MistResult<List<MistRadioMac>> =
        safeGetAllPages("${baseUrl()}/orgs/$orgId/devices/radio_macs")

    suspend fun getMaps(siteId: String): MistResult<List<MistMap>> =
        safeGet("${baseUrl()}/sites/$siteId/maps")

    suspend fun getApStats(siteId: String): MistResult<List<MistApStats>> =
        safeGet("${baseUrl()}/sites/$siteId/stats/devices?type=ap")
}
