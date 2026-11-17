package com.parkmate.app.feature.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkmate.app.data.model.ParkingSpot
import com.parkmate.app.data.repository.ParkingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val spots: List<ParkingSpot> = emptyList(),
    val previewEmptyState: Boolean = false
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val parkingRepository: ParkingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            parkingRepository.observeAllSpots().collectLatest { list ->
                _uiState.update { it.copy(spots = list) }
            }
        }
    }

    fun togglePreviewEmptyState() {
        _uiState.update { it.copy(previewEmptyState = !it.previewEmptyState) }
    }

    fun loadExampleHistory() {
        _uiState.update { it.copy(previewEmptyState = false) }
        viewModelScope.launch {
            if (_uiState.value.spots.isEmpty()) {
                parkingRepository.seedSampleSpots()
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            parkingRepository.clearAllSpots()
        }
    }
}
