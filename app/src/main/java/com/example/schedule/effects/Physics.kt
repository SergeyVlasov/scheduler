package com.example.schedule.effects

import kotlin.random.Random

fun updateSnow(
    snowflakes: List<Snowflake>,
    width: Float,
    height: Float,
    dt: Float
) {
    for (f in snowflakes) {

        // 🌨️ gravity
        f.y += f.speed * 60f * dt

        // 🌬️ wind (slight randomness)
        f.x += kotlin.math.sin(f.y * 0.01f) * 0.3f

        // reset
        if (f.y > height) {
            f.y = -f.size
            f.x = Random.nextFloat() * width
        }
    }
}