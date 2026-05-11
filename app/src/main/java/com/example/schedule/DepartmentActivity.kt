package com.example.schedule

import android.content.Intent
import android.os.Bundle
import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.schedule.data.model.EmployeeShiftsResponse
import com.example.schedule.data.model.ShiftDto
import com.example.schedule.ui.theme.ScheduleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.YearMonth
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.sp

class DepartmentActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val activity = this

        val token = getSharedPreferences(
            AuthActivity.AUTH_PREFS,
            Context.MODE_PRIVATE
        ).getString(AuthActivity.KEY_TOKEN, null) ?: ""

        enableEdgeToEdge()

        setContent {
            ScheduleTheme {

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {

                        NavigationBar {

                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    startActivity(Intent(activity, MainActivity::class.java))
                                    finish()
                                },
                                icon = { Icon(Icons.Filled.Home, null) },
                                label = { Text("График") }
                            )

                            NavigationBarItem(
                                selected = true,
                                onClick = { },
                                icon = { Icon(Icons.Filled.List, null) },
                                label = { Text("Отдел") }
                            )

                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    startActivity(Intent(activity, SettingsActivity::class.java))
                                    finish()
                                },
                                icon = { Icon(Icons.Filled.Settings, null) },
                                label = { Text("Настройки") }
                            )
                        }
                    }
                ) { padding ->

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                    ) {
                        DepartmentScreen(
                            token = token,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DepartmentScreen(
    token: String,
    modifier: Modifier = Modifier
) {
    var month by remember { mutableStateOf(YearMonth.now()) }
    var employees by remember { mutableStateOf<List<EmployeeShiftsResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val days = remember(month) {
        (1..month.lengthOfMonth()).toList()
    }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()

    LaunchedEffect(month) {
        loading = true
        error = null

        val result = loadSubdivisionShifts(
            year = month.year,
            month = month.monthValue,
            token = token
        )

        result
            .onSuccess { employees = it }
            .onFailure {
                employees = emptyList()
                error = it.message
            }

        loading = false
    }

    val shiftColors: (String?) -> Color = { type ->
        when (type) {
            "Д" -> Color(0xFFFFB3B3)
            "Н" -> Color(0xFFB3D4FF)
            "О" -> Color.Gray
            "Б" -> Color(0xFFFFA500)
            "8" -> Color(0xFF90EE90)
            else -> Color.LightGray
        }
    }

    val employeeShiftMaps = remember(employees) {
        employees.associate { emp ->
            emp.employee_id to emp.work_shifts.associate {
                val day = it.shift_date % 100   // 👈 берём день
                day to it
            }
        }
    }

    Column(modifier = modifier.padding(12.dp)) {

        // HEADER
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }

            Text(
                text = "${month.month.getDisplayName(
                    java.time.format.TextStyle.FULL,
                    java.util.Locale("ru")
                )} ${month.year}",
                style = MaterialTheme.typography.titleLarge
            )

            IconButton(onClick = { month = month.plusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
            }
        }

        if (loading) Text("Загрузка...")
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Spacer(Modifier.height(8.dp))

        val cellSize = 32.dp
        val rowHeight = cellSize

        // TABLE
        Row(modifier = Modifier.fillMaxSize()) {

            // LEFT FIXED COLUMN
            Column(
                modifier = Modifier
                    .width(140.dp)
                    .verticalScroll(vScroll)
            ) {

                Spacer(Modifier.height(rowHeight))

                employees.forEach { emp ->
                    Text(
                        text = emp.last_name,
                        modifier = Modifier
                            .height(rowHeight)
                            .padding(vertical = 8.dp)
                    )
                }
            }

            // RIGHT GRID
            Box(
                modifier = Modifier
                    .horizontalScroll(hScroll)
                    .verticalScroll(vScroll)
            ) {

                Column {

                    // DAYS HEADER
                    Row {
                        days.forEach { day ->
                            Box(
                                modifier = Modifier
                                    .width(cellSize)
                                    .height(rowHeight)
                                    .border(1.dp, Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(day.toString(), fontSize = 12.sp)
                            }
                        }
                    }

                    // EMPLOYEES ROWS
                    employees.forEach { emp ->

                        val shiftMap = employeeShiftMaps[emp.employee_id] ?: emptyMap()

                        Row {
                            days.forEach { day ->

                                val shift = shiftMap[day]

                                Box(
                                    modifier = Modifier
                                        .size(cellSize)
                                        .border(1.dp, Color.Black)
                                        .background(shiftColors(shift?.shift_type)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shift?.shift_type ?: "",
                                        fontSize = 12.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val TAG = "API_LOG"

private suspend fun loadSubdivisionShifts(
    year: Int,
    month: Int,
    token: String
): Result<List<EmployeeShiftsResponse>> {

    return withContext(Dispatchers.IO) {
        runCatching {

            val url = ApiConfig.subdivisionUrl(year, month)

            val startTime = System.currentTimeMillis()

            Log.d(TAG, "➡️ REQUEST: $url")
            Log.d(TAG, "Authorization: Bearer ${token.take(10)}...")

            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000

                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Accept", "application/json")
            }

            try {
                val code = connection.responseCode

                val body = if (code in 200..299) {
                    connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                }

                val duration = System.currentTimeMillis() - startTime

                Log.d(TAG, "⬅️ RESPONSE: HTTP $code (${duration}ms)")
                Log.d(TAG, "BODY: $body")

                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code: $body")
                }

                parseEmployees(body)
            } finally {
                connection.disconnect()
            }
        }
    }
}

private fun parseEmployees(body: String): List<EmployeeShiftsResponse> {

    val result = mutableListOf<EmployeeShiftsResponse>()
    val array = JSONArray(body)

    for (i in 0 until array.length()) {

        val obj = array.getJSONObject(i)

        val shiftsArray = obj.getJSONArray("work_shifts")
        val shifts = mutableListOf<ShiftDto>()

        for (j in 0 until shiftsArray.length()) {

            val s = shiftsArray.getJSONObject(j)

            shifts.add(
                ShiftDto(
                    id = s.optInt("id"),
                    employee_id = s.optInt("employee_id"),
                    shift_date = s.getInt("shift_date"),
                    shift_type = s.getString("shift_type"),
                    year = s.optInt("year"),
                    month = s.optInt("month")
                )
            )
        }

        result.add(
            EmployeeShiftsResponse(
                employee_id = obj.getInt("employee_id"),
                first_name = obj.getString("first_name"),
                last_name = obj.getString("last_name"),
                middle_name = obj.optString("middle_name"),
                work_shifts = shifts
            )
        )
    }

    return result
}
