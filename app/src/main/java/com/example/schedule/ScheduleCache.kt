package com.example.schedule

import android.content.Context
import java.io.File

/**
 * Локальное хранение сырых JSON-ответов API для работы без интернета.
 */
class ScheduleCache(context: Context) {

    private val dir: File =
        File(context.filesDir, CACHE_DIR).apply { mkdirs() }

    fun putMonth(year: Int, month: Int, body: String) {
        file("month_${year}_$month.json").writeText(body)
    }

    fun getMonth(year: Int, month: Int): String? =
        file("month_${year}_$month.json").readOrNull()

    fun putDayDetail(year: Int, month: Int, day: Int, body: String) {
        file("day_${year}_${month}_$day.json").writeText(body)
    }

    fun getDayDetail(year: Int, month: Int, day: Int): String? =
        file("day_${year}_${month}_$day.json").readOrNull()

    fun putSubdivision(year: Int, month: Int, body: String) {
        file("subdivision_${year}_$month.json").writeText(body)
    }

    fun getSubdivision(year: Int, month: Int): String? =
        file("subdivision_${year}_$month.json").readOrNull()

    private fun file(name: String): File = File(dir, name)

    private fun File.readOrNull(): String? =
        if (exists()) readText() else null

    companion object {
        private const val CACHE_DIR = "schedule_cache"
    }
}

data class ScheduleLoad<T>(
    val value: T,
    val fromCache: Boolean = false
)

const val OFFLINE_DATA_HINT =
    "Нет подключения к интернету. Показаны сохранённые данные."
