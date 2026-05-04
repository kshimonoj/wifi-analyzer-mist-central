package com.kshimono.wifianalyzer.data.location

import android.Manifest
import android.content.Context
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationRepository {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    override suspend fun getCurrentLocation(): LocationResult {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PermissionChecker.PERMISSION_GRANTED
        ) {
            throw SecurityException("Location permission denied")
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                      lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        if (!enabled) throw Exception("Location services disabled")

        return try {
            withTimeout(10_000L) {
                suspendCancellableCoroutine { cont ->
                    val cts = CancellationTokenSource()
                    cont.invokeOnCancellation { cts.cancel() }

                    fusedLocationClient
                        .getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                        .addOnCompleteListener { task ->
                            val location: Location? = if (task.isSuccessful) task.result else null
                            if (location != null) {
                                cont.resume(location.toLocationResult())
                            } else {
                                fusedLocationClient.lastLocation
                                    .addOnSuccessListener { last ->
                                        if (last != null) {
                                            cont.resume(last.toLocationResult(isGps = false))
                                        } else {
                                            cont.resumeWithException(Exception("Location unavailable"))
                                        }
                                    }
                                    .addOnFailureListener { cont.resumeWithException(it) }
                            }
                        }
                }
            }
        } catch (e: TimeoutCancellationException) {
            throw Exception("Location timeout")
        }
    }

    private fun Location.toLocationResult(isGps: Boolean = provider == "gps"): LocationResult =
        LocationResult(latitude, longitude, accuracy, isGps)
}
