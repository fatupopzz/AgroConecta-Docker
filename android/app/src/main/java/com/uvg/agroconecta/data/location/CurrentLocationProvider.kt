package com.uvg.agroconecta.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double
)

interface CurrentLocationProvider {
    suspend fun getCurrentCoordinates(): GeoCoordinates?
}

@Singleton
class AndroidCurrentLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : CurrentLocationProvider {

    private val locationManager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentCoordinates(): GeoCoordinates? {
        val hasFinePermission = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarsePermission = context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!hasFinePermission && !hasCoarsePermission) {
            throw SecurityException("Permiso de ubicación no concedido")
        }

        val candidateProviders = buildList {
            if (hasFinePermission) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { provider -> locationManager.isProviderAvailable(provider) }

        val provider = candidateProviders.firstOrNull()
            ?: return bestLastKnownLocation(candidateProviders)?.toCoordinates()

        val currentLocation = withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
            suspendCancellableCoroutine<Location?> { continuation ->
                val cancellationSignal = CancellationSignal()
                LocationManagerCompat.getCurrentLocation(
                    locationManager,
                    provider,
                    cancellationSignal,
                    ContextCompat.getMainExecutor(context)
                ) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
                continuation.invokeOnCancellation { cancellationSignal.cancel() }
            }
        }

        return (currentLocation ?: bestLastKnownLocation(candidateProviders))?.toCoordinates()
    }

    @SuppressLint("MissingPermission")
    private fun bestLastKnownLocation(providers: List<String>): Location? =
        providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull(Location::getTime)

    private fun LocationManager.isProviderAvailable(provider: String): Boolean =
        runCatching { isProviderEnabled(provider) }.getOrDefault(false)

    private companion object {
        const val LOCATION_TIMEOUT_MILLIS = 10_000L
    }
}

private fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Location.toCoordinates() = GeoCoordinates(
    latitude = latitude,
    longitude = longitude
)
