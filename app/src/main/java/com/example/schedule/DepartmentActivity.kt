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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
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
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

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

            val configuration = LocalConfiguration.current

            val isLandscape =
                configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            ScheduleTheme {

                Scaffold(
                    bottomBar = {

                        if (!isLandscape) {

                            NavigationBar {

                                NavigationBarItem(
                                    selected = false,
                                    onClick = {
                                        startActivity(
                                            Intent(activity, MainActivity::class.java)
                                        )
                                        finish()
                                    },
                                    icon = {
                                        Icon(Icons.Filled.Home, null)
                                    },
                                    label = {
                                        Text("График")
                                    }
                                )

                                NavigationBarItem(
                                    selected = true,
                                    onClick = {},
                                    icon = {
                                        Icon(Icons.Filled.List, null)
                                    },
                                    label = {
                                        Text("Отдел")
                                    }
                                )

                                NavigationBarItem(
                                    selected = false,
                                    onClick = {
                                        startActivity(
                                            Intent(activity, SettingsActivity::class.java)
                                        )
                                        finish()
                                    },
                                    icon = {
                                        Icon(Icons.Filled.Settings, null)
                                    },
                                    label = {
                                        Text("Настройки")
                                    }
                                )
                            }
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
    val configuration = LocalConfiguration.current

    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var employees by remember { mutableStateOf<List<EmployeeShiftsResponse>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val groupedEmployees = remember(employees) {
        employees
            .sortedBy { it.employee_id }
            .groupBy { it.subdivision }
    }

    val days = remember(month) {
        (1..month.lengthOfMonth()).toList()
    }

    val vScroll = rememberScrollState()
    val hScroll = rememberScrollState()
    val context = LocalContext.current

    LaunchedEffect(month) {
        loading = true
        error = null

        val result = loadSubdivisionShifts(
            context = context,
            year = month.year,
            month = month.monthValue,
            token = token
        )

        result
            .onSuccess { load ->
                employees = load.value.sortedBy { it.employee_id }
                if (load.fromCache) {
                    error = OFFLINE_DATA_HINT
                }
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

            "1", "2", "3", "4", "5", "6",
            "7", "8", "9", "10", "11", "12",
            "У" -> Color(0xFF90EE90)

            else -> Color.LightGray
        }
    }

    val employeeShiftMaps = remember(employees) {
        employees.associate { emp ->
            emp.employee_id to emp.work_shifts.associate {
                val day = it.shift_date % 100
                day to it
            }
        }
    }

    val monthName = MONTHS_RU[month.monthValue - 1]

    Column(modifier = modifier.padding(12.dp)) {

        val arrowCircleColor = Color(0xFF294597)

        if (!isLandscape) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                FilledIconButton(
                    onClick = { month = month.minusMonths(1) },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = arrowCircleColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("‹")
                }

                Text(
                    text = "$monthName ${month.year}",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )

                FilledIconButton(
                    onClick = { month = month.plusMonths(1) },
                    modifier = Modifier.size(40.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = arrowCircleColor,
                        contentColor = Color.White
                    )
                ) {
                    Text("›")
                }
            }

            Spacer(Modifier.height(8.dp))
        }

        if (loading) Text("Загрузка...")
        error?.let {
            Text(
                text = it,
                color = if (it == OFFLINE_DATA_HINT) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        val cellSize = 32.dp
        val rowHeight = cellSize

        Row(modifier = Modifier.fillMaxSize()) {

            // ЛЕВАЯ КОЛОНКА С ФАМИЛИЯМИ
            Column(
                modifier = Modifier.width(100.dp)
            ) {

                Spacer(Modifier.height(rowHeight))

                Column(
                    modifier = Modifier.verticalScroll(vScroll)
                ) {

                    groupedEmployees.forEach { (subdivision, employeesInSubdivision) ->

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF294597))
                                .padding(4.dp)
                        ) {
                            Text(
                                text = subdivision,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }

                        employeesInSubdivision.forEach { emp ->

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
                }
            }

            // ПРАВАЯ ЧАСТЬ ТАБЛИЦЫ
            Column(
                modifier = Modifier
                    .horizontalScroll(hScroll)
            ) {

                // ЗАКРЕПЛЕННАЯ ШАПКА
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

                // СКРОЛЛ ТОЛЬКО ДЛЯ ТЕЛА ТАБЛИЦЫ
                Column(
                    modifier = Modifier.verticalScroll(vScroll)
                ) {

                    groupedEmployees.forEach { (_, employeesInSubdivision) ->

                        Box(
                            modifier = Modifier
                                .width(cellSize * days.size)
                                .height(rowHeight)
                                .background(Color(0xFF294597))
                        )

                        employeesInSubdivision.forEach { emp ->

                            val shiftMap =
                                employeeShiftMaps[emp.employee_id] ?: emptyMap()

                            Row {

                                days.forEach { day ->

                                    val shift = shiftMap[day]

                                    val date = LocalDate.of(month.year, month.monthValue, day)

                                    val isWeekend =
                                        date.dayOfWeek == DayOfWeek.SATURDAY ||
                                                date.dayOfWeek == DayOfWeek.SUNDAY

                                    val baseColor = shiftColors(shift?.shift_type)

                                    Box(
                                        modifier = Modifier
                                            .size(cellSize)
                                            .border(1.dp, Color.Black)
                                            .background(
                                                if (isWeekend)
                                                    darkenColor(baseColor)
                                                else
                                                    baseColor
                                            ),
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
}

private const val TAG = "API_LOG"

private suspend fun loadSubdivisionShifts(
    context: Context,
    year: Int,
    month: Int,
    token: String
): Result<ScheduleLoad<List<EmployeeShiftsResponse>>> {

    val cache = ScheduleCache(context)

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

                cache.putSubdivision(year, month, body)

                ScheduleLoad(parseEmployees(body))

            } finally {
                connection.disconnect()
            }

        }.recoverCatching { error ->

            val cachedBody = cache.getSubdivision(year, month)

            if (cachedBody != null) {
                Log.w(TAG, "Using cached subdivision shifts", error)
                ScheduleLoad(
                    parseEmployees(cachedBody),
                    fromCache = true
                )
            } else {
                throw error
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
                subdivision = obj.optString("subdivision"),
                work_shifts = shifts
            )
        )
    }

    return result
}

private fun darkenColor(color: Color, factor: Float = 0.75f): Color {
    return Color(
        red = color.red * factor,
        green = color.green * factor,
        blue = color.blue * factor,
        alpha = color.alpha
    )
}