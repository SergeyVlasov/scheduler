package com.example.schedule

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ScheduleFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.i(TAG, "FCM token: $token")
        // Если вы используете собственный сервер, отправьте этот токен на него.
        // Пример: sendTokenToServer(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Сообщение получено! От: ${remoteMessage.from}")

        // Проверяем, есть ли данные в сообщении
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Данные сообщения: ${remoteMessage.data}")
            // Здесь вы можете обработать полезную нагрузку данных
        }

        // Проверяем, есть ли полезная нагрузка уведомления
        remoteMessage.notification?.let {
            Log.d(TAG, "Заголовок уведомления: ${it.title}")
            Log.d(TAG, "Текст уведомления: ${it.body}")

            // Теперь создаем и отображаем уведомление
            sendNotification(it.title, it.body)
        }
    }

    private fun sendNotification(messageTitle: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java) // Замените MainActivity на Activity, которое должно открываться
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0 /* Request code */, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE) // Добавлен FLAG_IMMUTABLE

        val channelId = "default_channel_id" // Уникальный ID вашего канала уведомлений
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher) // Замените на иконку вашего приложения
            .setContentTitle(messageTitle ?: "Новое уведомление")
            .setContentText(messageBody ?: "Получено новое сообщение.")
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Создание канала уведомлений для Android 8.0 (API 26) и выше
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId,
                "Default Channel Name", // Название канала, которое будет видеть пользователь
                NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(0 /* ID уведомления */, notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FirebaseToken"
    }
}
