package com.parkmate.app.feature.details

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.parkmate.app.R
import com.parkmate.app.core.math.TimerProgressState
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val TimerGreen = Color(0xFF22C55E)
private val TimerAmber = Color(0xFFE89918)
private val TimerRed = Color(0xFFE53935)

/**
 * Computes a smooth color transition from Green (1.0f, just parked) -> Amber (0.5f) -> Red (0.0f, expiring).
 */
private fun computeTimerColor(fraction: Float, hasStarted: Boolean, isExpired: Boolean): Color {
    if (!hasStarted) return TimerAmber
    if (isExpired) return TimerRed
    val clamped = fraction.coerceIn(0f, 1f)
    return if (clamped >= 0.5f) {
        val t = (clamped - 0.5f) / 0.5f
        lerp(TimerAmber, TimerGreen, t)
    } else {
        val t = clamped / 0.5f
        lerp(TimerRed, TimerAmber, t)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PaidParkingTimerCard(
    timerState: TimerProgressState,
    onAdd15Minutes: () -> Unit,
    onAdd30Minutes: () -> Unit,
    onResetTimer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ParkMateThemeTokens.colors
    val context = LocalContext.current

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val ensureNotifPermissionThen = { action: () -> Unit ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        action()
    }

    val hasTimerStarted = timerState.expiryEpochMillis != null
    val rawFraction = if (hasTimerStarted && !timerState.isExpired) {
        timerState.progressFraction.coerceIn(0f, 1f)
    } else {
        0f
    }

    val animatedFraction by animateFloatAsState(
        targetValue = rawFraction,
        animationSpec = tween(durationMillis = 450),
        label = "timerRingFraction"
    )

    val targetRingColor = computeTimerColor(
        fraction = rawFraction,
        hasStarted = hasTimerStarted,
        isExpired = timerState.isExpired
    )
    val animatedRingColor by animateColorAsState(
        targetValue = targetRingColor,
        animationSpec = tween(durationMillis = 450),
        label = "timerRingColor"
    )

    // Uses dark-mode-aware semantic surfaces so text & ring always have crisp contrast
    val cardBgColor by animateColorAsState(
        targetValue = when {
            !hasTimerStarted -> colors.amberSoft
            timerState.isExpired || rawFraction < 0.25f -> colors.dangerSoft
            rawFraction >= 0.6f -> colors.statusGreenBg
            else -> colors.amberSoft
        },
        animationSpec = tween(durationMillis = 450),
        label = "timerCardBg"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = animatedRingColor.copy(alpha = 0.42f),
        animationSpec = tween(durationMillis = 450),
        label = "timerCardBorder"
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = cardBgColor,
        border = BorderStroke(1.dp, cardBorderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp)
        ) {
            // Circular Progress Ring (empty when 0 min left, transitioning Green -> Amber -> Red when active)
            Box(
                modifier = Modifier.size(94.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(6.dp)) {
                    val strokeWidth = 8.dp.toPx()
                    drawArc(
                        color = animatedRingColor.copy(alpha = 0.22f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                    if (animatedFraction > 0.001f) {
                        drawArc(
                            color = animatedRingColor,
                            startAngle = -90f,
                            sweepAngle = 360f * animatedFraction,
                            useCenter = false,
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = timerState.remainingMinutes.toString(),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.text
                    )
                    Text(
                        text = stringResource(R.string.timer_min_left),
                        fontSize = 9.sp,
                        color = colors.muted
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.timer_paid_parking),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.text
                    )

                    // Status label
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = animatedRingColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val badgeText = when {
                            !hasTimerStarted ->
                                stringResource(R.string.timer_not_started_badge)
                            timerState.isExpired ->
                                stringResource(R.string.timer_expired_badge)
                            timerState.isAlmostExpired ->
                                stringResource(R.string.timer_almost_expired)
                            else ->
                                stringResource(R.string.timer_active_badge)
                        }
                        Text(
                            text = badgeText.uppercase(),
                            color = animatedRingColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                val expiryFormatted = timerState.expiryEpochMillis?.let { millis ->
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
                }
                Text(
                    text = if (expiryFormatted != null) {
                        stringResource(R.string.timer_expires_at, expiryFormatted)
                    } else {
                        stringResource(R.string.timer_not_set)
                    },
                    fontSize = 11.sp,
                    color = colors.muted
                )
                Spacer(modifier = Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    TimerQuickActionChip(
                        label = stringResource(R.string.timer_add_15),
                        accentColor = animatedRingColor,
                        onClick = { ensureNotifPermissionThen(onAdd15Minutes) }
                    )
                    TimerQuickActionChip(
                        label = stringResource(R.string.timer_add_30),
                        accentColor = animatedRingColor,
                        onClick = { ensureNotifPermissionThen(onAdd30Minutes) }
                    )
                    if (hasTimerStarted) {
                        TimerQuickActionChip(
                            label = stringResource(R.string.timer_stop),
                            accentColor = animatedRingColor,
                            onClick = onResetTimer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimerQuickActionChip(
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    val colors = ParkMateThemeTokens.colors
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.text
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.heightIn(min = 48.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
