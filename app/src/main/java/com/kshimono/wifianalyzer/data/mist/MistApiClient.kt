package com.kshimono.wifianalyzer.data.mist

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

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

    suspend fun getSelf(): MistResult<List<MistOrg>> = runCatching {
        val resp = http.get("${baseUrl()}/self") {
            header("Authorization", authHeader())
        }.body<MistSelfResponse>()
        val orgs = resp.privileges
            .filter { it.scope == "org" && it.orgId != null }
            .map { MistOrg(id = it.orgId!!, name = it.name ?: it.orgId) }
        MistResult.Success(orgs)
    }.getOrElse { e -> MistResult.Error(e.message ?: "Unknown error") }

    suspend fun getSites(orgId: String): MistResult<List<MistSite>> = runCatching {
        val resp = http.get("${baseUrl()}/orgs/$orgId/sites") {
            header("Authorization", authHeader())
        }.body<List<MistSite>>()
        MistResult.Success(resp)
    }.getOrElse { e -> MistResult.Error(e.message ?: "Unknown error") }

    suspend fun getAps(orgId: String, siteId: String?): MistResult<List<MistApDevice>> = runCatching {
        val all = http.get("${baseUrl()}/orgs/$orgId/inventory?type=ap") {
            header("Authorization", authHeader())
        }.body<List<MistApDevice>>()
        val filtered = if (siteId != null && siteId != "all")
            all.filter { it.siteId == siteId }
        else
            all
        MistResult.Success(filtered)
    }.getOrElse { e -> MistResult.Error(e.message ?: "Unknown error") }

    suspend fun getRadioMacs(orgId: String): MistResult<List<MistRadioMac>> = runCatching {
        val resp = http.get("${baseUrl()}/orgs/$orgId/devices/radio_macs") {
            header("Authorization", authHeader())
        }.body<List<MistRadioMac>>()
        MistResult.Success(resp)
    }.getOrElse { e -> MistResult.Error(e.message ?: "Unknown error") }
}
