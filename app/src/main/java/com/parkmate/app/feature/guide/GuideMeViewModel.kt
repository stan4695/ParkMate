package com.parkmate.app.feature.guide

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkmate.app.core.math.LocationMath
import com.parkmate.app.data.model.ParkingSpot
import com.parkmate.app.data.repository.ParkingRepository
import com.parkmate.app.system.location.GeoCoordinate
import com.parkmate.app.system.location.LocationClientHelper
import com.parkmate.app.system.sensor.CompassSensorMonitor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class GuideMeUiState(
    val spot: ParkingSpot? = null,
    val distanceMeters: Int = 320,
    val walkMinutes: Int = 4,
    val targetBearingDegrees: Float = 42f,
    val deviceAzimuthDegrees: Float = 0f,
    val relativeArrowRotationDegrees: Float = 42f,
    val cardinalDirection: String = "NE",
    val isCompassSensorAvailable: Boolean = true
)

@HiltViewModel
class GuideMeViewModel @Inject constructor(
    private val parkingRepository: ParkingRepository,
    private val locationClientHelper: LocationClientHelper,
    private val compassSensorMonitor: CompassSensorMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(GuideMeUiState())
    val uiState: StateFlow<GuideMeUiState> = _uiState.asStateFlow()

    private var guideJob: Job? = null

    fun startGuidance(spotId: Long) {
        guideJob?.cancel()
        guideJob = viewModelScope.launch {
            val spotFlow = if (spotId > 0L) {
                parkingRepository.observeSpotById(spotId)
            } else {
                parkingRepository.observeActiveSpot()
            }

            combine(
                spotFlow,
                locationClientHelper.observeLocationUpdates(3000L),
                compassSensorMonitor.observeAzimuth()
            ) { spot, userLocation, compass ->
                buildGuideState(spot, userLocation, compass.azimuthDegrees, compass.isSensorAvailable)
            }.collectLatest { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun buildGuideState(
        spot: ParkingSpot?,
        userCoord: GeoCoordinate,
        azimuthDegrees: Float,
        sensorAvailable: Boolean
    ): GuideMeUiState {
        if (spot == null) {
            return GuideMeUiState(isCompassSensorAvailable = sensorAvailable)
        }

        val dist = LocationMath.distanceMeters(
            userCoord.latitude,
            userCoord.longitude,
            spot.latitude,
            spot.longitude
        ).roundToInt()

        val bearing = LocationMath.initialBearingDegrees(
            userCoord.latitude,
            userCoord.longitude,
            spot.latitude,
            spot.longitude
        )
        val relativeAngle = LocationMath.relativeRotationDegrees(
            targetBearing = bearing,
            deviceAzimuth = if (sensorAvailable) azimuthDegrees else 0f
        )

        return GuideMeUiState(
            spot = spot,
            distanceMeters = dist,
            walkMinutes = LocationMath.walkingTimeMinutes(dist.toDouble()),
            targetBearingDegrees = bearing,
            deviceAzimuthDegrees = azimuthDegrees,
            relativeArrowRotationDegrees = relativeAngle,
            cardinalDirection = LocationMath.cardinalDirection(bearing),
            isCompassSensorAvailable = sensorAvailable
        )
    }
}
