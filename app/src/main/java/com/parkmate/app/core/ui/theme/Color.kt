package com.parkmate.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Light design tokens extracted from index.css
val ParkBlue = Color(0xFF3B6CF6)
val ParkBlueSoft = Color(0xFFEDF2FF)
val ParkBgLight = Color(0xFFF7F8FA)
val ParkSurfaceLight = Color(0xFFFFFFFF)
val ParkTextLight = Color(0xFF1C1F26)
val ParkMutedLight = Color(0xFF777D89)
val ParkLineLight = Color(0xFFE9EBEF)
val ParkAmber = Color(0xFFE89918)
val ParkAmberSoft = Color(0xFFFFF6E7)
val ParkAmberBorder = Color(0xFFF6DFB9)
val ParkAmberTrack = Color(0xFFF5DFBD)
val ParkAmberText = Color(0xFF876538)
val ParkStatusGreenBg = Color(0xFFEAF8F0)
val ParkStatusGreenText = Color(0xFF258457)
val ParkDanger = Color(0xFFC74444)
val ParkDangerSoftLight = Color(0xFFFFEFEF)
val ParkImmersiveDark = Color(0xFF090A0C)

// Derived Dark Theme tokens
val ParkBlueDark = Color(0xFF6B93FF)
val ParkBlueSoftDark = Color(0xFF1E2B4F)
val ParkBgDark = Color(0xFF0F1218)
val ParkSurfaceDark = Color(0xFF181C24)
val ParkTextDark = Color(0xFFF1F3F7)
val ParkMutedDark = Color(0xFF9BA1AE)
val ParkLineDark = Color(0xFF262C38)
val ParkAmberDark = Color(0xFFF3AD35)
val ParkAmberSoftDark = Color(0xFF2B2214)
val ParkAmberBorderDark = Color(0xFF4C391D)
val ParkAmberTrackDark = Color(0xFF3E301B)
val ParkAmberTextDark = Color(0xFFE5C088)
val ParkStatusGreenBgDark = Color(0xFF143325)
val ParkStatusGreenTextDark = Color(0xFF56CC8D)
val ParkDangerSoftDark = Color(0xFF2E1619)

@Immutable
data class ParkMateExtendedColors(
    val blue: Color,
    val blueSoft: Color,
    val bg: Color,
    val surface: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val amber: Color,
    val amberSoft: Color,
    val amberBorder: Color,
    val amberTrack: Color,
    val amberText: Color,
    val statusGreenBg: Color,
    val statusGreenText: Color,
    val danger: Color,
    val dangerSoft: Color
)

val LightExtendedColors = ParkMateExtendedColors(
    blue = ParkBlue,
    blueSoft = ParkBlueSoft,
    bg = ParkBgLight,
    surface = ParkSurfaceLight,
    text = ParkTextLight,
    muted = ParkMutedLight,
    line = ParkLineLight,
    amber = ParkAmber,
    amberSoft = ParkAmberSoft,
    amberBorder = ParkAmberBorder,
    amberTrack = ParkAmberTrack,
    amberText = ParkAmberText,
    statusGreenBg = ParkStatusGreenBg,
    statusGreenText = ParkStatusGreenText,
    danger = ParkDanger,
    dangerSoft = ParkDangerSoftLight
)

val DarkExtendedColors = ParkMateExtendedColors(
    blue = ParkBlueDark,
    blueSoft = ParkBlueSoftDark,
    bg = ParkBgDark,
    surface = ParkSurfaceDark,
    text = ParkTextDark,
    muted = ParkMutedDark,
    line = ParkLineDark,
    amber = ParkAmberDark,
    amberSoft = ParkAmberSoftDark,
    amberBorder = ParkAmberBorderDark,
    amberTrack = ParkAmberTrackDark,
    amberText = ParkAmberTextDark,
    statusGreenBg = ParkStatusGreenBgDark,
    statusGreenText = ParkStatusGreenTextDark,
    danger = Color(0xFFEF6B6B),
    dangerSoft = ParkDangerSoftDark
)

val LocalParkMateColors = staticCompositionLocalOf { LightExtendedColors }
