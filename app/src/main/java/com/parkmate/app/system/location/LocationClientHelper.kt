package com.parkmate.app.system.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
    val isFallback: Boolean = false
)

@Singleton
class LocationClientHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    companion object {
        // Default Bucharest center matching the Figma prototype ("Strada Exemplu 12, Bucharest")
        val DEFAULT_COORDINATE = GeoCoordinate(
            latitude = 44.4412,
            longitude = 26.0979,
            isFallback = true
        )
    }

    fun hasForegroundLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasBackgroundLocationPermission(): Boolean {
        if (!hasForegroundLocationPermission()) return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    suspend fun getCurrentHighAccuracyLocation(): GeoCoordinate {
        if (!hasForegroundLocationPermission()) {
            return DEFAULT_COORDINATE
        }

        val currentFix: Location? = withTimeoutOrNull(4500L) {
            runCatching {
                val tokenSource = CancellationTokenSource()
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    tokenSource.token
                ).await()
            }.getOrNull()
        }

        if (currentFix != null) {
            return GeoCoordinate(currentFix.latitude, currentFix.longitude, isFallback = false)
        }

        val lastFix: Location? = runCatching {
            fusedClient.lastLocation.await()
        }.getOrNull()

        return if (lastFix != null) {
            GeoCoordinate(lastFix.latitude, lastFix.longitude, isFallback = false)
        } else {
            DEFAULT_COORDINATE
        }
    }

    @SuppressLint("MissingPermission")
    fun observeLocationUpdates(intervalMillis: Long = 4000L): Flow<GeoCoordinate> = callbackFlow {
        if (!hasForegroundLocationPermission()) {
            trySend(DEFAULT_COORDINATE)
            close()
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(2000L)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                val loc = result.lastLocation ?: return
                trySend(GeoCoordinate(loc.latitude, loc.longitude, isFallback = false))
            }
        }

        runCatching {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    trySend(GeoCoordinate(loc.latitude, loc.longitude, isFallback = false))
                }
            }
            fusedClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }.onFailure {
            trySend(DEFAULT_COORDINATE)
        }

        awaitClose {
            runCatching { fusedClient.removeLocationUpdates(callback) }
        }
    }
}
