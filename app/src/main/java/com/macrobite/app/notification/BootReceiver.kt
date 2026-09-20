package com.macrobite.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.macrobite.app.domain.model.NotificationPreferences
import com.macrobite.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d("BootReceiver", "Received broadcast action: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val prefs = userPreferencesRepository.getNotificationPreferences().firstOrNull()
                        ?: NotificationPreferences()

                    Log.d("BootReceiver", "Restoring scheduled meal alarms after boot. Master enabled: ${prefs.enabled}")
                    NotificationScheduler.scheduleAll(context, prefs)
                } catch (e: Throwable) {
                    Log.e("BootReceiver", "Error restoring scheduled alarms after boot", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
