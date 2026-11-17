package com.parkmate.app.feature.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowOutward
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.parkmate.app.R
import com.parkmate.app.core.ui.components.ParkMatePrimaryButton
import com.parkmate.app.core.ui.components.ParkMateSecondaryButton
import com.parkmate.app.core.ui.components.ParkMateTopBar
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens
import com.parkmate.app.system.camera.AnnotationTool
import com.parkmate.app.system.camera.PhotoAnnotationItem
import java.io.File
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

private sealed interface DragInteraction {
    data class MoveItem(val id: String) : DragInteraction
    data class ArrowTail(val id: String) : DragInteraction
    data class ArrowTip(val id: String) : DragInteraction
    data class ResizeCircle(val id: String) : DragInteraction
    data class ScaleText(val id: String, val centerPx: Offset, val baseDistancePx: Float) : DragInteraction
}

@Composable
fun PhotoReviewScreen(
    spotId: Long,
    photoPath: String,
    viewModel: CameraFlowViewModel,
    onRetake: () -> Unit,
    onPhotoSaved: () -> Unit,
    onBack: () -> Unit
) {
    val state by viewModel.reviewState.collectAsStateWithLifecycle()
    val currentState by rememberUpdatedState(state)
    val colors = ParkMateThemeTokens.colors
    val textMeasurer = rememberTextMeasurer()

    if (state.showTextDialog) {
        var textInput by remember(state.editingAnnotationId, state.initialDialogText) {
            mutableStateOf(state.initialDialogText.ifBlank { "Pillar C4" })
        }
        AlertDialog(
            onDismissRequest = { viewModel.dismissTextDialog() },
            title = { Text(stringResource(R.string.review_text_prompt_title)) },
            text = {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it.take(30) },
                    placeholder = { Text(stringResource(R.string.review_text_prompt_hint)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmTextAnnotation(textInput) }) {
                    Text(stringResource(R.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissTextDialog() }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ParkMateTopBar(
            title = stringResource(R.string.review_title),
            onBack = onBack,
            action = {
                if (state.annotations.isNotEmpty()) {
                    TextButton(onClick = { viewModel.clearAnnotations() }) {
                        Text(
                            text = stringResource(R.string.review_clear_annotations),
                            color = colors.blue,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        )

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isLandscape = maxWidth > maxHeight && maxHeight < 520.dp

            if (isLandscape) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhotoAnnotationCanvasBox(
                        photoPath = photoPath,
                        state = state,
                        currentStateProvider = { currentState },
                        viewModel = viewModel,
                        textMeasurer = textMeasurer,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )

                    Column(
                        modifier = Modifier
                            .width(240.dp)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.review_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ParkMatePrimaryButton(
                                text = stringResource(R.string.review_use_photo),
                                enabled = !state.isSavingPhoto,
                                onClick = {
                                    viewModel.commitReviewedPhotoToSpot(
                                        spotId = spotId,
                                        rawPhotoPath = photoPath,
                                        onComplete = onPhotoSaved
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            ParkMateSecondaryButton(
                                text = stringResource(R.string.review_retake),
                                onClick = onRetake,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    PhotoAnnotationCanvasBox(
                        photoPath = photoPath,
                        state = state,
                        currentStateProvider = { currentState },
                        viewModel = viewModel,
                        textMeasurer = textMeasurer,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.review_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Bottom Actions (Retake & Use photo)
                    Row(
                        modifier = Modifier
                            .widthIn(max = 520.dp)
                            .fillMaxWidth()
                            .align(Alignment.CenterHorizontally)
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ParkMateSecondaryButton(
                            text = stringResource(R.string.review_retake),
                            onClick = onRetake,
                            modifier = Modifier.weight(1f)
                        )
                        ParkMatePrimaryButton(
                            text = stringResource(R.string.review_use_photo),
                            enabled = !state.isSavingPhoto,
                            onClick = {
                                viewModel.commitReviewedPhotoToSpot(
                                    spotId = spotId,
                                    rawPhotoPath = photoPath,
                                    onComplete = onPhotoSaved
                                )
                            },
                            modifier = Modifier.weight(1.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoAnnotationCanvasBox(
    photoPath: String,
    state: PhotoReviewUiState,
    currentStateProvider: () -> PhotoReviewUiState,
    viewModel: CameraFlowViewModel,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    modifier: Modifier = Modifier
) {
    val colors = ParkMateThemeTokens.colors

    // Review photo container with interactive multi-gesture annotation canvas
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.line)
    ) {
        AsyncImage(
            model = File(photoPath),
            contentDescription = stringResource(R.string.review_title),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        var activeDrag by remember { mutableStateOf<DragInteraction?>(null) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { tapOffset ->
                            val currentState = currentStateProvider()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (w <= 0f || h <= 0f) return@detectTapGestures
                            val hit = findHitAnnotation(
                                tapPx = tapOffset,
                                canvasW = w,
                                canvasH = h,
                                annotations = currentState.annotations,
                                selectedId = currentState.selectedAnnotationId
                            )
                            if (hit is DragInteraction.MoveItem) {
                                val item = currentState.annotations.firstOrNull { it.id == hit.id }
                                if (item?.tool == AnnotationTool.TEXT) {
                                    viewModel.openEditTextDialog(item.id)
                                }
                            }
                        },
                        onTap = { tapOffset ->
                            val currentState = currentStateProvider()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (w <= 0f || h <= 0f) return@detectTapGestures

                            val hit = findHitAnnotation(
                                tapPx = tapOffset,
                                canvasW = w,
                                canvasH = h,
                                annotations = currentState.annotations,
                                selectedId = currentState.selectedAnnotationId
                            )

                            when (hit) {
                                is DragInteraction.MoveItem -> viewModel.selectAnnotation(hit.id)
                                is DragInteraction.ArrowTail -> viewModel.selectAnnotation(hit.id)
                                is DragInteraction.ArrowTip -> viewModel.selectAnnotation(hit.id)
                                is DragInteraction.ResizeCircle -> viewModel.selectAnnotation(hit.id)
                                is DragInteraction.ScaleText -> viewModel.selectAnnotation(hit.id)
                                null -> {
                                    viewModel.addAnnotationAt(
                                        normalizedX = (tapOffset.x / w).coerceIn(0.05f, 0.95f),
                                        normalizedY = (tapOffset.y / h).coerceIn(0.05f, 0.95f)
                                    )
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { startOffset ->
                            val currentState = currentStateProvider()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (w <= 0f || h <= 0f) return@detectDragGestures

                            val hit = findHitAnnotation(
                                tapPx = startOffset,
                                canvasW = w,
                                canvasH = h,
                                annotations = currentState.annotations,
                                selectedId = currentState.selectedAnnotationId
                            )

                            if (hit != null) {
                                val id = when (hit) {
                                    is DragInteraction.MoveItem -> hit.id
                                    is DragInteraction.ArrowTail -> hit.id
                                    is DragInteraction.ArrowTip -> hit.id
                                    is DragInteraction.ResizeCircle -> hit.id
                                    is DragInteraction.ScaleText -> hit.id
                                }
                                viewModel.selectAnnotation(id)
                                activeDrag = hit
                            } else {
                                val newId = viewModel.createAnnotationFromDragStart(
                                    startX = startOffset.x / w,
                                    startY = startOffset.y / h
                                )
                                activeDrag = when (currentState.selectedTool) {
                                    AnnotationTool.ARROW -> newId?.let { DragInteraction.ArrowTip(it) }
                                    AnnotationTool.CIRCLE -> newId?.let { DragInteraction.ResizeCircle(it) }
                                    AnnotationTool.TEXT -> null
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            if (w <= 0f || h <= 0f) return@detectDragGestures

                            when (val interaction = activeDrag) {
                                is DragInteraction.MoveItem -> {
                                    viewModel.moveAnnotation(
                                        id = interaction.id,
                                        dx = dragAmount.x / w,
                                        dy = dragAmount.y / h
                                    )
                                }
                                is DragInteraction.ArrowTail -> {
                                    viewModel.updateArrowEndpoint(
                                        id = interaction.id,
                                        isEndTip = false,
                                        newX = change.position.x / w,
                                        newY = change.position.y / h
                                    )
                                }
                                is DragInteraction.ArrowTip -> {
                                    viewModel.updateArrowEndpoint(
                                        id = interaction.id,
                                        isEndTip = true,
                                        newX = change.position.x / w,
                                        newY = change.position.y / h
                                    )
                                }
                                is DragInteraction.ResizeCircle -> {
                                    viewModel.updateCircleRadiusFromPoint(
                                        id = interaction.id,
                                        touchX = change.position.x / w,
                                        touchY = change.position.y / h,
                                        aspectRatio = h / w
                                    )
                                }
                                is DragInteraction.ScaleText -> {
                                    val currentDist = hypot(
                                        (change.position.x - interaction.centerPx.x).toDouble(),
                                        (change.position.y - interaction.centerPx.y).toDouble()
                                    ).toFloat().coerceAtLeast(20f)
                                    val prevDist = hypot(
                                        (change.previousPosition.x - interaction.centerPx.x).toDouble(),
                                        (change.previousPosition.y - interaction.centerPx.y).toDouble()
                                    ).toFloat().coerceAtLeast(20f)
                                    viewModel.scaleSelectedAnnotation(currentDist / prevDist)
                                }
                                null -> Unit
                            }
                        },
                        onDragEnd = { activeDrag = null },
                        onDragCancel = { activeDrag = null }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val minDim = min(w, h)
            val dashedStroke = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
            )

            state.annotations.forEach { item ->
                val isSelected = item.id == state.selectedAnnotationId
                val itemScale = item.scale.coerceIn(0.5f, 2.8f)
                val startX = item.normalizedX * w
                val startY = item.normalizedY * h

                when (item.tool) {
                    AnnotationTool.ARROW -> {
                        val endX = item.normalizedEndX * w
                        val endY = item.normalizedEndY * h
                        val start = Offset(startX, startY)
                        val end = Offset(endX, endY)
                        val length = hypot((endX - startX).toDouble(), (endY - startY).toDouble()).toFloat()
                        val headLen = (length * 0.28f).coerceIn(22.dp.toPx(), 48.dp.toPx()) * itemScale

                        drawLine(
                            color = Color.White,
                            start = start,
                            end = end,
                            strokeWidth = 14f * itemScale,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = colors.blue,
                            start = start,
                            end = end,
                            strokeWidth = 9f * itemScale,
                            cap = StrokeCap.Round
                        )
                        val angle = atan2((endY - startY).toDouble(), (endX - startX).toDouble())
                        val arrowAngle = Math.toRadians(28.0)
                        val p1 = Offset(
                            endX - (headLen * cos(angle - arrowAngle)).toFloat(),
                            endY - (headLen * sin(angle - arrowAngle)).toFloat()
                        )
                        val p2 = Offset(
                            endX - (headLen * cos(angle + arrowAngle)).toFloat(),
                            endY - (headLen * sin(angle + arrowAngle)).toFloat()
                        )
                        val path = Path().apply {
                            moveTo(endX, endY)
                            lineTo(p1.x, p1.y)
                            moveTo(endX, endY)
                            lineTo(p2.x, p2.y)
                        }
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(width = 14f * itemScale, cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = path,
                            color = colors.blue,
                            style = Stroke(width = 9f * itemScale, cap = StrokeCap.Round)
                        )

                        if (isSelected) {
                            val handleRadius = 9.dp.toPx()
                            listOf(start, end).forEach { pt ->
                                drawCircle(color = Color.White, radius = handleRadius + 3.dp.toPx(), center = pt)
                                drawCircle(color = colors.blue, radius = handleRadius, center = pt)
                                drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = pt)
                            }
                        }
                    }

                    AnnotationTool.CIRCLE -> {
                        val radius = item.radiusNormalized.coerceIn(0.04f, 0.44f) * minDim
                        val center = Offset(startX, startY)

                        drawCircle(
                            color = Color.White,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 12f * itemScale)
                        )
                        drawCircle(
                            color = colors.blue,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 8f * itemScale)
                        )

                        if (isSelected) {
                            drawCircle(
                                color = Color.White.copy(alpha = 0.85f),
                                radius = radius + 10.dp.toPx(),
                                center = center,
                                style = dashedStroke
                            )
                            val handleCenter = Offset(center.x + radius, center.y)
                            val handleR = 10.dp.toPx()
                            drawCircle(color = Color.White, radius = handleR + 3.dp.toPx(), center = handleCenter)
                            drawCircle(color = colors.blue, radius = handleR, center = handleCenter)
                            drawCircle(color = Color.White, radius = 4.dp.toPx(), center = handleCenter)
                        }
                    }

                    AnnotationTool.TEXT -> {
                        val layoutResult = textMeasurer.measure(
                            text = item.label,
                            style = TextStyle(
                                color = Color.White,
                                fontSize = (14f * itemScale).sp,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        val padH = 12.dp.toPx() * itemScale
                        val padV = 7.dp.toPx() * itemScale
                        val boxW = layoutResult.size.width + padH * 2
                        val boxH = layoutResult.size.height + padV * 2
                        val topLeft = Offset(startX - boxW / 2f, startY - boxH / 2f)
                        val corner = CornerRadius(12.dp.toPx() * itemScale, 12.dp.toPx() * itemScale)

                        drawRoundRect(
                            color = colors.blue,
                            topLeft = topLeft,
                            size = Size(boxW, boxH),
                            cornerRadius = corner
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = topLeft,
                            size = Size(boxW, boxH),
                            cornerRadius = corner,
                            style = Stroke(width = if (isSelected) 3.dp.toPx() else 1.5.dp.toPx())
                        )
                        drawText(
                            textLayoutResult = layoutResult,
                            topLeft = Offset(topLeft.x + padH, topLeft.y + padV)
                        )

                        if (isSelected) {
                            val handleCenter = Offset(topLeft.x + boxW, topLeft.y + boxH)
                            val handleR = 9.dp.toPx()
                            drawCircle(color = Color.White, radius = handleR + 2.5.dp.toPx(), center = handleCenter)
                            drawCircle(color = colors.blue, radius = handleR, center = handleCenter)
                            drawCircle(color = Color.White, radius = 3.5.dp.toPx(), center = handleCenter)
                        }
                    }
                }
            }
        }

        // Contextual controls for the currently selected annotation (Resize -, +, Edit, Delete)
        val selectedItem = state.annotations.firstOrNull { it.id == state.selectedAnnotationId }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 14.dp)
        ) {
            if (selectedItem != null) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xD9161920),
                    shadowElevation = 6.dp,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.scaleSelectedAnnotation(0.86f) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Remove,
                                contentDescription = stringResource(R.string.cd_shrink_annotation),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.scaleSelectedAnnotation(1.16f) },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = stringResource(R.string.cd_enlarge_annotation),
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        if (selectedItem.tool == AnnotationTool.TEXT) {
                            IconButton(
                                onClick = { viewModel.openEditTextDialog(selectedItem.id) },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        IconButton(
                            onClick = { viewModel.deleteSelectedAnnotation() },
                            modifier = Modifier.size(38.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteOutline,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = Color(0xFFFF8080),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Floating Annotation Toolbar (Arrow, Circle, Text)
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = colors.surface.copy(alpha = 0.95f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(5.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tools = listOf(
                        Triple(AnnotationTool.ARROW, Icons.Rounded.ArrowOutward, R.string.cd_annotation_arrow),
                        Triple(AnnotationTool.CIRCLE, Icons.Rounded.RadioButtonUnchecked, R.string.cd_annotation_circle),
                        Triple(AnnotationTool.TEXT, Icons.Rounded.TextFields, R.string.cd_annotation_text)
                    )
                    tools.forEach { (tool, icon, cdRes) ->
                        val isSelected = state.selectedTool == tool
                        IconButton(
                            onClick = { viewModel.selectTool(tool) },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(if (isSelected) colors.blue else Color.Transparent)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = stringResource(cdRes),
                                tint = if (isSelected) Color.White else colors.muted
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Hit-tests handles first (for the selected annotation), then annotation bodies from top-most to bottom-most.
 */
private fun findHitAnnotation(
    tapPx: Offset,
    canvasW: Float,
    canvasH: Float,
    annotations: List<PhotoAnnotationItem>,
    selectedId: String?
): DragInteraction? {
    val minDim = min(canvasW, canvasH)
    val handleHitRadius = 34f

    // 1. Check resize/endpoint handles on the currently selected item first
    val selected = annotations.firstOrNull { it.id == selectedId }
    if (selected != null) {
        val sx = selected.normalizedX * canvasW
        val sy = selected.normalizedY * canvasH
        when (selected.tool) {
            AnnotationTool.ARROW -> {
                val ex = selected.normalizedEndX * canvasW
                val ey = selected.normalizedEndY * canvasH
                if (hypot((tapPx.x - ex).toDouble(), (tapPx.y - ey).toDouble()) <= handleHitRadius * 1.3f) {
                    return DragInteraction.ArrowTip(selected.id)
                }
                if (hypot((tapPx.x - sx).toDouble(), (tapPx.y - sy).toDouble()) <= handleHitRadius * 1.3f) {
                    return DragInteraction.ArrowTail(selected.id)
                }
            }
            AnnotationTool.CIRCLE -> {
                val r = selected.radiusNormalized.coerceIn(0.04f, 0.44f) * minDim
                val distFromCenter = hypot((tapPx.x - sx).toDouble(), (tapPx.y - sy).toDouble()).toFloat()
                // Near the circle's rim or right handle -> resize circle
                if (kotlin.math.abs(distFromCenter - r) <= handleHitRadius * 1.15f) {
                    return DragInteraction.ResizeCircle(selected.id)
                }
            }
            AnnotationTool.TEXT -> {
                val itemScale = selected.scale.coerceIn(0.5f, 2.8f)
                val approxHalfW = (selected.label.length * 11f + 36f) * itemScale
                val approxHalfH = 26f * itemScale
                val cornerX = sx + approxHalfW
                val cornerY = sy + approxHalfH
                if (hypot((tapPx.x - cornerX).toDouble(), (tapPx.y - cornerY).toDouble()) <= handleHitRadius * 1.3f) {
                    val baseDist = hypot((cornerX - sx).toDouble(), (cornerY - sy).toDouble()).toFloat()
                    return DragInteraction.ScaleText(selected.id, Offset(sx, sy), baseDist)
                }
            }
        }
    }

    // 2. Check annotation bodies from top-most to bottom-most
    for (item in annotations.asReversed()) {
        val sx = item.normalizedX * canvasW
        val sy = item.normalizedY * canvasH
        when (item.tool) {
            AnnotationTool.ARROW -> {
                val ex = item.normalizedEndX * canvasW
                val ey = item.normalizedEndY * canvasH
                if (hypot((tapPx.x - ex).toDouble(), (tapPx.y - ey).toDouble()) <= handleHitRadius) {
                    return DragInteraction.ArrowTip(item.id)
                }
                if (hypot((tapPx.x - sx).toDouble(), (tapPx.y - sy).toDouble()) <= handleHitRadius) {
                    return DragInteraction.ArrowTail(item.id)
                }
                val distToSegment = distancePointToSegment(tapPx, Offset(sx, sy), Offset(ex, ey))
                if (distToSegment <= 38f) {
                    return DragInteraction.MoveItem(item.id)
                }
            }
            AnnotationTool.CIRCLE -> {
                val r = item.radiusNormalized.coerceIn(0.04f, 0.44f) * minDim
                val dist = hypot((tapPx.x - sx).toDouble(), (tapPx.y - sy).toDouble()).toFloat()
                if (kotlin.math.abs(dist - r) <= 28f) {
                    return DragInteraction.ResizeCircle(item.id)
                }
                if (dist < r) {
                    return DragInteraction.MoveItem(item.id)
                }
            }
            AnnotationTool.TEXT -> {
                val itemScale = item.scale.coerceIn(0.5f, 2.8f)
                val approxHalfW = (item.label.length * 12f + 42f) * itemScale
                val approxHalfH = 34f * itemScale
                if (kotlin.math.abs(tapPx.x - sx) <= approxHalfW &&
                    kotlin.math.abs(tapPx.y - sy) <= approxHalfH
                ) {
                    return DragInteraction.MoveItem(item.id)
                }
            }
        }
    }

    return null
}

private fun distancePointToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abX = b.x - a.x
    val abY = b.y - a.y
    val lenSq = abX * abX + abY * abY
    if (lenSq == 0f) return hypot((p.x - a.x).toDouble(), (p.y - a.y).toDouble()).toFloat()
    val t = (((p.x - a.x) * abX + (p.y - a.y) * abY) / lenSq).coerceIn(0f, 1f)
    val projX = a.x + t * abX
    val projY = a.y + t * abY
    return hypot((p.x - projX).toDouble(), (p.y - projY).toDouble()).toFloat()
}
