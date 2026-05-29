package com.example.schedule

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging

class ScheduleApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.i(TAG, "FCM token: ${task.result}")
            } else {
                Log.w(TAG, "Failed to get FCM token", task.exception)
            }
        }
    }

    companion object {
        private const val TAG = "FirebaseToken"
    }
}
