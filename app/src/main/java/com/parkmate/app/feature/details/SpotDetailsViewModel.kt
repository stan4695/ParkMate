package com.parkmate.app.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkmate.app.core.math.LocationMath
import com.parkmate.app.core.math.ParkingTimerCalculator
import com.parkmate.app.core.math.TimerProgressState
import com.parkmate.app.data.model.ParkingSpot
import com.parkmate.app.data.repository.ParkingRepository
import com.parkmate.app.system.location.LocationClientHelper
import com.parkmate.app.system.timer.ParkingTimerScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SpotDetailsUiState(
    val spot: ParkingSpot? = null,
    val draftTags: List<String> = emptyList(),
    val draftNote: String = "",
    val draftPhotos: List<String> = emptyList(),
    val distanceMeters: Int = 320,
    val walkMinutes: Int = 4,
    val timerState: TimerProgressState = ParkingTimerCalculator.calculateState(0L, null, null),
    val remindTenMinutesBefore: Boolean = true,
    val isDeleted: Boolean = false
)

@HiltViewModel
class SpotDetailsViewModel @Inject constructor(
    private val parkingRepository: ParkingRepository,
    private val locationClientHelper: LocationClientHelper,
    private val timerScheduler: ParkingTimerScheduler
) : ViewModel() {

    companion object {
        val AVAILABLE_QUICK_TAGS = listOf(
            "Level -1",
            "Level -2",
            "Zone A",
            "Zone B",
            "Row 14",
            "Pillar C4",
            "Near elevator",
            "Near exit"
        )
    }

    private val _uiState = MutableStateFlow(SpotDetailsUiState())
    val uiState: StateFlow<SpotDetailsUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var timerTickerJob: Job? = null
    private var initializedDraftForSpotId: Long = -1L

    fun loadSpot(spotId: Long) {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            val flow = if (spotId > 0L) {
                parkingRepository.observeSpotById(spotId)
            } else {
                parkingRepository.observeActiveSpot()
            }

            flow.collectLatest { spot ->
                if (spot == null) return@collectLatest
                val now = System.currentTimeMillis()
                val timer = ParkingTimerCalculator.calculateState(
                    nowMillis = now,
                    startEpochMillis = spot.timerStartMillis,
                    expiryEpochMillis = spot.timerExpiryMillis
                )
                val shouldResetDraft = initializedDraftForSpotId != spot.id
                if (shouldResetDraft) {
                    initializedDraftForSpotId = spot.id
                }

                _uiState.update { prev ->
                    prev.copy(
                        spot = spot,
                        draftTags = if (shouldResetDraft) spot.tags else prev.draftTags,
                        draftNote = if (shouldResetDraft) spot.note else prev.draftNote,
                        draftPhotos = spot.photoPaths,
                        timerState = timer,
                        remindTenMinutesBefore = spot.timerRemindBeforeExpiry
                    )
                }
                refreshDistance(spot)
                ensureTimerTickerRunning()
            }
        }
    }

    private fun refreshDistance(spot: ParkingSpot) {
        viewModelScope.launch {
            val userCoord = locationClientHelper.getCurrentHighAccuracyLocation()
            val dist = LocationMath.distanceMeters(
                userCoord.latitude,
                userCoord.longitude,
                spot.latitude,
                spot.longitude
            ).roundToInt()
            val walk = LocationMath.walkingTimeMinutes(dist.toDouble())
            _uiState.update { it.copy(distanceMeters = dist, walkMinutes = walk) }
        }
    }

    private fun ensureTimerTickerRunning() {
        if (timerTickerJob?.isActive == true) return
        timerTickerJob = viewModelScope.launch {
            while (true) {
                delay(1_000L)
                val spot = _uiState.value.spot ?: continue
                val updatedTimer = ParkingTimerCalculator.calculateState(
                    nowMillis = System.currentTimeMillis(),
                    startEpochMillis = spot.timerStartMillis,
                    expiryEpochMillis = spot.timerExpiryMillis
                )
                _uiState.update { it.copy(timerState = updatedTimer) }
            }
        }
    }

    fun toggleQuickTag(tag: String) {
        _uiState.update { current ->
            val updated = if (current.draftTags.contains(tag)) {
                current.draftTags - tag
            } else {
                current.draftTags + tag
            }
            current.copy(draftTags = updated)
        }
    }

    fun updateDraftNote(note: String) {
        _uiState.update { it.copy(draftNote = note.take(200)) }
    }

    fun saveInlineNoteNow(note: String) {
        val trimmed = note.take(200)
        _uiState.update { it.copy(draftNote = trimmed) }
        val currentSpot = _uiState.value.spot ?: return
        viewModelScope.launch {
            parkingRepository.updateSpot(currentSpot.copy(note = trimmed))
        }
    }

    fun removePhoto(path: String) {
        val currentSpot = _uiState.value.spot ?: return
        val updatedPhotos = currentSpot.photoPaths.filterNot { it == path }
        _uiState.update { it.copy(draftPhotos = updatedPhotos) }
        viewModelScope.launch {
            parkingRepository.updateSpot(currentSpot.copy(photoPaths = updatedPhotos))
            runCatching { File(path).delete() }
        }
    }

    fun saveDraftDetails(onSaved: () -> Unit) {
        val currentSpot = _uiState.value.spot ?: run {
            onSaved()
            return
        }
        viewModelScope.launch {
            val updated = currentSpot.copy(
                tags = _uiState.value.draftTags,
                note = _uiState.value.draftNote.trim().take(200),
                photoPaths = _uiState.value.draftPhotos.take(4)
            )
            parkingRepository.updateSpot(updated)
            onSaved()
        }
    }

    fun addTimerMinutes(minutesToAdd: Int) {
        val currentSpot = _uiState.value.spot ?: return
        val now = System.currentTimeMillis()
        val (_, newExpiry) = ParkingTimerCalculator.computeNewExpiry(
            nowMillis = now,
            currentExpiryMillis = currentSpot.timerExpiryMillis,
            additionalMinutes = minutesToAdd
        )
        val effectiveStart = currentSpot.timerStartMillis?.takeIf {
            currentSpot.timerExpiryMillis != null && currentSpot.timerExpiryMillis > now
        } ?: now

        val updatedSpot = currentSpot.copy(
            timerStartMillis = effectiveStart,
            timerExpiryMillis = newExpiry
        )
        viewModelScope.launch {
            parkingRepository.updateSpot(updatedSpot)
            timerScheduler.scheduleTimerAlarms(
                expiryEpochMillis = newExpiry,
                addressLine = updatedSpot.addressLine,
                remindTenMinutesBefore = updatedSpot.timerRemindBeforeExpiry
            )
        }
    }

    fun resetTimer() {
        val currentSpot = _uiState.value.spot ?: return
        val updatedSpot = currentSpot.copy(
            timerStartMillis = null,
            timerExpiryMillis = null
        )
        viewModelScope.launch {
            parkingRepository.updateSpot(updatedSpot)
            timerScheduler.cancelTimerAlarms()
        }
    }

    fun toggleRemindBeforeExpiry(enabled: Boolean) {
        val currentSpot = _uiState.value.spot ?: return
        val updatedSpot = currentSpot.copy(timerRemindBeforeExpiry = enabled)
        _uiState.update { it.copy(remindTenMinutesBefore = enabled) }
        viewModelScope.launch {
            parkingRepository.updateSpot(updatedSpot)
            val expiry = updatedSpot.timerExpiryMillis
            if (expiry != null && expiry > System.currentTimeMillis()) {
                timerScheduler.scheduleTimerAlarms(
                    expiryEpochMillis = expiry,
                    addressLine = updatedSpot.addressLine,
                    remindTenMinutesBefore = enabled
                )
            }
        }
    }

    fun deleteCurrentSpot(onDeleted: () -> Unit) {
        val currentSpot = _uiState.value.spot ?: return
        viewModelScope.launch {
            timerScheduler.cancelTimerAlarms()
            parkingRepository.deleteSpot(currentSpot.id)
            _uiState.update { it.copy(isDeleted = true) }
            onDeleted()
        }
    }
}
