package com.example.schedule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.schedule.data.model.EmployeeShiftsResponse
import com.example.schedule.data.model.ShiftDto
import com.example.schedule.ui.theme.ScheduleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val MONTHS_RU = listOf(
    "Январь","Февраль","Март","Апрель","Май","Июнь",
    "Июль","Август","Сентябрь","Октябрь","Ноябрь","Декабрь"
)

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
                                onClick = {},
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

    val sortedEmployees = remember(employees) {
        employees.sortedBy { it.employee_id }
    }

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
            .onSuccess { list ->
                employees = list.sortedBy { it.employee_id }
            }
            .onFailure {
                employees = emptyList()
                error = it.message
            }

        loading = false
    }

    val shiftColors: (String?) -> Color = { type ->
        when (type) {
            "Д" -> Color(0xFFDC2626)
            "Н" -> Color(0xFF2563EB)
            "О" -> Color.Gray
            "Б" -> Color(0xFFFFA500)
            "8", "7", "У" -> Color(0xFF90EE90)
            else -> Color.LightGray
        }
    }

    val employeeShiftMaps = remember(sortedEmployees) {
        sortedEmployees.associate { emp ->
            emp.employee_id to emp.work_shifts.associate {
                val day = it.shift_date % 100
                day to it
            }
        }
    }

    val monthName = MONTHS_RU[month.monthValue - 1]

    Column(modifier = modifier.padding(12.dp)) {

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { month = month.minusMonths(1) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null)
            }

            Text(
                text = "$monthName ${month.year}",
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

        Row(modifier = Modifier.fillMaxSize()) {

            Column(
                modifier = Modifier
                    .width(100.dp)
                    .verticalScroll(vScroll)
            ) {
                Spacer(Modifier.height(rowHeight))

                sortedEmployees.forEach { emp ->
                    Text(
                        text = emp.last_name,
                        fontSize = 12.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .height(rowHeight)
                            .padding(start = 4.dp, top = 3.dp),
                        lineHeight = 12.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .horizontalScroll(hScroll)
                    .verticalScroll(vScroll)
            ) {

                Column {

                    Row {
                        days.forEach { day ->

                            val date = LocalDate.of(month.year, month.monthValue, day)

                            val isWeekend =
                                date.dayOfWeek == DayOfWeek.SATURDAY ||
                                        date.dayOfWeek == DayOfWeek.SUNDAY

                            Box(
                                modifier = Modifier
                                    .width(cellSize)
                                    .height(rowHeight)
                                    .border(1.dp, Color.Black)
                                    .background(
                                        if (isWeekend) Color(0xFF616161)
                                        else Color(0xFFBDBDBD)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 12.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    sortedEmployees.forEach { emp ->

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

                Log.d(TAG, "HTTP $code")
                Log.d(TAG, body)

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

    val array = JSONArray(body)
    val result = mutableListOf<EmployeeShiftsResponse>()

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