package com.parkmate.app.feature.camera

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.parkmate.app.data.repository.ParkingRepository
import com.parkmate.app.system.camera.AnnotationTool
import com.parkmate.app.system.camera.PhotoAnnotationItem
import com.parkmate.app.system.camera.PhotoStorageManager
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlin.math.hypot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PhotoReviewUiState(
    val selectedTool: AnnotationTool = AnnotationTool.ARROW,
    val annotations: List<PhotoAnnotationItem> = emptyList(),
    val selectedAnnotationId: String? = null,
    val isSavingPhoto: Boolean = false,
    val showTextDialog: Boolean = false,
    val editingAnnotationId: String? = null,
    val initialDialogText: String = "",
    val pendingTextPosition: Pair<Float, Float>? = null
)

@HiltViewModel
class CameraFlowViewModel @Inject constructor(
    private val parkingRepository: ParkingRepository,
    private val photoStorageManager: PhotoStorageManager
) : ViewModel() {

    private val _reviewState = MutableStateFlow(PhotoReviewUiState())
    val reviewState: StateFlow<PhotoReviewUiState> = _reviewState.asStateFlow()

    fun createCaptureFile(): File = photoStorageManager.createNewPhotoFile()

    fun onCameraFileCaptured(
        rawFilePath: String,
        onNormalized: (String) -> Unit
    ) {
        viewModelScope.launch {
            val uprightPath = photoStorageManager.normalizePhotoOrientation(rawFilePath)
            onNormalized(uprightPath)
        }
    }

    fun importGalleryPhoto(
        uri: Uri,
        onImported: (String) -> Unit
    ) {
        viewModelScope.launch {
            val savedPath = photoStorageManager.copyGalleryUriToInternalStorage(uri)
            if (savedPath != null) {
                onImported(savedPath)
            }
        }
    }

    fun captureFallbackSamplePhoto(onCaptured: (String) -> Unit) {
        viewModelScope.launch {
            val path = photoStorageManager.createSampleParkingPhoto()
            onCaptured(path)
        }
    }

    fun selectTool(tool: AnnotationTool) {
        _reviewState.update { it.copy(selectedTool = tool) }
    }

    fun selectAnnotation(id: String?) {
        _reviewState.update { it.copy(selectedAnnotationId = id) }
    }

    /**
     * Adds a new annotation at normalized coordinates [0..1] when tapping on an empty area.
     */
    fun addAnnotationAt(normalizedX: Float, normalizedY: Float) {
        val currentTool = _reviewState.value.selectedTool
        val x = normalizedX.coerceIn(0.06f, 0.94f)
        val y = normalizedY.coerceIn(0.06f, 0.94f)

        if (currentTool == AnnotationTool.TEXT) {
            _reviewState.update {
                it.copy(
                    showTextDialog = true,
                    editingAnnotationId = null,
                    initialDialogText = "",
                    pendingTextPosition = Pair(x, y)
                )
            }
            return
        }

        val newItem = when (currentTool) {
            AnnotationTool.ARROW -> PhotoAnnotationItem(
                tool = AnnotationTool.ARROW,
                normalizedX = (x - 0.09f).coerceIn(0.05f, 0.90f),
                normalizedY = (y + 0.08f).coerceIn(0.10f, 0.95f),
                normalizedEndX = (x + 0.09f).coerceIn(0.10f, 0.95f),
                normalizedEndY = (y - 0.08f).coerceIn(0.05f, 0.90f)
            )
            AnnotationTool.CIRCLE -> PhotoAnnotationItem(
                tool = AnnotationTool.CIRCLE,
                normalizedX = x,
                normalizedY = y,
                radiusNormalized = 0.11f
            )
            AnnotationTool.TEXT -> return
        }

        _reviewState.update { state ->
            state.copy(
                annotations = state.annotations + newItem,
                selectedAnnotationId = newItem.id
            )
        }
    }

    /**
     * Creates an annotation directly from a drag gesture on empty space and returns its ID.
     */
    fun createAnnotationFromDragStart(startX: Float, startY: Float): String? {
        val currentTool = _reviewState.value.selectedTool
        val sx = startX.coerceIn(0.04f, 0.96f)
        val sy = startY.coerceIn(0.04f, 0.96f)

        val newItem = when (currentTool) {
            AnnotationTool.ARROW -> PhotoAnnotationItem(
                tool = AnnotationTool.ARROW,
                normalizedX = sx,
                normalizedY = sy,
                normalizedEndX = (sx + 0.04f).coerceIn(0.04f, 0.96f),
                normalizedEndY = (sy - 0.04f).coerceIn(0.04f, 0.96f)
            )
            AnnotationTool.CIRCLE -> PhotoAnnotationItem(
                tool = AnnotationTool.CIRCLE,
                normalizedX = sx,
                normalizedY = sy,
                radiusNormalized = 0.06f
            )
            AnnotationTool.TEXT -> return null
        }

        _reviewState.update { state ->
            state.copy(
                annotations = state.annotations + newItem,
                selectedAnnotationId = newItem.id
            )
        }
        return newItem.id
    }

    /**
     * Moves an entire annotation by normalized deltas [dx, dy].
     */
    fun moveAnnotation(id: String, dx: Float, dy: Float) {
        _reviewState.update { state ->
            state.copy(
                annotations = state.annotations.map { item ->
                    if (item.id != id) return@map item
                    when (item.tool) {
                        AnnotationTool.ARROW -> {
                            val newStartX = (item.normalizedX + dx).coerceIn(0.03f, 0.97f)
                            val newStartY = (item.normalizedY + dy).coerceIn(0.03f, 0.97f)
                            val appliedDx = newStartX - item.normalizedX
                            val appliedDy = newStartY - item.normalizedY
                            item.copy(
                                normalizedX = newStartX,
                                normalizedY = newStartY,
                                normalizedEndX = (item.normalizedEndX + appliedDx).coerceIn(0.03f, 0.97f),
                                normalizedEndY = (item.normalizedEndY + appliedDy).coerceIn(0.03f, 0.97f)
                            )
                        }
                        AnnotationTool.CIRCLE, AnnotationTool.TEXT -> {
                            item.copy(
                                normalizedX = (item.normalizedX + dx).coerceIn(0.04f, 0.96f),
                                normalizedY = (item.normalizedY + dy).coerceIn(0.04f, 0.96f)
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Updates the start (tail) or end (tip) point of an arrow annotation.
     */
    fun updateArrowEndpoint(id: String, isEndTip: Boolean, newX: Float, newY: Float) {
        val clampedX = newX.coerceIn(0.03f, 0.97f)
        val clampedY = newY.coerceIn(0.03f, 0.97f)
        _reviewState.update { state ->
            state.copy(
                annotations = state.annotations.map { item ->
                    if (item.id != id || item.tool != AnnotationTool.ARROW) return@map item
                    if (isEndTip) {
                        item.copy(normalizedEndX = clampedX, normalizedEndY = clampedY)
                    } else {
                        item.copy(normalizedX = clampedX, normalizedY = clampedY)
                    }
                }
            )
        }
    }

    /**
     * Resizes a circle annotation based on finger distance from its center.
     */
    fun updateCircleRadiusFromPoint(id: String, touchX: Float, touchY: Float, aspectRatio: Float) {
        _reviewState.update { state ->
            state.copy(
                annotations = state.annotations.map { item ->
                    if (item.id != id || item.tool != AnnotationTool.CIRCLE) return@map item
                    val dx = touchX - item.normalizedX
                    val dy = (touchY - item.normalizedY) * aspectRatio
                    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    item.copy(radiusNormalized = dist.coerceIn(0.04f, 0.44f))
                }
            )
        }
    }

    /**
     * Scales the selected annotation (circle radius, text size, or arrow thickness/length) by [zoomFactor].
     */
    fun scaleSelectedAnnotation(zoomFactor: Float) {
        val selectedId = _reviewState.value.selectedAnnotationId ?: return
        _reviewState.update { state ->
            state.copy(
                annotations = state.annotations.map { item ->
                    if (item.id != selectedId) return@map item
                    when (item.tool) {
                        AnnotationTool.CIRCLE -> item.copy(
                            radiusNormalized = (item.radiusNormalized * zoomFactor).coerceIn(0.04f, 0.44f),
                            scale = (item.scale * zoomFactor).coerceIn(0.6f, 2.4f)
                        )
                        AnnotationTool.TEXT -> item.copy(
                            scale = (item.scale * zoomFactor).coerceIn(0.6f, 2.6f)
                        )
                        AnnotationTool.ARROW -> {
                            val midX = (item.normalizedX + item.normalizedEndX) / 2f
                            val midY = (item.normalizedY + item.normalizedEndY) / 2f
                            val halfDx = (item.normalizedEndX - item.normalizedX) / 2f * zoomFactor
                            val halfDy = (item.normalizedEndY - item.normalizedY) / 2f * zoomFactor
                            item.copy(
                                normalizedX = (midX - halfDx).coerceIn(0.03f, 0.97f),
                                normalizedY = (midY - halfDy).coerceIn(0.03f, 0.97f),
                                normalizedEndX = (midX + halfDx).coerceIn(0.03f, 0.97f),
                                normalizedEndY = (midY + halfDy).coerceIn(0.03f, 0.97f),
                                scale = (item.scale * zoomFactor).coerceIn(0.6f, 2.4f)
                            )
                        }
                    }
                }
            )
        }
    }

    fun openEditTextDialog(id: String) {
        val target = _reviewState.value.annotations.firstOrNull { it.id == id } ?: return
        if (target.tool != AnnotationTool.TEXT) return
        _reviewState.update {
            it.copy(
                showTextDialog = true,
                editingAnnotationId = id,
                initialDialogText = target.label,
                pendingTextPosition = Pair(target.normalizedX, target.normalizedY)
            )
        }
    }

    fun confirmTextAnnotation(label: String) {
        val trimmed = label.trim()
        if (trimmed.isBlank()) {
            dismissTextDialog()
            return
        }
        val editingId = _reviewState.value.editingAnnotationId
        if (editingId != null) {
            _reviewState.update { state ->
                state.copy(
                    annotations = state.annotations.map {
                        if (it.id == editingId) it.copy(label = trimmed) else it
                    },
                    selectedAnnotationId = editingId,
                    showTextDialog = false,
                    editingAnnotationId = null,
                    pendingTextPosition = null
                )
            }
        } else {
            val pos = _reviewState.value.pendingTextPosition ?: Pair(0.5f, 0.5f)
            val item = PhotoAnnotationItem(
                tool = AnnotationTool.TEXT,
                normalizedX = pos.first,
                normalizedY = pos.second,
                label = trimmed
            )
            _reviewState.update {
                it.copy(
                    annotations = it.annotations + item,
                    selectedAnnotationId = item.id,
                    showTextDialog = false,
                    editingAnnotationId = null,
                    pendingTextPosition = null
                )
            }
        }
    }

    fun dismissTextDialog() {
        _reviewState.update {
            it.copy(
                showTextDialog = false,
                editingAnnotationId = null,
                pendingTextPosition = null
            )
        }
    }

    fun deleteSelectedAnnotation() {
        val selectedId = _reviewState.value.selectedAnnotationId ?: return
        _reviewState.update { state ->
            val remaining = state.annotations.filterNot { it.id == selectedId }
            state.copy(
                annotations = remaining,
                selectedAnnotationId = remaining.lastOrNull()?.id
            )
        }
    }

    fun clearAnnotations() {
        _reviewState.update { it.copy(annotations = emptyList(), selectedAnnotationId = null) }
    }

    fun commitReviewedPhotoToSpot(
        spotId: Long,
        rawPhotoPath: String,
        onComplete: () -> Unit
    ) {
        if (_reviewState.value.isSavingPhoto) return
        _reviewState.update { it.copy(isSavingPhoto = true) }

        viewModelScope.launch {
            val finalPath = photoStorageManager.burnAnnotationsIntoPhoto(
                sourcePath = rawPhotoPath,
                annotations = _reviewState.value.annotations
            )
            val spot = parkingRepository.getSpotByIdOnce(spotId)
                ?: parkingRepository.getActiveSpotOnce()

            if (spot != null) {
                val updatedPhotos = (spot.photoPaths + finalPath).distinct().take(4)
                parkingRepository.updateSpot(spot.copy(photoPaths = updatedPhotos))
            }
            _reviewState.update { it.copy(isSavingPhoto = false) }
            onComplete()
        }
    }
}
