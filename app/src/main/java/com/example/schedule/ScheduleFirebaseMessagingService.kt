package com.example.schedule

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ScheduleFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.i(TAG, "FCM token: $token")
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message from: ${remoteMessage.from}, data: ${remoteMessage.data}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: remoteMessage.data["Title"]
        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: remoteMessage.data["Body"]
            ?: remoteMessage.data["message"]
            ?: remoteMessage.data["text"]

        if (title.isNullOrBlank() && body.isNullOrBlank()) {
            Log.w(TAG, "Push received without title/body, skipping notification")
            return
        }

        NotificationHelper.showNotification(this, title, body)
    }

    companion object {
        private const val TAG = "FirebaseToken"
    }
}
