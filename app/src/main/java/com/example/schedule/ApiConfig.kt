package com.example.schedule

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object ApiConfig {

    //const val BASE_URL = "http://157.22.201.243:8000"
    const val BASE_URL = "http://10.0.2.2:8000"

    const val LOGIN_PATH = "/api/login"
    const val SUBDIVISION_PATH = "/api/subdivision"

    const val LOGIN_URL = "$BASE_URL$LOGIN_PATH"

    const val CURRENT_MONTH_URL = "/api/%d/%d"
    const val DAY_DETAIL_URL = "/api/%d/%d/%d"

    const val UPLOAD_DIOP_PATH = "/api/upload-xls-DIOP"
    const val UPLOAD_SOPR_PATH = "/api/upload-xls-SOPR"

    fun currentMonthUrl(year: Int, month: Int): String {
        return "$BASE_URL/".trimEnd('/') + CURRENT_MONTH_URL.format(year, month)
    }

    fun dayDetailUrl(year: Int, month: Int, day: Int): String {
        return "$BASE_URL/".trimEnd('/') + DAY_DETAIL_URL.format(year, month, day)
    }

    fun subdivisionUrl(year: Int, month: Int): String {
        return "$BASE_URL$SUBDIVISION_PATH/$year/$month"
    }

    fun uploadDiopUrl(): String = "$BASE_URL$UPLOAD_DIOP_PATH"

    fun uploadSoprUrl(): String = "$BASE_URL$UPLOAD_SOPR_PATH"

    fun deletePushTokenUrl(token: String): String {
        val encodedToken = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
        return "$BASE_URL/api/delete_push_token/$encodedToken"
    }
}
