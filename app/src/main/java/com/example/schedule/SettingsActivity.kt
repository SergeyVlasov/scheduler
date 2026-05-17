package com.example.schedule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.schedule.ui.theme.ScheduleTheme
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.example.schedule.ApiConfig

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {

            val diopPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { uploadFile(it, ApiConfig.uploadDiopUrl()) }
            }

            val soprPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri ->
                uri?.let { uploadFile(it, ApiConfig.uploadSoprUrl()) }
            }

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
                                    Icon(Icons.Filled.Home, contentDescription = "График")
                                },
                                label = { Text("График") }
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
                                    Icon(Icons.Filled.List, contentDescription = "Отдел")
                                },
                                label = { Text("Отдел") }
                            )

                            NavigationBarItem(
                                selected = true,
                                onClick = { },
                                icon = {
                                    Icon(Icons.Filled.Settings, contentDescription = "Настройки")
                                },
                                label = { Text("Настройки") }
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
                            },

                            onPickDiop = {
                                diopPicker.launch(arrayOf("application/vnd.ms-excel"))
                            },

                            onPickSopr = {
                                soprPicker.launch(arrayOf("application/vnd.ms-excel"))
                            }
                        )
                    }
                }
            }
        }
    }

    private fun uploadFile(uri: Uri, url: String) {
        val token = getSharedPreferences(AuthActivity.AUTH_PREFS, MODE_PRIVATE)
            .getString(AuthActivity.KEY_TOKEN, null)
        if (token.isNullOrBlank()) {
            Toast.makeText(this, "Сначала войдите в аккаунт", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = resolveFileName(uri)
        if (!fileName.lowercase().endsWith(".xls")) {
            Toast.makeText(this, "Нужен файл с расширением .xls", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val bytes = withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: run {
                Toast.makeText(this@SettingsActivity, "Не удалось прочитать файл", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val requestBody = bytes.toRequestBody("application/vnd.ms-excel".toMediaType())
            val part = MultipartBody.Part.createFormData("file", fileName, requestBody)

            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer $token")
                .post(
                    MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addPart(part)
                        .build()
                )
                .build()

            withContext(Dispatchers.IO) {
                OkHttpClient().newCall(request).execute()
            }.use { response ->
                val message = if (response.isSuccessful) {
                    "Файл загружен"
                } else {
                    "Ошибка загрузки: ${response.code}"
                }
                Toast.makeText(this@SettingsActivity, message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resolveFileName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)?.let { return it }
            }
        }
        return "upload.xls"
    }
}

@Composable
private fun SettingsScreen(
    modifier: Modifier = Modifier,
    onLogout: () -> Unit,
    onPickDiop: () -> Unit,
    onPickSopr: () -> Unit
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
            onClick = onPickDiop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Загрузить график ДИОП")
        }

        Button(
            onClick = onPickSopr,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Загрузить график СОПР")
        }
    }
}