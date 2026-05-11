package com.example.schedule

object ApiConfig {

    const val BASE_URL = "http://157.22.201.243:8000"

    const val LOGIN_PATH = "/api/login"
    const val SUBDIVISION_PATH = "/api/subdivision"

    const val LOGIN_URL = "$BASE_URL$LOGIN_PATH"

    const val CURRENT_MONTH_URL = "/api/%d/%d"

    fun currentMonthUrl(year: Int, month: Int): String {
        return "$BASE_URL/".trimEnd('/') + CURRENT_MONTH_URL.format(year, month)
    }

    fun subdivisionUrl(year: Int, month: Int): String {
        return "$BASE_URL$SUBDIVISION_PATH/$year/$month"
    }
}
