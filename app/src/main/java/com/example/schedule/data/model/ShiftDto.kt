package com.example.schedule.data.model

data class ShiftDto(
    val id: Int? = null,
    val employee_id: Int? = null,
    val shift_date: Int,
    val shift_type: String,
    val year: Int? = null,
    val month: Int? = null
)