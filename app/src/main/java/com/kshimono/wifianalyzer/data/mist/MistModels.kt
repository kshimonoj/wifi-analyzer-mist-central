package com.kshimono.wifianalyzer.data.mist

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MistOrg(val id: String, val name: String)

@Serializable
data class MistSite(val id: String, val name: String)

@Serializable
data class MistApDevice(
    val id: String = "",
    val name: String = "",
    val mac: String,
    val model: String? = null,
    val serial: String? = null,
    @SerialName("site_id") val siteId: String? = null,
    @SerialName("org_id") val orgId: String? = null,
)

@Serializable
data class MistSelfResponse(
    val name: String = "",
    val privileges: List<MistPrivilege> = emptyList(),
)

@Serializable
data class MistPrivilege(
    val scope: String = "",
    val role: String? = null,
    val name: String? = null,
    @SerialName("org_id") val orgId: String? = null,
)

@Serializable
data class MistRadioMac(
    val mac: String,
    @SerialName("radio_mac") val radioMac: List<String> = emptyList(),
)

sealed class MistResult<out T> {
    data class Success<T>(val data: T) : MistResult<T>()
    data class Error<T>(val message: String) : MistResult<T>()
}
