package com.example.schedule.data.model

data class EmployeeShiftsResponse(
    val employee_id: Int,
    val first_name: String,
    val last_name: String,
    val middle_name: String,
    val work_shifts: List<ShiftDto>
)