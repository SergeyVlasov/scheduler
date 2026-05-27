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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.example.schedule.effects.SnowEffect
import com.example.schedule.effects.isSnowSeason
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

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {

                        CalendarScreen(
                            authToken = token,
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isSnowSeason()) {
                            SnowEffect(
                                modifier = Modifier.fillMaxSize()
                            )
                        }
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

    val context = LocalContext.current

    LaunchedEffect(month, authToken) {

        isLoading = true
        errorText = null

        val result = loadMonthShifts(
            context = context,
            month = month,
            token = authToken
        )

        result.onSuccess { load ->
            shiftsByDay = load.value
            if (load.fromCache) {
                errorText = OFFLINE_DATA_HINT
            }
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
                color = if (errorText == OFFLINE_DATA_HINT) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }

        MonthGrid(
            month = month,
            shiftsByDay = shiftsByDay,
            onDayClick = { dayOfMonth ->
                pickedDay = month.atDay(dayOfMonth)
            }
        )

        LegendButton()
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

    val context = LocalContext.current

    LaunchedEffect(date, authToken) {

        dialogLoading = true
        dialogError = null

        val result = loadDayWorkers(
            context = context,
            year = date.year,
            month = date.monthValue,
            day = date.dayOfMonth,
            token = authToken
        )

        result.onSuccess { load ->
            workers = load.value
            if (load.fromCache) {
                dialogError = OFFLINE_DATA_HINT
            }
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
                            color = if (dialogError == OFFLINE_DATA_HINT) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.error
                            }
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
fun LegendButton() {

    var showDialog by remember {
        mutableStateOf(false)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {

        FilledIconButton(
            onClick = {
                showDialog = true
            }
        ) {

            Text(
                text = "?",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }

    if (showDialog) {

        AlertDialog(
            onDismissRequest = {
                showDialog = false
            },

            confirmButton = {

                TextButton(
                    onClick = {
                        showDialog = false
                    }
                ) {
                    Text("Закрыть")
                }
            },

            title = {
                Text("Обозначения")
            },

            text = {

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
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
                        text = "Не 8"
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

                week.forEachIndexed { index, day ->

                    val isToday =
                        isCurrentMonth &&
                                day == today.dayOfMonth

                    val isWeekend = index >= 5

                    DayCell(
                        day = day,
                        isToday = isToday,
                        isWeekend = isWeekend,
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
    isWeekend: Boolean,
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

    val baseColor = when (shiftMarker) {

        ShiftMarker.DAY ->
            Color(0xFFDC2626)

        ShiftMarker.NIGHT ->
            Color(0xFF2563EB)

        ShiftMarker.EIGHT ->
            Color(0xFF16A34A)

        ShiftMarker.NOT_EIGHT ->
            Color(0xFFA7F3D0)

        ShiftMarker.U ->
            Color(0xFFBFDBFE)

        ShiftMarker.OFF ->
            Color(0xFF808080)

        ShiftMarker.SICK ->
            Color(0xFFFFA500)

        null ->
            Color(0xFFE5E7EB)
    }

    val backgroundColor =
        if (isWeekend) {
            darkenColor(baseColor, factor = 0.55f)
        } else {
            baseColor
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
            textAlign = TextAlign.Center,
            color = Color.Black
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
    EIGHT,
    NOT_EIGHT,
    U,
    OFF,
    SICK
}

private suspend fun loadMonthShifts(
    context: Context,
    month: YearMonth,
    token: String
): Result<ScheduleLoad<Map<Int, ShiftMarker>>> {

    val logTag = "ScheduleApi"
    val cache = ScheduleCache(context)

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

            val responseBody = fetchAuthorizedGet(requestUrl, token)

            cache.putMonth(
                month.year,
                month.monthValue,
                responseBody
            )

            ScheduleLoad(parseMonthShifts(responseBody))

        }.recoverCatching { error ->

            val cachedBody =
                cache.getMonth(month.year, month.monthValue)

            if (cachedBody != null) {
                Log.w(logTag, "Using cached month shifts", error)
                ScheduleLoad(
                    parseMonthShifts(cachedBody),
                    fromCache = true
                )
            } else {
                throw error
            }
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

            shiftType == "8" ->
                ShiftMarker.EIGHT

            shiftType in setOf(
                "1", "2", "3", "4",
                "5", "6", "7",
                "9", "10", "11", "12"
            ) ->
                ShiftMarker.NOT_EIGHT

            shiftType == "У" ->
                ShiftMarker.U

            else -> null
        }

        if (marker != null) {
            result[day] = marker
        }
    }

    return result
}

private suspend fun loadDayWorkers(
    context: Context,
    year: Int,
    month: Int,
    day: Int,
    token: String
): Result<ScheduleLoad<List<String>>> {

    val logTag = "ScheduleApi"
    val cache = ScheduleCache(context)

    return withContext(Dispatchers.IO) {

        runCatching {

            val requestUrl =
                ApiConfig.dayDetailUrl(year, month, day)

            Log.d(
                logTag,
                "Request -> GET $requestUrl (day workers)"
            )

            val responseBody = fetchAuthorizedGet(requestUrl, token)

            cache.putDayDetail(year, month, day, responseBody)

            ScheduleLoad(parseDayWorkers(responseBody))

        }.recoverCatching { error ->

            val cachedBody =
                cache.getDayDetail(year, month, day)

            if (cachedBody != null) {
                Log.w(logTag, "Using cached day workers", error)
                ScheduleLoad(
                    parseDayWorkers(cachedBody),
                    fromCache = true
                )
            } else {
                throw error
            }
        }
    }
}

private fun fetchAuthorizedGet(
    requestUrl: String,
    token: String
): String {

    val connection =
        (URL(requestUrl).openConnection() as HttpURLConnection).apply {

            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("Accept", "application/json")
        }

    try {

        val statusCode = connection.responseCode

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

        if (statusCode !in 200..299) {
            throw IllegalStateException("Ошибка загрузки ($statusCode)")
        }

        return responseBody

    } finally {
        connection.disconnect()
    }
}

private fun parseDayWorkers(body: String): List<String> {

    val trimmed = body.trim()

    if (trimmed.isEmpty()) {
        return emptyList()
    }

    return when (trimmed.first()) {

        '[' ->
            parseDayWorkersArray(JSONArray(trimmed))

        '{' -> {

            val root = JSONObject(trimmed)

            when {

                root.has("last_names_shift_D") ||
                        root.has("last_names_shift_N") ||
                        root.has("last_names_shift_8") ||
                        root.has("last_names_shift_7") ||
                        root.has("last_names_shift_U") ->
                    parseDayDetailShifts(root)

                root.has("employees") ->
                    parseDayWorkersArray(root.getJSONArray("employees"))

                root.has("data") ->
                    root.optJSONArray("data")
                        ?.let { parseDayWorkersArray(it) }
                        ?: emptyList()

                else ->
                    emptyList()
            }
        }

        else -> emptyList()
    }
}

private fun parseDayWorkersArray(array: JSONArray): List<String> {

    val names = mutableListOf<String>()

    for (i in 0 until array.length()) {

        when (val raw = array.get(i)) {

            is String -> {
                if (raw.isNotBlank()) {
                    names.add(raw)
                }
            }

            is JSONObject -> {
                val label = buildEmployeeLabel(raw)
                if (label.isNotBlank()) {
                    names.add(label)
                }
            }
        }
    }

    return names
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
        arrayKey = "last_names_shift_U",
        label = "У",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_1",
        label = "Смена 1",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_2",
        label = "Смена 2",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_3",
        label = "Смена 3",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_4",
        label = "Смена 4",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_5",
        label = "Смена 5",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_6",
        label = "Смена 6",
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
        arrayKey = "last_names_shift_9",
        label = "Смена 9",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_10",
        label = "Смена 10",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_11",
        label = "Смена 11",
        lines = lines
    )

    appendShiftLastNamesLine(
        root = root,
        arrayKey = "last_names_shift_12",
        label = "Смена 12",
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

    val arr = root.optJSONArray(arrayKey) ?: return

    val names = mutableListOf<String>()

    for (i in 0 until arr.length()) {
        val s = arr.optString(i).trim()
        if (s.isNotEmpty()) {
            names.add(s)
        }
    }

    if (names.isNotEmpty()) {
        lines.add("$label: ${names.joinToString(", ")}")
    }
}

private fun buildEmployeeLabel(obj: JSONObject): String {

    val fio = obj.optString("fio").trim()
    if (fio.isNotEmpty()) return fio

    val name = obj.optString("name").trim()
    if (name.isNotEmpty()) return name

    val last = obj.optString("last_name").trim()
    val first = obj.optString("first_name").trim()
    val middle = obj.optString("middle_name").trim()

    return listOf(last, first, middle)
        .filter { it.isNotEmpty() }
        .joinToString(" ")
}

private fun darkenColor(
    color: Color,
    factor: Float = 0.65f
): Color {

    return Color(
        red = color.red * factor,
        green = color.green * factor,
        blue = color.blue * factor,
        alpha = color.alpha
    )
}