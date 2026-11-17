package com.parkmate.app.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.parkmate.app.R
import com.parkmate.app.core.ui.theme.ParkMateThemeTokens
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.tan

@Composable
fun ParkMateTopBar(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    onBack: (() -> Unit)? = null,
    action: (@Composable () -> Unit)? = null
) {
    val colors = ParkMateThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBack != null) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(48.dp)
                        .padding(end = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_go_back),
                        tint = colors.text
                    )
                }
            }
            Column {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.muted
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = colors.text,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (action != null) {
            action()
        }
    }
}

@Composable
fun ParkMatePrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val colors = ParkMateThemeTokens.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.blue,
            contentColor = Color.White
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 0.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = colors.blue.copy(alpha = 0.25f),
                spotColor = colors.blue.copy(alpha = 0.25f)
            )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun ParkMateSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ParkMateThemeTokens.colors
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, colors.line),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.text
        ),
        modifier = modifier.heightIn(min = 50.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun ParkMateOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val colors = ParkMateThemeTokens.colors
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(13.dp),
        border = BorderStroke(1.dp, colors.blue.copy(alpha = 0.35f)),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.surface,
            contentColor = colors.blue
        ),
        modifier = modifier.heightIn(min = 48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ParkMateTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ParkMateThemeTokens.colors
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = colors.blue
        )
    }
}

@Composable
fun ParkMateSettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    checked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = ParkMateThemeTokens.colors
    val rowModifier = if (onClick != null) {
        modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp)
    } else {
        modifier
            .fillMaxWidth()
            .heightIn(min = 68.dp)
            .padding(vertical = 8.dp)
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.blueSoft),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.blue,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(11.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.text
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        if (checked != null && onCheckedChange != null) {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colors.blue,
                    uncheckedThumbColor = Color.White,
                    uncheckedTrackColor = colors.line,
                    uncheckedBorderColor = Color.Transparent
                )
            )
        } else if (trailing != null) {
            trailing()
        }
    }
}

