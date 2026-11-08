package com.ramguard.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** AMOLED: true black background saves power on OLED panels. One UI style: big radii, soft surfaces. */
private val Amoled = darkColorScheme(
    primary = Color(0xFF3DDCFF),
    onPrimary = Color(0xFF00212B),
    primaryContainer = Color(0xFF0B3A47),
    onPrimaryContainer = Color(0xFFBDF1FF),
    secondary = Color(0xFF7CF2B0),
    background = Color.Black,
    onBackground = Color(0xFFEDEDED),
    surface = Color.Black,
    onSurface = Color(0xFFEDEDED),
    surfaceVariant = Color(0xFF151518),      // cards
    onSurfaceVariant = Color(0xFFB4B4BA),
    outline = Color(0xFF2A2A30),
    error = Color(0xFFFF6B6B)
)

private val OneUiShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun RamGuardTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = Amoled, shapes = OneUiShapes, content = content)
