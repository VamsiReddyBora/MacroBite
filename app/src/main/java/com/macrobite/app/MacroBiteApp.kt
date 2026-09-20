package com.macrobite.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import com.macrobite.app.domain.model.NotificationPreferences
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.notification.NotificationHelper
import com.macrobite.app.notification.NotificationScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class MacroBiteApp : Application() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        installGlobalExceptionHandler(base ?: this)
    }

    override fun onCreate() {
        super.onCreate()
        installGlobalExceptionHandler(this)
        try {
            NotificationHelper.createNotificationChannels(this)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = userPreferencesRepository.getNotificationPreferences().firstOrNull()
                        ?: NotificationPreferences()
                    if (prefs.enabled) {
                        NotificationScheduler.scheduleAll(this@MacroBiteApp, prefs)
                    }
                } catch (e: Throwable) {
                    Log.e("MacroBiteApp", "Error initialising reminders on launch", e)
                }
            }
        } catch (t: Throwable) {
            Log.e("MacroBiteApp", "Failed to init notifications", t)
        }
    }

    private fun installGlobalExceptionHandler(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("MacroBite", "CRASH CAUGHT in thread ${thread.name}: ${throwable.message}", throwable)
            try {
                val stackTrace = Log.getStackTraceString(throwable)
                val intent = Intent(context, CrashActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    putExtra("EXTRA_ERROR_TITLE", "${throwable.javaClass.simpleName}: ${throwable.message}")
                    putExtra("EXTRA_ERROR_DETAILS", stackTrace)
                }
                context.startActivity(intent)
            } catch (e: Throwable) {
                Log.e("MacroBite", "Failed to launch CrashActivity", e)
                defaultHandler?.uncaughtException(thread, throwable)
            }
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(10)
        }
    }
}
