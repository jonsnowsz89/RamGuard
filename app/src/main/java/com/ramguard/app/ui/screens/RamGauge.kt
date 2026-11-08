package com.ramguard.app.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ramguard.app.R
import com.ramguard.app.data.MemInfo
import kotlin.math.roundToInt

val RamGreen = Color(0xFF2BE38A)
val RamAmber = Color(0xFFFFC857)
val RamRed = Color(0xFFFF6B6B)

fun ramColor(fraction: Float) = when {
    fraction < 0.60f -> RamGreen
    fraction < 0.80f -> RamAmber
    else -> RamRed
}

/**
 * حلقة حالة الرام: the arc fills clockwise from the top according to used RAM, animates smoothly
 * (e.g. drops after Quick Clean) and turns green / amber / red with usage.
 * [compact] = small ring with only the percentage (used in headers).
 */
@Composable
fun RamGauge(
    mem: MemInfo,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    stroke: Dp = 18.dp,
    compact: Boolean = false
) {
    val fraction by animateFloatAsState(mem.usedFraction, tween(900), label = "ramFraction")
    val color by animateColorAsState(ramColor(fraction), tween(600), label = "ramColor")
    val track = MaterialTheme.colorScheme.outline

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(stroke / 2)) {
            val s = Stroke(width = stroke.toPx(), cap = StrokeCap.Round)
            drawArc(color = track, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = s)
            if (fraction > 0f) {
                drawArc(color = color, startAngle = -90f, sweepAngle = 360f * fraction, useCenter = false, style = s)
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "${(fraction * 100).roundToInt()}%",
                fontSize = if (compact) 14.sp else 46.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (!compact) {
                Text(stringResource(R.string.ram_used), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
