package com.example.schedule

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.schedule.ui.theme.ScheduleTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = getSharedPreferences(AuthActivity.AUTH_PREFS, Context.MODE_PRIVATE)
            .getString(AuthActivity.KEY_TOKEN, null)
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
                                label = { Text("График") }
                            )
                            NavigationBarItem(
                                selected = false,
                                onClick = {
                                    startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                                },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "Настройки"
                                    )
                                },
                                label = { Text("Настройки") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Surface(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                        CalendarScreen(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

@Composable
fun CalendarScreen(modifier: Modifier = Modifier) {
    var month by remember { mutableStateOf(YearMonth.now()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .widthIn(max = 560.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CalendarHeader(
            month = month,
            onPrevMonth = { month = month.minusMonths(1) },
            onNextMonth = { month = month.plusMonths(1) },
        )

        MonthGrid(month = month)
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
) {
    val arrowCircleColor = Color(0xFF294597)
    val locale = Locale.forLanguageTag("ru")
    val monthName = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }

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
        ) { Text("‹") }

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
        ) { Text("›") }
    }
}

@Composable
private fun MonthGrid(month: YearMonth) {
    val weekDays = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }

        val firstDay = month.atDay(1)
        val leadingEmpty = firstDay.dayOfWeek.value - 1 // Monday=1 ... Sunday=7
        val daysInMonth = month.lengthOfMonth()

        val cells = buildList<Int?> {
            repeat(leadingEmpty) { add(null) }
            for (day in 1..daysInMonth) add(day)
            while (size % 7 != 0) add(null)
        }

        val today = LocalDate.now()
        val isCurrentMonth = today.year == month.year && today.month == month.month

        cells.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { day ->
                    val isToday = isCurrentMonth && day == today.dayOfMonth
                    DayCell(day = day, isToday = isToday, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int?, isToday: Boolean, modifier: Modifier = Modifier) {
    val borderColor = if (isToday) Color.Red else MaterialTheme.colorScheme.outlineVariant
    val borderWidth = if (isToday) 3.dp else 1.dp

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .border(width = borderWidth, color = borderColor, shape = MaterialTheme.shapes.small),
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
        CalendarScreen()
    }
}