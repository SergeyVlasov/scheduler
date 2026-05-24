package com.example.schedule.effects

import java.time.LocalDate

fun isSnowSeason(): Boolean {
    val now = LocalDate.now()
    val month = now.monthValue
    val day = now.dayOfMonth
    val isJanuarySnow = month == 1 && day in 1..15
    val isDecemberSnow = month == 12 && day in 15..31
    return isJanuarySnow || isDecemberSnow
}