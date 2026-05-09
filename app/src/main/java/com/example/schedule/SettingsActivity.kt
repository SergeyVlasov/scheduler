package com.example.schedule

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.schedule.ui.theme.ScheduleTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                                    startActivity(
                                        Intent(
                                            this@SettingsActivity,
                                            MainActivity::class.java
                                        )
                                    )
                                    finish()
                                },
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
                                            this@SettingsActivity,
                                            DepartmentActivity::class.java
                                        )
                                    )
                                    finish()
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
                                selected = true,
                                onClick = { },
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

                        SettingsScreen(
                            modifier = Modifier.fillMaxSize(),

                            onLogout = {

                                getSharedPreferences(
                                    AuthActivity.AUTH_PREFS,
                                    MODE_PRIVATE
                                ).edit()
                                    .remove(AuthActivity.KEY_TOKEN)
                                    .apply()

                                startActivity(
                                    Intent(
                                        this@SettingsActivity,
                                        AuthActivity::class.java
                                    )
                                )

                                finish()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit
) {

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Text(
            text = "Настройки",
            style = MaterialTheme.typography.headlineSmall
        )

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Выйти")
        }

        Button(
            onClick = { },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Настройки экрана")
        }
    }
}