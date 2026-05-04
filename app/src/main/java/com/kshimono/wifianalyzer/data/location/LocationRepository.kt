package com.kshimono.wifianalyzer.data.location

interface LocationRepository {
    suspend fun getCurrentLocation(): LocationResult
}

data class LocationResult(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val isGps: Boolean,
)

sealed class LocationError {
    object PermissionDenied : LocationError()
    object LocationDisabled : LocationError()
    object Timeout : LocationError()
    data class Unknown(val message: String) : LocationError()
}
