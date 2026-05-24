package com.example.schedule.effects

import kotlin.random.Random

fun generateSnowflakes(
    count: Int,
    width: Int,
    height: Int
): List<Snowflake> {
    return List(count) {
        Snowflake(
            x = Random.nextFloat() * width,
            y = Random.nextFloat() * height,
            size = Random.nextInt(3, 10).toFloat(),
            speed = Random.nextFloat() * 1.5f + 0.5f,
            alpha = Random.nextFloat() * 0.6f + 0.3f
        )
    }
}