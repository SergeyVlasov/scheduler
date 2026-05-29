package com.example.schedule.effects

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.random.Random


@Composable
fun SnowEffect(
    modifier: Modifier = Modifier,
    snowCount: Int = 120
) {
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {

        val width = with(density) { maxWidth.toPx() }
        val height = with(density) { maxHeight.toPx() }

        // ⚠️ ключ: состояние кадра
        var frame by remember { mutableStateOf(0L) }

        // снежинки (immutable!)
        var snowflakes by remember {
            mutableStateOf(
                generateSnowflakes(snowCount, width.toInt(), height.toInt())
            )
        }

        LaunchedEffect(Unit) {
            while (true) {

                frame++

                snowflakes = snowflakes.map { flake ->
                    val newY = flake.y + flake.speed
                    val reset = newY > height

                    flake.copy(
                        x = if (reset) (0..width.toInt()).random().toFloat() else flake.x,
                        y = if (reset) 0f else newY
                    )
                }

                delay(16L)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {

            // ⚠️ важно: привязка к frame
            // чтобы Compose точно перерисовывал Canvas
            frame

            snowflakes.forEach { flake ->
                drawCircle(
                    color = Color.White.copy(alpha = flake.alpha),
                    radius = flake.size,
                    center = Offset(flake.x, flake.y)
                )
            }
        }
    }
}