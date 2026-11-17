package com.parkmate.app.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ParkMateShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp), // --button-radius: 16px
    large = RoundedCornerShape(20.dp),  // --card-radius: 20px
    extraLarge = RoundedCornerShape(24.dp) // bottom-sheet top radius: 24px
)
