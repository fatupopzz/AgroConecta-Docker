package com.uvg.agroconecta.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double
)

sealed interface BusinessLocationState {
    data object Idle : BusinessLocationState
    data object Loading : BusinessLocationState
    data class Located(val coordinates: GeoCoordinates) : BusinessLocationState
    data class Error(val message: String) : BusinessLocationState
}

interface CurrentLocationProvider {
    suspend fun getCurrentCoordinates(): GeoCoordinates?

    fun observeCoordinates(): Flow<GeoCoordinates> = emptyFlow()

    companion object {
        const val LOCATION_UPDATE_INTERVAL_MILLIS = 7_000L
        const val MIN_LOCATION_UPDATE_DISTANCE_METERS = 15f
    }
}

@Singleton
class AndroidCurrentLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : CurrentLocationProvider {

    private val locationManager: LocationManager
        get() = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentCoordinates(): GeoCoordinates? {
        val candidateProviders = availableProviders()

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
    override fun observeCoordinates(): Flow<GeoCoordinates> = callbackFlow {
        val candidateProviders = availableProviders()
        if (candidateProviders.isEmpty()) {
            close(IllegalStateException("Activa el GPS para actualizar tu ubicación"))
            return@callbackFlow
        }

        val listener = LocationListener { location ->
            location.toCoordinates()
                .takeIf(GeoCoordinates::isValid)
                ?.let { coordinates -> trySend(coordinates) }
        }

        try {
            candidateProviders.forEach { provider ->
                locationManager.requestLocationUpdates(
                    provider,
                    CurrentLocationProvider.LOCATION_UPDATE_INTERVAL_MILLIS,
                    CurrentLocationProvider.MIN_LOCATION_UPDATE_DISTANCE_METERS,
                    listener,
                    Looper.getMainLooper()
                )
            }
        } catch (error: Exception) {
            locationManager.removeUpdates(listener)
            close(error)
            return@callbackFlow
        }

        bestLastKnownLocation(candidateProviders)
            ?.toCoordinates()
            ?.takeIf(GeoCoordinates::isValid)
            ?.let { coordinates -> trySend(coordinates) }

        awaitClose {
            locationManager.removeUpdates(listener)
        }
    }.distinctUntilChanged { previous, current ->
        previous.distanceTo(current) < CurrentLocationProvider.MIN_LOCATION_UPDATE_DISTANCE_METERS
    }

    private fun availableProviders(): List<String> {
        val hasFinePermission = context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasCoarsePermission = context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (!hasFinePermission && !hasCoarsePermission) {
            throw SecurityException("Permiso de ubicación no concedido")
        }

        return buildList {
            if (hasFinePermission) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
        }.filter { provider -> locationManager.isProviderAvailable(provider) }
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

private val GeoCoordinates.isValid: Boolean
    get() = latitude.isFinite() &&
        longitude.isFinite() &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0

private fun GeoCoordinates.distanceTo(other: GeoCoordinates): Float {
    val result = FloatArray(1)
    Location.distanceBetween(latitude, longitude, other.latitude, other.longitude, result)
    return result[0]
}
