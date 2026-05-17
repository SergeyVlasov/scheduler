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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.schedule.ui.theme.ScheduleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val token = getSharedPreferences(
            AuthActivity.AUTH_PREFS,
            Context.MODE_PRIVATE
        ).getString(AuthActivity.KEY_TOKEN, null)

        if (token.isNullOrBlank()) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()

        setContent {
            ScheduleTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {

                        NavigationBar {

                            NavigationBarItem(
                                selected = true,
                                onClick = { },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Home,
                                        contentDescription = "График"
                                    )
                                },
                                label = {
                                    Text("График")
                                }
                            )

                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            DepartmentActivity::class.java
                                        )
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.List,
                                        contentDescription = "Отдел"
                                    )
                                },
                                label = {
                                    Text("Отдел")
                                }
                            )

                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    startActivity(
                                        Intent(
                                            this@MainActivity,
                                            SettingsActivity::class.java
                                        )
                                    )
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Настройки"
                                    )
                                },
                                label = {
                                    Text("Настройки")
                                }
                            )
                        }
                    }

                ) { innerPadding ->

                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        CalendarScreen(
                            authToken = token,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(
    authToken: String,
    modifier: Modifier = Modifier
) {

    var month by remember {
        mutableStateOf(YearMonth.now())
    }

    var shiftsByDay by remember {
        mutableStateOf<Map<Int, ShiftMarker>>(emptyMap())
    }

    var isLoading by remember {
        mutableStateOf(false)
    }

    var errorText by remember {
        mutableStateOf<String?>(null)
    }

    var pickedDay by remember {
        mutableStateOf<LocalDate?>(null)
    }

    LaunchedEffect(month, authToken) {

        isLoading = true
        errorText = null

        val result = loadMonthShifts(
            month = month,
            token = authToken
        )

        result.onSuccess {
            shiftsByDay = it
        }.onFailure { throwable ->

            shiftsByDay = emptyMap()

            errorText =
                throwable.message ?: "Не удалось загрузить смены"
        }

        isLoading = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 560.dp),

        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        CalendarHeader(
            month = month,
            onPrevMonth = {
                month = month.minusMonths(1)
            },
            onNextMonth = {
                month = month.plusMonths(1)
            }
        )

        if (isLoading) {
            Text("Загрузка смен...")
        }

        if (!errorText.isNullOrBlank()) {
            Text(
                text = errorText.orEmpty(),
                color = MaterialTheme.colorScheme.error
            )
        }

        MonthGrid(
            month = month,
            shiftsByDay = shiftsByDay,
            onDayClick = { dayOfMonth ->
                pickedDay = month.atDay(dayOfMonth)
            }
        )

        ShiftLegend()
    }

    pickedDay?.let { date ->
        DayWorkersDialog(
            date = date,
            authToken = authToken,
            onDismiss = { pickedDay = null }
        )
    }
}

@Composable
private fun DayWorkersDialog(
    date: LocalDate,
    authToken: String,
    onDismiss: () -> Unit
) {

    var workers by remember(date) {
        mutableStateOf<List<String>>(emptyList())
    }

    var dialogLoading by remember(date) {
        mutableStateOf(true)
    }

    var dialogError by remember(date) {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(date, authToken) {

        dialogLoading = true
        dialogError = null

        val result = loadDayWorkers(
            year = date.year,
            month = date.monthValue,
            day = date.dayOfMonth,
            token = authToken
        )

        result.onSuccess {
            workers = it
            dialogLoading = false
        }.onFailure { throwable ->
            workers = emptyList()
            dialogError =
                throwable.message ?: "Не удалось загрузить список"
            dialogLoading = false
        }
    }

    val locale = Locale.forLanguageTag("ru")

    val titleDate = remember(date) {

        val monthTitle =
            date.month.getDisplayName(
                TextStyle.FULL_STANDALONE,
                locale
            ).replaceFirstChar {
                if (it.isLowerCase()) {
                    it.titlecase(locale)
                } else {
                    it.toString()
                }
            }

        "${date.dayOfMonth} $monthTitle ${date.year}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        title = {
            Text(
                text = "Работают в этот день",
                style = MaterialTheme.typography.titleMedium
            )
        },
        text = {

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Text(
                    text = titleDate,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                when {

                    dialogLoading ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }

                    !dialogError.isNullOrBlank() ->
                        Text(
                            text = dialogError.orEmpty(),
                            color = MaterialTheme.colorScheme.error
                        )

                    workers.isEmpty() ->
                        Text("Нет данных о работниках")

                    else ->
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 360.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {

                            itemsIndexed(
                                items = workers,
                                key = { index, _ -> index }
                            ) { _, name ->

                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                }
            }
        }
    )
}

@Composable
fun ShiftLegend() {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {

        LegendItem(
            color = Color(0xFFDC2626),
            text = "Дневная смена"
        )

        LegendItem(
            color = Color(0xFF2563EB),
            text = "Ночная смена"
        )

        LegendItem(
            color = Color(0xFF16A34A),
            text = "8"
        )

        LegendItem(
            color = Color(0xFFA7F3D0),
            text = "7"
        )

        LegendItem(
            color = Color(0xFFBFDBFE),
            text = "У"
        )

        LegendItem(
            color = Color(0xFF808080),
            text = "Отпуск"
        )

        LegendItem(
            color = Color(0xFFFFA500),
            text = "Больничный"
        )
    }
}

@Composable
fun LegendItem(
    color: Color,
    text: String
) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color,
                    shape = MaterialTheme.shapes.small
                )
        )

        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {

    val arrowCircleColor = Color(0xFF294597)

    val locale = Locale.forLanguageTag("ru")

    val monthName = month.month
        .getDisplayName(
            TextStyle.FULL_STANDALONE,
            locale
        )
        .replaceFirstChar {
            if (it.isLowerCase()) {
                it.titlecase(locale)
            } else {
                it.toString()
            }
        }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {

        FilledIconButton(
            onClick = onPrevMonth,
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
            onClick = onNextMonth,
            modifier = Modifier.size(40.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = arrowCircleColor,
                contentColor = Color.White
            )
        ) {
            Text("›")
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    shiftsByDay: Map<Int, ShiftMarker>,
    onDayClick: (dayOfMonth: Int) -> Unit
) {

    val weekDays = listOf(
        "Пн",
        "Вт",
        "Ср",
        "Чт",
        "Пт",
        "Сб",
        "Вс"
    )

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth()
        ) {

            weekDays.forEach { label ->

                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }

        val firstDay = month.atDay(1)

        val leadingEmpty =
            firstDay.dayOfWeek.value - 1

        val daysInMonth =
            month.lengthOfMonth()

        val cells = buildList<Int?> {

            repeat(leadingEmpty) {
                add(null)
            }

            for (day in 1..daysInMonth) {
                add(day)
            }

            while (size % 7 != 0) {
                add(null)
            }
        }

        val today = LocalDate.now()

        val isCurrentMonth =
            today.year == month.year &&
                    today.month == month.month

        cells.chunked(7).forEach { week ->

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                week.forEach { day ->

                    val isToday =
                        isCurrentMonth &&
                                day == today.dayOfMonth

                    DayCell(
                        day = day,
                        isToday = isToday,
                        shiftMarker = day?.let {
                            shiftsByDay[it]
                        },
                        onClick = {
                            day?.let(onDayClick)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int?,
    isToday: Boolean,
    shiftMarker: ShiftMarker?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val borderColor =
        if (isToday) {
            Color.Green
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }

    val borderWidth =
        if (isToday) 5.dp else 1.dp

    val backgroundColor = when (shiftMarker) {

        ShiftMarker.DAY ->
            Color(0xFFDC2626)

        ShiftMarker.NIGHT ->
            Color(0xFF2563EB)

        ShiftMarker.NUMERIC ->
            Color(0xFF16A34A)

        ShiftMarker.SEVEN ->
            Color(0xFFA7F3D0) // бледно-зелёный

        ShiftMarker.U ->
            Color(0xFFBFDBFE) // бледно-синий

        ShiftMarker.OFF ->
            Color(0xFF808080)

        ShiftMarker.SICK ->
            Color(0xFFFFA500)

        null ->
            Color.Transparent
    }

    val cellModifier = modifier
        .aspectRatio(1f)
        .background(
            color = backgroundColor,
            shape = MaterialTheme.shapes.small
        )
        .border(
            width = borderWidth,
            color = borderColor,
            shape = MaterialTheme.shapes.small
        )
        .then(
            if (day != null) {
                Modifier.clickable(onClick = onClick)
            } else {
                Modifier
            }
        )

    Box(
        modifier = cellModifier,

        contentAlignment = Alignment.Center
    ) {

        Text(
            text = day?.toString() ?: "",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CalendarPreview() {

    ScheduleTheme {
        CalendarScreen(
            authToken = "preview-token"
        )
    }
}

private enum class ShiftMarker {
    DAY,
    NIGHT,
    NUMERIC,
    SEVEN,
    U,
    OFF,
    SICK
}

private suspend fun loadMonthShifts(
    month: YearMonth,
    token: String
): Result<Map<Int, ShiftMarker>> {

    val logTag = "ScheduleApi"

    return withContext(Dispatchers.IO) {

        runCatching {

            val requestUrl =
                ApiConfig.currentMonthUrl(
                    month.year,
                    month.monthValue
                )

            Log.d(
                logTag,
                "Request -> GET $requestUrl"
            )

            Log.d(
                logTag,
                "Request headers -> Authorization=Bearer ***, Accept=application/json"
            )

            val connection =
                (URL(requestUrl).openConnection() as HttpURLConnection).apply {

                    requestMethod = "GET"

                    connectTimeout = 10_000
                    readTimeout = 10_000

                    setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )

                    setRequestProperty(
                        "Accept",
                        "application/json"
                    )
                }

            val statusCode =
                connection.responseCode

            val responseBody =
                if (statusCode in 200..299) {

                    connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                } else {

                    connection.errorStream
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                }

            val bodyForLog =
                if (responseBody.length > 1_500) {
                    responseBody.take(1_500) + "...(truncated)"
                } else {
                    responseBody
                }

            Log.d(
                logTag,
                "Response <- status=$statusCode body=$bodyForLog"
            )

            if (statusCode !in 200..299) {

                Log.e(
                    logTag,
                    "Request failed with status=$statusCode"
                )

                throw IllegalStateException(
                    "Ошибка загрузки смен ($statusCode)"
                )
            }

            parseMonthShifts(responseBody)
        }
    }
}

private fun parseMonthShifts(
    body: String
): Map<Int, ShiftMarker> {

    val result =
        mutableMapOf<Int, ShiftMarker>()

    val array =
        JSONArray(body)

    for (index in 0 until array.length()) {

        val item =
            array.optJSONObject(index)
                ?: continue

        val shiftDate =
            item.optInt("shift_date", 0)

        val day =
            shiftDate % 100

        if (day !in 1..31) {
            continue
        }

        val shiftType =
            item.optString("shift_type")
                .trim()

        val marker = when {

            shiftType == "Д" ->
                ShiftMarker.DAY

            shiftType == "Н" ->
                ShiftMarker.NIGHT

            shiftType == "О" ->
                ShiftMarker.OFF

            shiftType == "Б" ->
                ShiftMarker.SICK

            shiftType == "7" ->
                ShiftMarker.SEVEN

            shiftType == "У" ->
                ShiftMarker.U

            shiftType.all { it.isDigit() } ->
                ShiftMarker.NUMERIC

            else -> null
        }

        if (marker != null) {
            result[day] = marker
        }
    }

    return result
}

private suspend fun loadDayWorkers(
    year: Int,
    month: Int,
    day: Int,
    token: String
): Result<List<String>> {

    val logTag = "ScheduleApi"

    return withContext(Dispatchers.IO) {

        runCatching {

            val requestUrl =
                ApiConfig.dayDetailUrl(
                    year,
                    month,
                    day
                )

            Log.d(
                logTag,
                "Request -> GET $requestUrl (day workers)"
            )

            val connection =
                (URL(requestUrl).openConnection() as HttpURLConnection).apply {

                    requestMethod = "GET"

                    connectTimeout = 10_000
                    readTimeout = 10_000

                    setRequestProperty(
                        "Authorization",
                        "Bearer $token"
                    )

                    setRequestProperty(
                        "Accept",
                        "application/json"
                    )
                }

            try {

                val statusCode =
                    connection.responseCode

                val responseBody =
                    if (statusCode in 200..299) {

                        connection.inputStream
                            .bufferedReader()
                            .use { it.readText() }

                    } else {

                        connection.errorStream
                            ?.bufferedReader()
                            ?.use { it.readText() }
                            .orEmpty()
                    }

                val bodyForLog =
                    if (responseBody.length > 1_500) {
                        responseBody.take(1_500) + "...(truncated)"
                    } else {
                        responseBody
                    }

                Log.d(
                    logTag,
                    "Response <- status=$statusCode body=$bodyForLog"
                )

                if (statusCode !in 200..299) {

                    throw IllegalStateException(
                        "Ошибка загрузки ($statusCode)"
                    )
                }

                parseDayWorkers(responseBody)

            } finally {
                connection.disconnect()
            }
        }
    }
}

private fun parseDayWorkers(body: String): List<String> {

    val trimmed =
        body.trim()

    if (trimmed.isEmpty()) {
        return emptyList()
    }

    return when (trimmed.first()) {

        '[' ->
            parseDayWorkersArray(JSONArray(trimmed))

        '{' -> {

            val root =
                JSONObject(trimmed)

            when {

                root.has("last_names_shift_D") ||
                        root.has("last_names_shift_N") ||
                        root.has("last_names_shift_8") ||
                        root.has("last_names_shift_7") ||
                        root.has("last_names_shift_U") ->
                    parseDayDetailShifts(root)

                root.has("employees") ->
                    parseDayWorkersArray(
                        root.getJSONArray("employees")
                    )

                root.has("data") ->
                    root.optJSONArray("data")?.let {
                        parseDayWorkersArray(it)
                    }.orEmpty()

                else ->
                    emptyList()
            }
        }

        else ->
            emptyList()
    }
}

private fun parseDayDetailShifts(
    root: JSONObject
): List<String> {

    val lines = mutableListOf<String>()

    val subdivision =
        root.optString("subdivision").trim()

    if (subdivision.isNotEmpty()) {
        lines.add(subdivision)
    }

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_D",
        label = "Дневная (Д)",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_N",
        label = "Ночная (Н)",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_8",
        label = "Смена 8",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_7",
        label = "Смена 7",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_U",
        label = "У",
        lines = lines
    )

    return lines
}

private fun appendShiftLastNamesLine(
    root: JSONObject,
    arrayKey: String,
    label: String,
    lines: MutableList<String>
) {

    val arr =
        root.optJSONArray(arrayKey)
            ?: return

    val names =
        mutableListOf<String>()

    for (index in 0 until arr.length()) {

        val s =
            arr.optString(index).trim()

        if (s.isNotEmpty()) {
            names.add(s)
        }
    }

    if (names.isNotEmpty()) {
        lines.add(
            "$label: ${names.joinToString(", ")}"
        )
    }
}

private fun parseDayWorkersArray(
    array: JSONArray
): List<String> {

    val names =
        mutableListOf<String>()

    for (index in 0 until array.length()) {

        val raw =
            array.get(index)

        when (raw) {

            is String ->
                if (raw.isNotBlank()) {
                    names.add(raw)
                }

            is JSONObject -> {

                val label =
                    buildEmployeeLabel(raw)

                if (label.isNotBlank()) {
                    names.add(label)
                }
            }
        }
    }

    return names
}

private fun buildEmployeeLabel(
    obj: JSONObject
): String {

    val fio =
        obj.optString("fio").trim()

    if (fio.isNotEmpty()) {
        return fio
    }

    val singleName =
        obj.optString("name").trim()

    if (singleName.isNotEmpty()) {
        return singleName
    }

    val last =
        obj.optString("last_name").trim()

    val first =
        obj.optString("first_name").trim()

    val middle =
        obj.optString("middle_name").trim()

    return listOf(
        last,
        first,
        middle
    ).filter { it.isNotEmpty() }
        .joinToString(" ")
}