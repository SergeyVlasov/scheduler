package com.example.schedule

import android.content.Context
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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import android.util.Log
import com.example.schedule.ui.theme.ScheduleTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_TOKEN, null) != null) {
            openMainAndFinish()
            return
        }

        enableEdgeToEdge()
        setContent {
            ScheduleTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AuthScreen(
                        onAuthSuccess = { token ->
                            prefs.edit().putString(KEY_TOKEN, token).apply()
                            openMainAndFinish()
                        }
                    )
                }
            }
        }
    }

    private fun openMainAndFinish() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    companion object {
        const val AUTH_PREFS = "auth_prefs"
        const val KEY_TOKEN = "auth_token"
    }
}

@Composable
private fun AuthScreen(
    onAuthSuccess: (String) -> Unit,
) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Авторизация",
            style = MaterialTheme.typography.headlineSmall
        )

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Логин") },
            singleLine = true
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Пароль") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation()
        )

        Button(
            onClick = {
                if (login.isBlank() || password.isBlank()) {
                    error = "Введите логин и пароль"
                    return@Button
                }

                isLoading = true
                error = null
                scope.launch {
                    val result = loginRequest(login.trim(), password)
                    isLoading = false
                    result.onSuccess(onAuthSuccess)
                        .onFailure { throwable ->
                            error = throwable.message ?: "Ошибка входа"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text("Войти")
            }
        }

        if (error != null) {
            Text(
                text = error.orEmpty(),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

private suspend fun loginRequest(login: String, password: String): Result<String> {
    return withContext(Dispatchers.IO) {
        runCatching {
            val logTag = "Network"
            val connection = (URL(ApiConfig.LOGIN_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }

            val requestBody = JSONObject()
                .put("login", login)
                .put("password", password)
                .toString()

            Log.d(logTag, "-> ${connection.requestMethod} ${ApiConfig.LOGIN_URL}")
            Log.d(logTag, "-> Headers: Content-Type=application/json")
            Log.d(logTag, "-> Body: $requestBody")

            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray())
            }

            val statusCode = connection.responseCode
            val body = if (statusCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
            }

            Log.d(logTag, "<- Status: $statusCode ${connection.responseMessage.orEmpty()}")
            Log.d(logTag, "<- Body: $body")

            if (statusCode !in 200..299) {
                Log.e(logTag, "<- Request failed with status $statusCode")
                throw IllegalStateException("Ошибка авторизации ($statusCode)")
            }

            val responseJson = JSONObject(body)
            val token = responseJson.optString("access_token")
                .ifBlank { responseJson.optString("token") }
            if (token.isBlank()) {
                Log.e(logTag, "<- Token is missing in response")
                throw IllegalStateException("Токен не получен")
            }

            token
        }
    }
}
