package com.parkmate.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkmate.app.core.math.LocationMath
import com.parkmate.app.data.model.ParkingSpot
import com.parkmate.app.data.repository.ParkingRepository
import com.parkmate.app.system.location.GeoCoordinate
import com.parkmate.app.system.location.LocationClientHelper
import com.parkmate.app.system.location.LocationReverseGeocoder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SelectedMapPoint(
    val latitude: Double,
    val longitude: Double,
    val addressLine: String,
    val cityDistrict: String = "",
    val distanceMeters: Int = 0,
    val walkMinutes: Int = 1,
    val isResolvingAddress: Boolean = true
)

data class HomeUiState(
    val activeSpot: ParkingSpot? = null,
    val selectedMapPoint: SelectedMapPoint? = null,
    val userLocation: GeoCoordinate = LocationClientHelper.DEFAULT_COORDINATE,
    val distanceMeters: Int = 0,
    val walkMinutes: Int = 1,
    val parkedElapsedMinutes: Long = 0L,
    val isSavingSpot: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null
)

sealed interface HomeEvent {
    data class SpotSavedNavigateQuickSave(val spotId: Long) : HomeEvent
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val parkingRepository: ParkingRepository,
    private val locationClientHelper: LocationClientHelper,
    private val reverseGeocoder: LocationReverseGeocoder
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    private var locationUpdatesJob: Job? = null

    init {
        observeActiveSpot()
        refreshPermissionAndLocation()
        startElapsedMinuteTicker()
    }

    private fun observeActiveSpot() {
        viewModelScope.launch {
            parkingRepository.observeActiveSpot().collectLatest { spot ->
                _uiState.update { current ->
                    recalculateMetrics(current.copy(activeSpot = spot))
                }
            }
        }
    }

    private fun startElapsedMinuteTicker() {
        viewModelScope.launch {
            while (true) {
                delay(30_000L)
                _uiState.update { current -> recalculateMetrics(current) }
            }
        }
    }

    fun refreshPermissionAndLocation() {
        val granted = locationClientHelper.hasForegroundLocationPermission()
        _uiState.update { it.copy(hasLocationPermission = granted) }
        if (granted) {
            viewModelScope.launch {
                val coord = locationClientHelper.getCurrentHighAccuracyLocation()
                _uiState.update { current ->
                    recalculateMetrics(current.copy(userLocation = coord))
                }
            }
            startContinuousLocationUpdates()
        }
    }

    private fun startContinuousLocationUpdates() {
        if (locationUpdatesJob?.isActive == true) return
        locationUpdatesJob = viewModelScope.launch {
            locationClientHelper.observeLocationUpdates(intervalMillis = 4000L).collectLatest { coord ->
                _uiState.update { current ->
                    recalculateMetrics(current.copy(userLocation = coord))
                }
            }
        }
    }

    fun saveCurrentSpotManual() {
        if (_uiState.value.isSavingSpot) return
        _uiState.update { it.copy(isSavingSpot = true, errorMessage = null) }

        viewModelScope.launch {
            val coord = locationClientHelper.getCurrentHighAccuracyLocation()
            val address = reverseGeocoder.resolveAddress(coord.latitude, coord.longitude)
            val now = System.currentTimeMillis()
            val newSpot = ParkingSpot(
                latitude = coord.latitude,
                longitude = coord.longitude,
                addressLine = address.addressLine,
                cityDistrict = address.cityDistrict,
                savedAtMillis = now,
                isActive = true
            )
            val spotId = parkingRepository.saveNewSpot(newSpot)
            _uiState.update {
                recalculateMetrics(
                    it.copy(
                        userLocation = coord,
                        selectedMapPoint = null,
                        isSavingSpot = false
                    )
                )
            }
            _events.emit(HomeEvent.SpotSavedNavigateQuickSave(spotId))
        }
    }

    /**
     * Triggered when the user long-presses any point on the map to select a custom parking location.
     */
    fun onMapLongPress(latitude: Double, longitude: Double) {
        val currentUser = _uiState.value.userLocation
        val dist = LocationMath.distanceMeters(
            startLat = currentUser.latitude,
            startLng = currentUser.longitude,
            endLat = latitude,
            endLng = longitude
        ).roundToInt()
        val walk = LocationMath.walkingTimeMinutes(dist.toDouble())
        val fallbackCoords = String.format(Locale.US, "Lat %.5f, Lng %.5f", latitude, longitude)

        _uiState.update {
            it.copy(
                selectedMapPoint = SelectedMapPoint(
                    latitude = latitude,
                    longitude = longitude,
                    addressLine = fallbackCoords,
                    cityDistrict = "",
                    distanceMeters = dist,
                    walkMinutes = walk,
                    isResolvingAddress = true
                )
            )
        }

        viewModelScope.launch {
            val address = reverseGeocoder.resolveAddress(latitude, longitude)
            _uiState.update { state ->
                val currentSel = state.selectedMapPoint ?: return@update state
                if (currentSel.latitude == latitude && currentSel.longitude == longitude) {
                    state.copy(
                        selectedMapPoint = currentSel.copy(
                            addressLine = address.addressLine,
                            cityDistrict = address.cityDistrict,
                            isResolvingAddress = false
                        )
                    )
                } else {
                    state
                }
            }
        }
    }

    fun dismissSelectedMapPoint() {
        _uiState.update { it.copy(selectedMapPoint = null) }
    }

    fun confirmSaveSelectedMapPoint() {
        val sel = _uiState.value.selectedMapPoint ?: return
        if (_uiState.value.isSavingSpot) return
        _uiState.update { it.copy(isSavingSpot = true, errorMessage = null) }

        viewModelScope.launch {
            val resolved = if (sel.isResolvingAddress) {
                reverseGeocoder.resolveAddress(sel.latitude, sel.longitude)
            } else {
                null
            }
            val now = System.currentTimeMillis()
            val newSpot = ParkingSpot(
                latitude = sel.latitude,
                longitude = sel.longitude,
                addressLine = resolved?.addressLine ?: sel.addressLine,
                cityDistrict = resolved?.cityDistrict ?: sel.cityDistrict,
                savedAtMillis = now,
                isActive = true
            )
            val spotId = parkingRepository.saveNewSpot(newSpot)
            _uiState.update {
                recalculateMetrics(
                    it.copy(
                        selectedMapPoint = null,
                        isSavingSpot = false
                    )
                )
            }
            _events.emit(HomeEvent.SpotSavedNavigateQuickSave(spotId))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun recalculateMetrics(state: HomeUiState): HomeUiState {
        val spot = state.activeSpot ?: return state.copy(
            distanceMeters = 0,
            walkMinutes = 0,
            parkedElapsedMinutes = 0L
        )

        val dist = LocationMath.distanceMeters(
            startLat = state.userLocation.latitude,
            startLng = state.userLocation.longitude,
            endLat = spot.latitude,
            endLng = spot.longitude
        ).roundToInt()

        val walk = LocationMath.walkingTimeMinutes(dist.toDouble())
        val elapsedMinutes = ((System.currentTimeMillis() - spot.savedAtMillis) / 60_000L).coerceAtLeast(0L)

        return state.copy(
            distanceMeters = dist,
            walkMinutes = walk,
            parkedElapsedMinutes = elapsedMinutes
        )
    }
}