@Composable
fun StatusPillBadge(
    text: String,
    modifier: Modifier = Modifier,
    isWarning: Boolean = false
) {
    val colors = ParkMateThemeTokens.colors
    val bgColor = if (isWarning) colors.amberSoft else colors.statusGreenBg
    val textColor = if (isWarning) colors.amberText else colors.statusGreenText
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 9.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Renders a real Google Maps tile thumbnail centered on the given [latitude] and [longitude],
 * with a crisp location pin overlay and vector fallback while loading/offline.
 */
@Composable
fun RealMapThumbnail(
    latitude: Double,
    longitude: Double,
    modifier: Modifier = Modifier,
    zoom: Int = 16
) {
    val colors = ParkMateThemeTokens.colors
    val n = 2.0.pow(zoom.toDouble())
    val safeLat = latitude.coerceIn(-85.0, 85.0)
    val safeLng = longitude.coerceIn(-180.0, 180.0)
    val xExact = (safeLng + 180.0) / 360.0 * n
    val latRad = Math.toRadians(safeLat)
    val yExact = (1.0 - ln(tan(latRad) + 1.0 / cos(latRad)) / Math.PI) / 2.0 * n

    val tileX = floor(xExact).toInt().coerceIn(0, n.toInt() - 1)
    val tileY = floor(yExact).toInt().coerceIn(0, n.toInt() - 1)
    val fracX = (xExact - tileX).toFloat().coerceIn(0.15f, 0.85f)
    val fracY = (yExact - tileY).toFloat().coerceIn(0.15f, 0.85f)

    val tileUrl = "https://mt1.google.com/vt/lyrs=m&x=$tileX&y=$tileY&z=$zoom&scale=2"

    Box(
        modifier = modifier
            .background(Color(0xFFEEF2F6)),
        contentAlignment = Alignment.Center
    ) {
        // Fallback vector background underneath in case device is offline
        MiniMapThumbnail(
            modifier = Modifier.fillMaxSize(),
            showPin = false
        )

        // Real Google Maps Retina tile shifted so (latitude, longitude) is centered
        AsyncImage(
            model = tileUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val zoomScale = 1.65f
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = (0.5f - fracX) * size.width * zoomScale * 0.65f
                    translationY = (0.5f - fracY) * size.height * zoomScale * 0.65f
                }
        )

        // Crisp Car Pin badge in the center
        Canvas(modifier = Modifier.size(28.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(
                color = colors.blue.copy(alpha = 0.25f),
                radius = size.minDimension * 0.48f,
                center = center
            )
            drawCircle(
                color = Color.White,
                radius = size.minDimension * 0.28f,
                center = center
            )
            drawCircle(
                color = colors.blue,
                radius = size.minDimension * 0.20f,
                center = center
            )
        }
    }
}

/**
 * Stylized vector map artwork for mini-map thumbnails and background previews.
 */
@Composable
fun MiniMapThumbnail(
    modifier: Modifier = Modifier,
    seedOffset: Int = 0,
    showPin: Boolean = true
) {
    val colors = ParkMateThemeTokens.colors
    Canvas(
        modifier = modifier
            .background(Color(0xFFEEF2F6))
    ) {
        val w = size.width
        val h = size.height
        val shiftX = (seedOffset % 3 - 1) * (w * 0.07f)

        // Subtle green park zone
        drawRoundRect(
            color = Color(0xFFDDF0E4),
            topLeft = Offset(w * 0.06f + shiftX, h * 0.08f),
            size = Size(w * 0.34f, h * 0.28f),
            cornerRadius = CornerRadius(12f, 12f)
        )

        // Building blocks with subtle depth
        val blockShadow = Color(0xFFD5DBE3)
        val blockColor = Color(0xFFE3E7ED)
        val blocks = listOf(
            Pair(Offset(w * 0.56f + shiftX, h * 0.12f), Size(w * 0.32f, h * 0.26f)),
            Pair(Offset(w * 0.12f + shiftX, h * 0.54f), Size(w * 0.30f, h * 0.28f)),
            Pair(Offset(w * 0.58f + shiftX, h * 0.58f), Size(w * 0.30f, h * 0.26f))
        )
        blocks.forEach { (topLeft, blockSize) ->
            drawRoundRect(
                color = blockShadow,
                topLeft = Offset(topLeft.x + 2f, topLeft.y + 3f),
                size = blockSize,
                cornerRadius = CornerRadius(10f, 10f)
            )
            drawRoundRect(
                color = blockColor,
                topLeft = topLeft,
                size = blockSize,
                cornerRadius = CornerRadius(10f, 10f)
            )
        }

        // Road casings & avenues
        val casingStroke = Stroke(width = w * 0.14f, cap = StrokeCap.Round)
        val roadStroke = Stroke(width = w * 0.11f, cap = StrokeCap.Round)

        val roadPath1 = Path().apply {
            moveTo(0f, h * 0.45f)
            cubicTo(w * 0.35f, h * 0.38f, w * 0.65f, h * 0.55f, w, h * 0.42f)
        }
        drawPath(path = roadPath1, color = Color(0xFFD8DEE6), style = casingStroke)
        drawPath(path = roadPath1, color = Color.White, style = roadStroke)

        val roadPath2 = Path().apply {
            moveTo(w * 0.47f + shiftX, 0f)
            cubicTo(w * 0.52f, h * 0.35f, w * 0.41f, h * 0.65f, w * 0.49f, h)
        }
        drawPath(path = roadPath2, color = Color(0xFFD8DEE6), style = casingStroke)
        drawPath(path = roadPath2, color = Color.White, style = roadStroke)

        if (showPin) {
            val pinCenter = Offset(w * 0.50f + shiftX * 0.5f, h * 0.46f)
            drawCircle(
                color = colors.blue.copy(alpha = 0.22f),
                radius = w * 0.16f,
                center = pinCenter
            )
            drawCircle(
                color = Color.White,
                radius = w * 0.085f,
                center = pinCenter
            )
            drawCircle(
                color = colors.blue,
                radius = w * 0.06f,
                center = pinCenter
            )
        }
    }
}
