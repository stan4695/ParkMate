package com.parkmate.app.system.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

data class ResolvedAddress(
    val addressLine: String,
    val cityDistrict: String
)

@Singleton
class LocationReverseGeocoder @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun resolveAddress(latitude: Double, longitude: Double): ResolvedAddress =
        withContext(Dispatchers.IO) {
            val fallbackLine = String.format(Locale.US, "%.5f, %.5f", latitude, longitude)
            val fallbackCity = "GPS Coordinates"

            if (!Geocoder.isPresent()) {
                return@withContext ResolvedAddress(fallbackLine, fallbackCity)
            }

            val geocoder = Geocoder(context, Locale.getDefault())
            val address: Address? = withTimeoutOrNull(3500L) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        suspendCancellableCoroutine<Address?> { cont ->
                            geocoder.getFromLocation(
                                latitude,
                                longitude,
                                1,
                                object : Geocoder.GeocodeListener {
                                    override fun onGeocode(addresses: MutableList<Address>) {
                                        if (cont.isActive) cont.resume(addresses.firstOrNull())
                                    }

                                    override fun onError(errorMessage: String?) {
                                        if (cont.isActive) cont.resume(null)
                                    }
                                }
                            )
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
                    }
                }.getOrNull()
            }

            if (address == null) {
                return@withContext ResolvedAddress(fallbackLine, fallbackCity)
            }

            val thoroughfare = address.thoroughfare
            val subThoroughfare = address.subThoroughfare
            val primaryLine = when {
                !thoroughfare.isNullOrBlank() && !subThoroughfare.isNullOrBlank() ->
                    "$thoroughfare $subThoroughfare"
                !thoroughfare.isNullOrBlank() -> thoroughfare
                !address.featureName.isNullOrBlank() -> address.featureName
                else -> fallbackLine
            }

            val locality = address.locality ?: address.subAdminArea ?: ""
            val subLocality = address.subLocality ?: address.adminArea ?: ""
            val secondaryLine = listOf(locality, subLocality)
                .filter { it.isNotBlank() }
                .distinct()
                .joinToString(", ")
                .ifBlank { fallbackCity }

            ResolvedAddress(primaryLine, secondaryLine)
        }
}
