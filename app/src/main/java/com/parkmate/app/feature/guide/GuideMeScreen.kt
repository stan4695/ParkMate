package com.parkmate.app.feature.guide

import android.content.Intent
import android.content.res.Configuration
import androidx.core.net.toUri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.parkmate.app.R
import com.parkmate.app.core.ui.components.ParkMatePrimaryButton
import com.parkmate.app.core.ui.components.ParkMateTopBar
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private val ArrivedGreen = Color(0xFF22C55E)

@Composable
fun GuideMeScreen(
    spotId: Long,
    viewModel: GuideMeViewModel,
    onBack: () -> Unit
) {
    LaunchedEffect(spotId) {
        viewModel.startGuidance(spotId)
    }

    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = ParkMateThemeTokens.colors
    val context = LocalContext.current

    val isArrived = state.distanceMeters <= 15

    val animatedRotation by animateFloatAsState(
        targetValue = state.relativeArrowRotationDegrees,
        animationSpec = tween(durationMillis = 260),
        label = "compassArrowRotation"
    )

    val openGoogleMaps: () -> Unit = {
        val lat = state.spot?.latitude ?: 44.4412
        val lng = state.spot?.longitude ?: 26.0979
        val gmmIntentUri = "google.navigation:q=$lat,$lng&mode=w".toUri()
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
            setPackage("com.google.android.apps.maps")
        }
        runCatching {
            context.startActivity(mapIntent)
        }.onFailure {
            val browserUri =
                "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=walking".toUri()
            context.startActivity(Intent(Intent.ACTION_VIEW, browserUri))
        }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        ParkMateTopBar(
            title = stringResource(R.string.guide_title),
            eyebrow = stringResource(R.string.guide_eyebrow),
            onBack = onBack
        )

        if (isLandscape) {
            // Landscape two-column adaptive layout
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    CompassOrArrivedBadge(
                        isArrived = isArrived,
                        animatedRotation = animatedRotation,
                        dialSize = 168.dp
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    GuideDetailsContent(
                        state = state,
                        isArrived = isArrived
                    )

                    AnimatedVisibility(
                        visible = !isArrived,
                        enter = fadeIn(tween(240)) + expandVertically(tween(240)),
                        exit = fadeOut(tween(200)) + shrinkVertically(tween(240))
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            ParkMatePrimaryButton(
                                text = stringResource(R.string.guide_open_google_maps),
                                icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                                onClick = openGoogleMaps
                            )
                        }
                    }
                }
            }
        } else {
            // Portrait layout with scroll safety
            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .align(Alignment.CenterHorizontally)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CompassOrArrivedBadge(
                    isArrived = isArrived,
                    animatedRotation = animatedRotation,
                    dialSize = 220.dp
                )

                Spacer(modifier = Modifier.height(28.dp))

                GuideDetailsContent(
                    state = state,
                    isArrived = isArrived
                )
            }

            // Open in Google Maps Button (hidden once arrived next to the car)
            AnimatedVisibility(
                visible = !isArrived,
                enter = fadeIn(tween(240)) + expandVertically(tween(240)),
                exit = fadeOut(tween(200)) + shrinkVertically(tween(240))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ParkMatePrimaryButton(
                        text = stringResource(R.string.guide_open_google_maps),
                        icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                        onClick = openGoogleMaps,
                        modifier = Modifier.widthIn(max = 520.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CompassOrArrivedBadge(
    isArrived: Boolean,
    animatedRotation: Float,
    dialSize: Dp
) {
    val colors = ParkMateThemeTokens.colors
    val innerSize = dialSize * 0.53f
    val iconSize = dialSize * 0.27f

    AnimatedContent(
        targetState = isArrived,
        transitionSpec = {
            (scaleIn(initialScale = 0.8f, animationSpec = tween(320)) + fadeIn(tween(280)))
                .togetherWith(scaleOut(targetScale = 0.8f, animationSpec = tween(220)) + fadeOut(tween(200)))
        },
        label = "compassOrArrivedCheck"
    ) { arrived ->
        if (arrived) {
            Box(
                modifier = Modifier.size(dialSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        color = colors.statusGreenBg,
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = ArrivedGreen.copy(alpha = 0.22f),
                        radius = radius * 0.78f,
                        center = center
                    )
                    drawCircle(
                        color = ArrivedGreen.copy(alpha = 0.38f),
                        radius = radius - 10.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape)
                        .background(ArrivedGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        } else {
            Box(
                modifier = Modifier.size(dialSize),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val radius = size.minDimension / 2f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        color = colors.blueSoft,
                        radius = radius,
                        center = center
                    )
                    drawCircle(
                        color = colors.blue.copy(alpha = 0.22f),
                        radius = radius - 10.dp.toPx(),
                        center = center,
                        style = Stroke(width = 2.dp.toPx())
                    )

                    for (deg in 0 until 360 step 15) {
                        val rad = Math.toRadians(deg.toDouble())
                        val isCardinal = deg % 90 == 0
                        val outer = radius - 16.dp.toPx()
                        val inner = outer - if (isCardinal) 14.dp.toPx() else 7.dp.toPx()
                        val start = Offset(
                            center.x + (inner * cos(rad)).toFloat(),
                            center.y + (inner * sin(rad)).toFloat()
                        )
                        val end = Offset(
                            center.x + (outer * cos(rad)).toFloat(),
                            center.y + (outer * sin(rad)).toFloat()
                        )
                        drawLine(
                            color = if (isCardinal) colors.blue else colors.muted.copy(alpha = 0.45f),
                            start = start,
                            end = end,
                            strokeWidth = if (isCardinal) 3.dp.toPx() else 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(CircleShape)
                        .background(colors.blue)
                        .rotate(animatedRotation),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Navigation,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideDetailsContent(
    state: GuideMeUiState,
    isArrived: Boolean
) {
    val colors = ParkMateThemeTokens.colors

    Text(
        text = if (isArrived) {
            stringResource(R.string.guide_arrived)
        } else if (state.distanceMeters < 1000) {
            stringResource(R.string.home_distance_away_meters, state.distanceMeters)
        } else {
            stringResource(R.string.home_distance_away_km, state.distanceMeters / 1000f)
        },
        fontSize = 26.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        color = if (isArrived) colors.statusGreenText else colors.text,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    )

    Spacer(modifier = Modifier.height(10.dp))

    Text(
        text = if (isArrived) {
            stringResource(R.string.guide_arrived_subtitle)
        } else {
            stringResource(R.string.home_about_walk_minutes, state.walkMinutes)
        },
        style = MaterialTheme.typography.titleMedium,
        lineHeight = 22.sp,
        color = colors.muted,
        textAlign = TextAlign.Center
    )

    if (!isArrived) {
        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.blueSoft
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Explore,
                    contentDescription = null,
                    tint = colors.blue,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(
                        R.string.guide_bearing_label,
                        state.targetBearingDegrees.roundToInt(),
                        state.cardinalDirection
                    ),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.blue
                )
            }
        }

        if (!state.isCompassSensorAvailable) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.guide_sensor_unavailable),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
                textAlign = TextAlign.Center
            )
        }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Target Car Summary Card
    val spot = state.spot
    if (spot != null) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isArrived) colors.statusGreenBg else colors.blueSoft),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.LocationOn,
                        contentDescription = null,
                        tint = if (isArrived) ArrivedGreen else colors.blue
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spot.addressLine,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text
                    )
                    if (spot.note.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = spot.note,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.muted
                        )
                    }
                }
            }
        }
    }
}
