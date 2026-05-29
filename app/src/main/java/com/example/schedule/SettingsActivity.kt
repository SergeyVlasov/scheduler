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
import android.app.DatePickerDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import kotlinx.serialization.encodeToString
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import androidx.compose.ui.platform.LocalContext
import java.time.LocalDate

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isAdmin = AuthActivity.isAdmin(this)

        setContent {
            var showReminderDialog by remember { mutableStateOf(false) }
            var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }

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

            LaunchedEffect(Unit) {
                lifecycleScope.launch {
                    try {
                        val token = getSharedPreferences(
                            AuthActivity.AUTH_PREFS,
                            MODE_PRIVATE
                        ).getString(AuthActivity.KEY_TOKEN, null) ?: return@launch

                        val request = Request.Builder()
                            .url(ApiConfig.BASE_URL + "/api/all_users")
                            .header("Authorization", "Bearer $token")
                            .build()

                        val response = withContext(Dispatchers.IO) {
                            NetworkModule.client.newCall(request).execute()
                        }

                        if (response.isSuccessful) {
                            val body = response.body?.string().orEmpty()
                            users = Json.decodeFromString<List<UserDto>>(body)
                        }

                    } catch (_: Exception) {
                    }
                }
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
                            isAdmin = isAdmin,
                            onLogout = {
                                getSharedPreferences(
                                    AuthActivity.AUTH_PREFS,
                                    MODE_PRIVATE
                                ).edit()
                                    .remove(AuthActivity.KEY_TOKEN)
                                    .remove(AuthActivity.KEY_IS_ADMIN)
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
                            },
                            onCreateReminder = {
                                showReminderDialog = true
                            }
                        )
                        if (showReminderDialog) {
                            ReminderDialog(
                                users = users,
                                onDismiss = {
                                    showReminderDialog = false
                                },
                                onCreate = { userIds, text, date ->

                                    lifecycleScope.launch {

                                        try {

                                            val token = getSharedPreferences(
                                                AuthActivity.AUTH_PREFS,
                                                MODE_PRIVATE
                                            ).getString(AuthActivity.KEY_TOKEN, null)
                                                ?: return@launch

                                            val requestBody = CreateNoteRequest(
                                                user_ids = userIds,
                                                note_text = text,
                                                expiration_date = "${date}T23:59:00"
                                            )

                                            val json = Json.encodeToString(requestBody)

                                            val request = Request.Builder()
                                                .url(ApiConfig.BASE_URL + "/api/create_note")
                                                .header("Authorization", "Bearer $token")
                                                .post(
                                                    json.toRequestBody(
                                                        "application/json".toMediaType()
                                                    )
                                                )
                                                .build()

                                            val response = withContext(Dispatchers.IO) {
                                                NetworkModule.client.newCall(request).execute()
                                            }

                                            if (response.isSuccessful) {
                                                Toast.makeText(
                                                    this@SettingsActivity,
                                                    "Напоминание создано",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            } else {
                                                Toast.makeText(
                                                    this@SettingsActivity,
                                                    "Ошибка ${response.code}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }

                                        } catch (e: Exception) {

                                            Toast.makeText(
                                                this@SettingsActivity,
                                                e.message ?: "Ошибка",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }

                                    showReminderDialog = false
                                }
                            )
                        }
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
                NetworkModule.client.newCall(request).execute()
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
    isAdmin: Boolean,
    onLogout: () -> Unit,
    onPickDiop: () -> Unit,
    onPickSopr: () -> Unit,
    onCreateReminder: () -> Unit,
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

        if (isAdmin) {
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

            Button(
                onClick = onCreateReminder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Создать напоминание")
            }
        }
    }
}
@Serializable
data class UserDto(
    val id: Int,
    val last_name: String,
    val first_name: String,
    val subdivision: String
)

@Serializable
data class CreateNoteRequest(
    val user_ids: List<Int>,
    val note_text: String,
    val expiration_date: String
)


@Composable
fun ReminderDialog(
    users: List<UserDto>,
    onDismiss: () -> Unit,
    onCreate: (
        userIds: List<Int>,
        text: String,
        date: String
    ) -> Unit
) {

    var noteText by remember { mutableStateOf("") }

    val context = LocalContext.current

    var selectedDate by remember {
        mutableStateOf(LocalDate.now().toString())
    }

    val selectedUsers = remember {
        mutableStateMapOf<Int, Boolean>()
    }

    // grouping теперь по subdivision
    val groupedUsers = users.groupBy { it.subdivision }

    AlertDialog(

        onDismissRequest = onDismiss,

        confirmButton = {},

        text = {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {

                OutlinedTextField(
                    value = noteText,
                    onValueChange = {
                        noteText = it
                    },
                    label = {
                        Text("Текст напоминания")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {

                        val today = LocalDate.now()

                        DatePickerDialog(
                            context,
                            { _, year, month, day ->

                                selectedDate =
                                    "%04d-%02d-%02d".format(
                                        year,
                                        month + 1,
                                        day
                                    )
                            },
                            today.year,
                            today.monthValue - 1,
                            today.dayOfMonth
                        ).show()
                    }
                ) {
                    Text("Дата окончания напоминания: $selectedDate")
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {

                    groupedUsers.forEach { (subdivision, usersInSubdivision) ->

                        Row(
                            modifier = Modifier.fillMaxWidth()
                        ) {

                            val allSelected =
                                usersInSubdivision.all {
                                    selectedUsers[it.id] == true
                                }

                            Checkbox(
                                checked = allSelected,

                                onCheckedChange = { checked ->

                                    usersInSubdivision.forEach {

                                        selectedUsers[it.id] = checked
                                    }
                                }
                            )

                            Text("Весь $subdivision")
                        }

                        usersInSubdivision.forEach { user ->

                            Row(
                                modifier = Modifier.fillMaxWidth()
                            ) {

                                Checkbox(
                                    checked =
                                        selectedUsers[user.id] == true,

                                    onCheckedChange = { checked ->
                                        selectedUsers[user.id] = checked
                                    }
                                )

                                Text(
                                    "${user.last_name} ${user.first_name}"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {

                        onCreate(
                            selectedUsers
                                .filterValues { it }
                                .keys
                                .toList(),

                            noteText,

                            selectedDate
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Создать напоминание")
                }
            }
        }
    )
}
