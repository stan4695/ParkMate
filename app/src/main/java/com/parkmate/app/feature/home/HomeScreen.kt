package com.parkmate.app.feature.home

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.PathParser
import kotlin.math.roundToInt
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkmate.app.R
import com.parkmate.app.core.ui.components.ParkMateOutlineButton
import com.parkmate.app.core.ui.components.ParkMatePrimaryButton
import com.parkmate.app.core.ui.components.ParkMateTextButton
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens
import com.parkmate.app.feature.details.localizeQuickTag
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

private enum class HomeSheetState {
    COLLAPSED,
    STANDARD,
    EXPANDED
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenCurrentSpotDetails: (Long) -> Unit,
    onNavigateQuickSave: (Long) -> Unit,
    onNavigateGuideMe: (Long) -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ParkMateThemeTokens.colors
    var pendingSaveAfterPermission by remember { mutableStateOf(false) }
    var hasPromptedLocationOnLaunch by rememberSaveable { mutableStateOf(false) }
    var sheetState by rememberSaveable { mutableStateOf(HomeSheetState.STANDARD) }
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refreshPermissionAndLocation()
        if (pendingSaveAfterPermission) {
            pendingSaveAfterPermission = false
            viewModel.saveCurrentSpotManual()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionAndLocation()
        if (!state.hasLocationPermission && !hasPromptedLocationOnLaunch) {
            hasPromptedLocationOnLaunch = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.SpotSavedNavigateQuickSave -> {
                    onNavigateQuickSave(event.spotId)
                }
            }
        }
    }

    var mapViewRef by remember { mutableStateOf<MapView?>(null) }
    var hasCenteredOnRealLocation by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.userLocation.latitude, state.userLocation.longitude, state.userLocation.isFallback) {
        if (!state.userLocation.isFallback && !hasCenteredOnRealLocation) {
            hasCenteredOnRealLocation = true
            mapViewRef?.controller?.setZoom(17.5)
            mapViewRef?.controller?.animateTo(
                GeoPoint(state.userLocation.latitude, state.userLocation.longitude)
            )
        }
    }

    val triggerSaveSpot = {
        if (state.hasLocationPermission) {
            viewModel.saveCurrentSpotManual()
        } else {
            pendingSaveAfterPermission = true
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(modifier = Modifier.fillMaxSize()) {
        OsmParkingMap(
            userLat = state.userLocation.latitude,
            userLng = state.userLocation.longitude,
            carLat = state.activeSpot?.latitude,
            carLng = state.activeSpot?.longitude,
            selectedPinLat = state.selectedMapPoint?.latitude,
            selectedPinLng = state.selectedMapPoint?.longitude,
            onMapLongPress = { lat, lng ->
                viewModel.onMapLongPress(lat, lng)
            },
            onMapSingleTap = {
                if (state.selectedMapPoint != null) {
                    viewModel.dismissSelectedMapPoint()
                }
            },
            onMapReady = { mapViewRef = it },
            modifier = Modifier.fillMaxSize()
        )

        // Brand pill top-left
        Surface(
            shape = RoundedCornerShape(13.dp),
            color = colors.surface.copy(alpha = 0.94f),
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = statusBarTop + 12.dp, start = 16.dp)
                .height(38.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.LocationOn,
                    contentDescription = null,
                    tint = colors.blue,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.text
                )
            }
        }

        // Top-right button: centers on parked car (if active) and ensures North-up
        if (state.activeSpot != null) {
            Surface(
                shape = RoundedCornerShape(15.dp),
                color = colors.surface,
                shadowElevation = 8.dp,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = statusBarTop + 12.dp, end = 16.dp)
                    .size(48.dp)
            ) {
                IconButton(
                    onClick = {
                        val spot = state.activeSpot ?: return@IconButton
                        mapViewRef?.mapOrientation = 0f
                        mapViewRef?.controller?.setZoom(17.5)
                        mapViewRef?.controller?.animateTo(GeoPoint(spot.latitude, spot.longitude))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DirectionsCar,
                        contentDescription = stringResource(R.string.cd_reset_map_orientation),
                        tint = colors.blue
                    )
                }
            }
        }

        // Locate button above bottom sheet — smoothly adjusts position as sheet expands/collapses
        val locateButtonBottomPadding by animateDpAsState(
            targetValue = when {
                state.selectedMapPoint != null -> 252.dp
                sheetState == HomeSheetState.COLLAPSED -> 112.dp
                sheetState == HomeSheetState.STANDARD -> if (state.activeSpot != null) 294.dp else 210.dp
                sheetState == HomeSheetState.EXPANDED -> if (state.activeSpot != null) 410.dp else 210.dp
                else -> 210.dp
            },
            animationSpec = tween(durationMillis = 280),
            label = "locateBtnPadding"
        )

        Surface(
            shape = RoundedCornerShape(15.dp),
            color = colors.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = locateButtonBottomPadding, end = 16.dp)
                .size(48.dp)
        ) {
            IconButton(
                onClick = {
                    if (!state.hasLocationPermission) {
                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    } else {
                        viewModel.refreshPermissionAndLocation()
                        val target = GeoPoint(state.userLocation.latitude, state.userLocation.longitude)
                        mapViewRef?.mapOrientation = 0f
                        mapViewRef?.controller?.setZoom(17.5)
                        mapViewRef?.controller?.animateTo(target)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = stringResource(R.string.cd_center_my_location),
                    tint = colors.blue
                )
            }
        }

        val stepSheetUp = {
            sheetState = when (sheetState) {
                HomeSheetState.COLLAPSED -> HomeSheetState.STANDARD
                HomeSheetState.STANDARD -> if (state.activeSpot != null) {
                    HomeSheetState.EXPANDED
                } else {
                    HomeSheetState.STANDARD
                }
                HomeSheetState.EXPANDED -> HomeSheetState.EXPANDED
            }
        }

        val stepSheetDown = {
            if (state.selectedMapPoint != null) {
                viewModel.dismissSelectedMapPoint()
            } else {
                sheetState = when (sheetState) {
                    HomeSheetState.EXPANDED -> HomeSheetState.STANDARD
                    HomeSheetState.STANDARD -> HomeSheetState.COLLAPSED
                    HomeSheetState.COLLAPSED -> HomeSheetState.COLLAPSED
                }
            }
        }

        val animatedDragVisualOffset by animateFloatAsState(
            targetValue = (cumulativeDragY * 0.25f).coerceIn(-28f, 36f),
            animationSpec = tween(durationMillis = 60),
            label = "sheetDragVisualOffset"
        )

        val sheetDraggableState = rememberDraggableState { delta ->
            cumulativeDragY += delta
            // Live continuous step while dragging a longer distance without lifting the finger
            if (cumulativeDragY < -115f) {
                stepSheetUp()
                cumulativeDragY = 0f
            } else if (cumulativeDragY > 115f) {
                stepSheetDown()
                cumulativeDragY = 0f
            }
        }

        // Interactive Draggable Bottom Sheet Card
        Surface(
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            color = colors.surface,
            shadowElevation = 16.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .offset { IntOffset(0, animatedDragVisualOffset.roundToInt().coerceAtLeast(0)) }
                .draggable(
                    state = sheetDraggableState,
                    orientation = Orientation.Vertical,
                    onDragStarted = { cumulativeDragY = 0f },
                    onDragStopped = { velocity ->
                        if (cumulativeDragY < -28f || velocity < -280f) {
                            stepSheetUp()
                        } else if (cumulativeDragY > 28f || velocity > 280f) {
                            stepSheetDown()
                        }
                        cumulativeDragY = 0f
                    }
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                // Interactive handle touch area (swipe up/down or tap to cycle sheet states)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (state.selectedMapPoint != null) {
                                viewModel.dismissSelectedMapPoint()
                            } else {
                                sheetState = when (sheetState) {
                                    HomeSheetState.COLLAPSED -> HomeSheetState.STANDARD
                                    HomeSheetState.STANDARD -> if (state.activeSpot != null) {
                                        HomeSheetState.EXPANDED
                                    } else {
                                        HomeSheetState.COLLAPSED
                                    }
                                    HomeSheetState.EXPANDED -> HomeSheetState.COLLAPSED
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.muted.copy(alpha = 0.42f))
                    )
                }

                val selectedPoint = state.selectedMapPoint
                val spot = state.activeSpot

                if (selectedPoint != null) {
                    // Long-Press Custom Map Pin Confirmation Card
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.amberSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AddLocationAlt,
                                contentDescription = null,
                                tint = colors.amber,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_custom_pin_eyebrow),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.amber
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedPoint.addressLine,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${formatDistanceText(selectedPoint.distanceMeters)} • ${stringResource(R.string.home_walk_minutes, selectedPoint.walkMinutes)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.muted
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissSelectedMapPoint() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.action_cancel),
                                tint = colors.muted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    ParkMatePrimaryButton(
                        text = if (state.isSavingSpot) {
                            stringResource(R.string.home_saving_location)
                        } else {
                            stringResource(R.string.home_custom_pin_save_btn)
                        },
                        icon = Icons.Rounded.LocationOn,
                        enabled = !state.isSavingSpot,
                        onClick = { viewModel.confirmSaveSelectedMapPoint() }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    ParkMateTextButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = { viewModel.dismissSelectedMapPoint() }
                    )
                } else if (spot != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenCurrentSpotDetails(spot.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.blueSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = colors.blue,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = spot.addressLine,
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = formatParkedAgo(state.parkedElapsedMinutes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = colors.muted
                        )
                    }

                    // Body visible in STANDARD and EXPANDED states
                    AnimatedVisibility(
                        visible = sheetState != HomeSheetState.COLLAPSED,
                        enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(220)) + fadeOut(tween(160))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Walk info row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 52.dp, top = 12.dp, bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                                    contentDescription = null,
                                    tint = colors.muted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatDistanceText(state.distanceMeters),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.muted
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(
                                    modifier = Modifier
                                        .size(3.dp)
                                        .clip(CircleShape)
                                        .background(colors.muted)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = stringResource(R.string.home_walk_minutes, state.walkMinutes),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.muted
                                )
                            }

                            // Extra details visible when dragged UP to EXPANDED state
                            AnimatedVisibility(
                                visible = sheetState == HomeSheetState.EXPANDED,
                                enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                                exit = shrinkVertically(tween(200)) + fadeOut(tween(150))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    HorizontalDivider(color = colors.line)
                                    Spacer(modifier = Modifier.height(10.dp))

                                    if (spot.tags.isNotEmpty()) {
                                        FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.padding(bottom = 10.dp)
                                        ) {
                                            spot.tags.forEach { tag ->
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(colors.blueSoft)
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        text = localizeQuickTag(tag),
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = colors.blue
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.bg)
                                            .padding(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.Notes,
                                            contentDescription = null,
                                            tint = colors.blue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = spot.note.ifBlank { stringResource(R.string.spot_no_note) },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.text,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    ParkMateOutlineButton(
                                        text = stringResource(R.string.spot_details_title),
                                        onClick = { onOpenCurrentSpotDetails(spot.id) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            ParkMatePrimaryButton(
                                text = stringResource(R.string.home_guide_me),
                                icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                                onClick = { onNavigateGuideMe(spot.id) }
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            if (state.isSavingSpot) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.5.dp,
                                        color = colors.blue
                                    )
                                }
                            } else {
                                ParkMateTextButton(
                                    text = stringResource(R.string.home_save_new_spot),
                                    onClick = triggerSaveSpot
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.blueSoft),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = colors.blue
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.home_no_spot_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = stringResource(R.string.home_no_spot_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.muted
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = sheetState != HomeSheetState.COLLAPSED,
                        enter = expandVertically(tween(240)) + fadeIn(tween(200)),
                        exit = shrinkVertically(tween(220)) + fadeOut(tween(160))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            ParkMatePrimaryButton(
                                text = if (state.isSavingSpot) {
                                    stringResource(R.string.home_saving_location)
                                } else {
                                    stringResource(R.string.home_park_here)
                                },
                                icon = Icons.Rounded.LocationOn,
                                enabled = !state.isSavingSpot,
                                onClick = triggerSaveSpot
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun formatParkedAgo(elapsedMinutes: Long): String {
    return when {
        elapsedMinutes < 1L -> stringResource(R.string.home_parked_just_now)
        elapsedMinutes < 60L -> stringResource(R.string.home_parked_minutes_ago, elapsedMinutes.toInt())
        else -> {
            val hours = (elapsedMinutes / 60L).toInt()
            val mins = (elapsedMinutes % 60L).toInt()
            stringResource(R.string.home_parked_hours_minutes_ago, hours, mins)
        }
    }
}

@Composable
fun formatDistanceText(distanceMeters: Int): String {
    return if (distanceMeters < 1000) {
        stringResource(R.string.home_distance_meters, distanceMeters)
    } else {
        stringResource(R.string.home_distance_km, distanceMeters / 1000f)
    }
}

/**
 * Clean, high-DPI (scale=2) Google Maps Road tile source — no API key required and no artificial color filter.
 */
private val GoogleMapsTileSource: OnlineTileSourceBase = object : XYTileSource(
    "GoogleMapsClean2x",
    1,
    20,
    256,
    ".png",
    arrayOf(
        "https://mt0.google.com/vt/lyrs=m&scale=2",
        "https://mt1.google.com/vt/lyrs=m&scale=2",
        "https://mt2.google.com/vt/lyrs=m&scale=2",
        "https://mt3.google.com/vt/lyrs=m&scale=2"
    ),
    "© Google Maps"
) {
    override fun getTileURLString(pMapTileIndex: Long): String {
        val x = MapTileIndex.getX(pMapTileIndex)
        val y = MapTileIndex.getY(pMapTileIndex)
        val z = MapTileIndex.getZoom(pMapTileIndex)
        return "$baseUrl&x=$x&y=$y&z=$z"
    }
}

@Composable
private fun OsmParkingMap(
    userLat: Double,
    userLng: Double,
    carLat: Double?,
    carLng: Double?,
    selectedPinLat: Double?,
    selectedPinLng: Double?,
    onMapLongPress: (Double, Double) -> Unit,
    onMapSingleTap: () -> Unit,
    onMapReady: (MapView) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnLongPress by rememberUpdatedState(onMapLongPress)
    val currentOnSingleTap by rememberUpdatedState(onMapSingleTap)

    val carPinIcon = remember(context) { createCarPinDrawable(context) }
    val selectedPinIcon = remember(context) { createSelectedPinDrawable(context) }
    val userDotIcon = remember(context) { createUserDotDrawable(context) }

    val mapView = remember {
        Configuration.getInstance().userAgentValue =
            "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
        MapView(context).apply {
            setTileSource(GoogleMapsTileSource)
            isTilesScaledToDpi = true
            setMultiTouchControls(true)
            // Keep North-up locked at all times so pinch-to-zoom never spins the map
            mapOrientation = 0f
            maxZoomLevel = 20.0
            minZoomLevel = 4.0
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            overlayManager.tilesOverlay.loadingBackgroundColor = AndroidColor.parseColor("#F1F3F4")
            overlayManager.tilesOverlay.loadingLineColor = AndroidColor.parseColor("#E0E3E7")
            overlayManager.tilesOverlay.setColorFilter(null)
            controller.setZoom(17.5)
            controller.setCenter(GeoPoint(carLat ?: userLat, carLng ?: userLng))
        }
    }

    val mapEventsOverlay = remember(mapView) {
        MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                currentOnSingleTap()
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                if (p != null) {
                    mapView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    mapView.controller.animateTo(p)
                    currentOnLongPress(p.latitude, p.longitude)
                    return true
                }
                return false
            }
        })
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = {
            onMapReady(mapView)
            mapView
        },
        update = { view ->
            // Enforce North-up orientation so zoom gestures never tilt/rotate the map
            if (view.mapOrientation != 0f) {
                view.mapOrientation = 0f
            }

            view.overlays.clear()
            view.overlays.add(mapEventsOverlay)

            val userPoint = GeoPoint(userLat, userLng)

            if (carLat != null && carLng != null) {
                val carPoint = GeoPoint(carLat, carLng)

                // Outer white casing line for high contrast on any street
                val routeCasing = Polyline(view).apply {
                    setPoints(listOf(userPoint, carPoint))
                    outlinePaint.color = AndroidColor.parseColor("#CCFFFFFF")
                    outlinePaint.strokeWidth = 18f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.isAntiAlias = true
                }
                view.overlays.add(routeCasing)

                // Inner dashed blue walking route line
                val routeLine = Polyline(view).apply {
                    setPoints(listOf(userPoint, carPoint))
                    outlinePaint.color = AndroidColor.parseColor("#3B6CF6")
                    outlinePaint.strokeWidth = 11f
                    outlinePaint.strokeCap = Paint.Cap.ROUND
                    outlinePaint.isAntiAlias = true
                    outlinePaint.pathEffect = DashPathEffect(floatArrayOf(16f, 22f), 0f)
                }
                view.overlays.add(routeLine)

                // Car Pin Marker
                val carMarker = Marker(view).apply {
                    position = carPoint
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = carPinIcon
                    infoWindow = null
                }
                view.overlays.add(carMarker)
            }

            // Long-pressed custom pin marker (when user holds finger on a point on the map)
            if (selectedPinLat != null && selectedPinLng != null) {
                val selectedMarker = Marker(view).apply {
                    position = GeoPoint(selectedPinLat, selectedPinLng)
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                    icon = selectedPinIcon
                    infoWindow = null
                }
                view.overlays.add(selectedMarker)
            }

            // User location blue dot marker
            val userMarker = Marker(view).apply {
                position = userPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                icon = userDotIcon
                infoWindow = null
            }
            view.overlays.add(userMarker)
            view.invalidate()
        },
        modifier = modifier
    )
}

private fun createSelectedPinDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (48f * density).toInt().coerceAtLeast(96)
    val heightPx = (60f * density).toInt().coerceAtLeast(120)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val rawPinPath = PathParser.createPathFromPathData(
        "M19,0 C8.5,0 0,8.5 0,19 C0,33.4 19,53 19,53 C19,53 38,33.4 38,19 C38,8.5 29.5,0 19,0 Z"
    )
    val pinScale = (36f * density) / 38f
    val offsetX = (widthPx - 38f * pinScale) / 2f
    val offsetY = 3f * density
    val pinMatrix = Matrix().apply {
        postScale(pinScale, pinScale)
        postTranslate(offsetX, offsetY)
    }
    val pinPath = Path(rawPinPath).apply { transform(pinMatrix) }

    val tipX = widthPx / 2f
    val tipY = offsetY + 53f * pinScale

    val groundShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            tipX,
            tipY,
            8f * density,
            intArrayOf(AndroidColor.parseColor("#44141C30"), AndroidColor.TRANSPARENT),
            floatArrayOf(0.15f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(tipX, tipY, 8f * density, groundShadowPaint)

    val whiteOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4.5f * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(pinPath, whiteOutlinePaint)

    // Warm Amber fill (#E89918) to distinguish the candidate long-pressed pin
    val pinFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#E89918")
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, pinFillPaint)

    // Plus / target center icon inside the pin bulb
    val bulbCenterY = offsetY + 19f * pinScale
    val plusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2.8f * density
        strokeCap = Paint.Cap.ROUND
    }
    val arm = 5.5f * density
    canvas.drawLine(tipX - arm, bulbCenterY, tipX + arm, bulbCenterY, plusPaint)
    canvas.drawLine(tipX, bulbCenterY - arm, tipX, bulbCenterY + arm, plusPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createCarPinDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val widthPx = (48f * density).toInt().coerceAtLeast(96)
    val heightPx = (60f * density).toInt().coerceAtLeast(120)
    val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Smooth cubic Bezier teardrop path from the Figma design (viewBox 0 0 38 53)
    val rawPinPath = PathParser.createPathFromPathData(
        "M19,0 C8.5,0 0,8.5 0,19 C0,33.4 19,53 19,53 C19,53 38,33.4 38,19 C38,8.5 29.5,0 19,0 Z"
    )
    val pinScale = (36f * density) / 38f
    val offsetX = (widthPx - 38f * pinScale) / 2f
    val offsetY = 3f * density
    val pinMatrix = Matrix().apply {
        postScale(pinScale, pinScale)
        postTranslate(offsetX, offsetY)
    }
    val pinPath = Path(rawPinPath).apply { transform(pinMatrix) }

    val tipX = widthPx / 2f
    val tipY = offsetY + 53f * pinScale

    // Subtle ground shadow under the pin tip
    val groundShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            tipX,
            tipY,
            8f * density,
            intArrayOf(AndroidColor.parseColor("#44141C30"), AndroidColor.TRANSPARENT),
            floatArrayOf(0.15f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(tipX, tipY, 8f * density, groundShadowPaint)

    // Outer white border stroke following the exact Bezier curve
    val whiteOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4.5f * density
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    canvas.drawPath(pinPath, whiteOutlinePaint)

    // Vibrant blue fill (#3B6CF6)
    val pinFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#3B6CF6")
        style = Paint.Style.FILL
    }
    canvas.drawPath(pinPath, pinFillPaint)

    // Crisp Material DirectionsCar vector icon centered in the upper bulb of the pin (viewBox 0 0 24 24)
    val rawCarPath = PathParser.createPathFromPathData(
        "M18.92,6.01C18.72,5.42 18.16,5 17.5,5h-11c-0.66,0 -1.21,0.42 -1.42,1.01L3,12v8c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-1h12v1c0,0.55 0.45,1 1,1h1c0.55,0 1,-0.45 1,-1v-8l-2.08,-5.99zM6.5,16c-0.83,0 -1.5,-0.67 -1.5,-1.5S5.67,13 6.5,13s1.5,0.67 1.5,1.5S7.33,16 6.5,16zM17.5,16c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5zM5,11l1.5,-4.5h11L19,11H5z"
    )
    val carSizePx = 18f * density
    val carScale = carSizePx / 24f
    val bulbCenterY = offsetY + 19f * pinScale
    val carMatrix = Matrix().apply {
        postScale(carScale, carScale)
        postTranslate(tipX - carSizePx / 2f, bulbCenterY - carSizePx / 2f)
    }
    val carPath = Path(rawCarPath).apply { transform(carMatrix) }

    val carPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawPath(carPath, carPaint)

    return BitmapDrawable(context.resources, bitmap)
}

private fun createUserDotDrawable(context: Context): BitmapDrawable {
    val density = context.resources.displayMetrics.density
    val sizePx = (36f * density).toInt().coerceAtLeast(72)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val cx = sizePx / 2f
    val cy = sizePx / 2f

    // Soft translucent pulse halo
    val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = RadialGradient(
            cx,
            cy,
            17f * density,
            intArrayOf(
                AndroidColor.parseColor("#423B6CF6"),
                AndroidColor.parseColor("#183B6CF6"),
                AndroidColor.TRANSPARENT
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawCircle(cx, cy, 17f * density, haloPaint)

    // Crisp white outer ring
    val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 8.5f * density, ringPaint)

    // Primary blue center dot
    val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.parseColor("#3B6CF6")
        style = Paint.Style.FILL
    }
    canvas.drawCircle(cx, cy, 6f * density, dotPaint)

    return BitmapDrawable(context.resources, bitmap)
}
