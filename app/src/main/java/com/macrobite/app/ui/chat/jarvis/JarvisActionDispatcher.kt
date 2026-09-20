package com.macrobite.app.ui.chat.jarvis

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.BatteryManager
import android.provider.AlarmClock
import android.provider.Settings
import android.util.Log
import com.macrobite.app.domain.model.JarvisActionPayload
import com.macrobite.app.domain.model.JarvisActionType

sealed class JarvisDispatchResult {
    data class Success(val message: String) : JarvisDispatchResult()
    data class Info(val message: String) : JarvisDispatchResult()
    data class Error(val message: String) : JarvisDispatchResult()
}

object JarvisActionDispatcher {

    private const val TAG = "JarvisActionDispatcher"
    private var isTorchOn: Boolean = false

    fun dispatch(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        return try {
            when (payload.actionType) {
                JarvisActionType.CALL -> executeCall(context, payload)
                JarvisActionType.WHATSAPP -> executeWhatsApp(context, payload)
                JarvisActionType.SMS -> executeSms(context, payload)
                JarvisActionType.ALARM -> executeAlarm(context, payload)
                JarvisActionType.TIMER -> executeTimer(context, payload)
                JarvisActionType.OPEN_APP -> executeOpenApp(context, payload)
                JarvisActionType.MAPS -> executeMaps(context, payload)
                JarvisActionType.YOUTUBE -> executeYouTube(context, payload)
                JarvisActionType.FLASHLIGHT -> executeFlashlight(context, payload)
                JarvisActionType.BATTERY -> executeBatteryCheck(context)
                JarvisActionType.NOTE -> executeNote(context, payload)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dispatch action: ${payload.actionType}", e)
            JarvisDispatchResult.Error("Could not execute ${payload.displayTitle}: ${e.localizedMessage ?: "Unknown error"}")
        }
    }

    private fun executeCall(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        var targetNumber = payload.phoneNumber?.trim()
        var contactDisplayName = payload.contactName?.trim()

        if (targetNumber.isNullOrBlank() && !contactDisplayName.isNullOrBlank()) {
            val matched = ContactResolver.findContactPhoneNumber(context, contactDisplayName)
            if (matched != null) {
                targetNumber = matched.phoneNumber
                contactDisplayName = matched.displayName
            }
        }

        return if (!targetNumber.isNullOrBlank()) {
            val clean = targetNumber.replace(Regex("[^0-9+]"), "")
            val name = contactDisplayName ?: clean
            val hasCallPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CALL_PHONE
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (hasCallPermission) {
                val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$clean")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(callIntent)
                JarvisDispatchResult.Success("Calling $name ($clean)...")
            } else {
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clean")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(dialIntent)
                JarvisDispatchResult.Success("Dialing $name ($clean)...")
            }
        } else {
            val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(dialIntent)
            JarvisDispatchResult.Info("Opening phone dialer...")
        }
    }

    private fun executeWhatsApp(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        var targetNumber = payload.phoneNumber?.trim()
        val contactDisplayName = payload.contactName?.trim()
        val messageText = payload.message?.trim().orEmpty()

        if (targetNumber.isNullOrBlank() && !contactDisplayName.isNullOrBlank()) {
            val matched = ContactResolver.findContactPhoneNumber(context, contactDisplayName)
            if (matched != null) {
                targetNumber = matched.phoneNumber
            }
        }

        if (!targetNumber.isNullOrBlank()) {
            var clean = targetNumber.replace(Regex("[^0-9]"), "")
            // If local 10-digit number without country code, assume India (+91) if 10 digits
            if (clean.length == 10) {
                clean = "91$clean"
            }
            val url = "https://api.whatsapp.com/send?phone=$clean&text=${Uri.encode(messageText)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(intent)
                JarvisDispatchResult.Success("Opening WhatsApp chat with $targetNumber...")
            } catch (e: Exception) {
                // Fallback to browser WhatsApp
                try {
                    val fallback = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                    JarvisDispatchResult.Success("Opening WhatsApp link...")
                } catch (ex: Exception) {
                    JarvisDispatchResult.Error("WhatsApp is not installed on this device.")
                }
            }
        } else {
            // General text share to WhatsApp
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage("com.whatsapp")
                putExtra(Intent.EXTRA_TEXT, messageText)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            return try {
                context.startActivity(intent)
                JarvisDispatchResult.Success("Opening WhatsApp...")
            } catch (e: Exception) {
                JarvisDispatchResult.Error("WhatsApp is not installed.")
            }
        }
    }

    private fun executeSms(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        var targetNumber = payload.phoneNumber?.trim().orEmpty()
        val contactDisplayName = payload.contactName?.trim()
        val messageText = payload.message?.trim().orEmpty()

        if (targetNumber.isBlank() && !contactDisplayName.isNullOrBlank()) {
            val matched = ContactResolver.findContactPhoneNumber(context, contactDisplayName)
            if (matched != null) {
                targetNumber = matched.phoneNumber
            }
        }

        val clean = targetNumber.replace(Regex("[^0-9+]"), "")
        val smsUri = Uri.parse("smsto:$clean")
        val intent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
            putExtra("sms_body", messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return JarvisDispatchResult.Success("Drafting SMS to ${if (clean.isNotBlank()) clean else "contact"}...")
    }

    private fun executeAlarm(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        val hour = payload.timeHour ?: 7
        val minute = payload.timeMinute ?: 0
        val label = payload.title ?: "MacroBite Reminder"

        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        val amPm = if (hour < 12) "AM" else "PM"
        val displayH = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        return JarvisDispatchResult.Success("Setting alarm for %02d:%02d %s (\"%s\")...".format(displayH, minute, amPm, label))
    }

    private fun executeTimer(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        val seconds = (payload.timerSeconds ?: 60).coerceAtLeast(1)
        val label = payload.title ?: "MacroBite Timer"

        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_MESSAGE, label)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        val formattedTime = if (seconds >= 60) "${seconds / 60}m ${seconds % 60}s" else "${seconds}s"
        return JarvisDispatchResult.Success("Started countdown timer for $formattedTime (\"$label\")...")
    }

    private fun executeOpenApp(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        val appName = payload.appName?.trim()?.lowercase().orEmpty()
        var pkg = payload.packageName?.trim().orEmpty()

        if (pkg.isBlank()) {
            pkg = when {
                appName.contains("youtube") -> "com.google.android.youtube"
                appName.contains("spotify") -> "com.spotify.music"
                appName.contains("whatsapp") -> "com.whatsapp"
                appName.contains("maps") -> "com.google.android.apps.maps"
                appName.contains("chrome") -> "com.android.chrome"
                appName.contains("instagram") -> "com.instagram.android"
                appName.contains("twitter") || appName.contains(" x") -> "com.twitter.android"
                appName.contains("telegram") -> "org.telegram.messenger"
                appName.contains("netflix") -> "com.netflix.mediaclient"
                appName.contains("uber") -> "com.ubercab"
                else -> ""
            }
        }

        // Special system apps & common apps
        if (appName.contains("youtube")) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return JarvisDispatchResult.Success("Opening YouTube...")
            } else {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                return JarvisDispatchResult.Success("Opening YouTube...")
            }
        }
        if (appName.contains("camera")) {
            val camIntent = Intent("android.media.action.STILL_IMAGE_CAMERA").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(camIntent)
            return JarvisDispatchResult.Success("Opening Camera...")
        }
        if (appName.contains("settings")) {
            val setIntent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(setIntent)
            return JarvisDispatchResult.Success("Opening System Settings...")
        }
        if (appName.contains("clock") || appName.contains("alarm")) {
            val clockIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(clockIntent)
            return JarvisDispatchResult.Success("Opening Alarms & Clock...")
        }

        if (pkg.isNotBlank()) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return JarvisDispatchResult.Success("Opening ${payload.appName ?: pkg}...")
            }
        }

        // If package not installed or not found, try searching in Play Store
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=${Uri.encode(payload.appName ?: pkg)}")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(marketIntent)
            JarvisDispatchResult.Info("App not found locally. Searching on Google Play Store...")
        } catch (e: Exception) {
            JarvisDispatchResult.Error("Could not find or launch application: ${payload.appName ?: pkg}")
        }
    }

    private fun executeMaps(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        val query = payload.query?.trim().orEmpty().ifBlank { "gyms near me" }
        val mapUri = Uri.parse("geo:0,0?q=${Uri.encode(query)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, mapUri).apply {
            setPackage("com.google.android.apps.maps")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(mapIntent)
            JarvisDispatchResult.Success("Navigating to \"$query\" on Google Maps...")
        } catch (e: Exception) {
            // Generic maps / browser fallback
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            JarvisDispatchResult.Success("Searching maps for \"$query\"...")
        }
    }

    private fun executeYouTube(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        val query = payload.query?.trim().orEmpty()
        if (query.isBlank() || query.equals("youtube", ignoreCase = true) || query.equals("open youtube", ignoreCase = true)) {
            val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.youtube")
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                return JarvisDispatchResult.Success("Opening YouTube...")
            } else {
                val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                return JarvisDispatchResult.Success("Opening YouTube...")
            }
        }
        val ytIntent = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(ytIntent)
            JarvisDispatchResult.Success("Searching YouTube for \"$query\"...")
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(fallback)
            JarvisDispatchResult.Success("Opening YouTube search for \"$query\"...")
        }
    }

    private fun executeFlashlight(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return JarvisDispatchResult.Error("Camera hardware not available.")

            val targetState = payload.turnOn ?: !isTorchOn
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val chars = cameraManager.getCameraCharacteristics(id)
                chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return JarvisDispatchResult.Error("No flashlight / LED torch found on this device.")

            cameraManager.setTorchMode(cameraId, targetState)
            isTorchOn = targetState
            val stateStr = if (targetState) "ON 🔦" else "OFF"
            JarvisDispatchResult.Success("Flashlight turned $stateStr")
        } catch (e: Exception) {
            JarvisDispatchResult.Error("Flashlight toggle error: ${e.localizedMessage}")
        }
    }

    fun executeBatteryCheck(context: Context): JarvisDispatchResult {
        return try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            val plugged = batteryStatus?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1) ?: 0
            val plugSource = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast Charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless Pad"
                else -> if (isCharging) "Power Source" else "Battery Discharging"
            }

            val statusMessage = buildString {
                append("🔋 Battery Level: $pct%")
                if (isCharging) {
                    append(" (⚡ Charging via $plugSource)")
                } else {
                    append(" (Discharging)")
                }
            }
            JarvisDispatchResult.Success(statusMessage)
        } catch (e: Exception) {
            JarvisDispatchResult.Error("Could not retrieve battery info: ${e.localizedMessage}")
        }
    }

    private fun executeNote(context: Context, payload: JarvisActionPayload): JarvisDispatchResult {
        val text = listOfNotNull(payload.title, payload.message).joinToString("\n\n")
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, "Save / Share Note").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
        return JarvisDispatchResult.Success("Sharing note...")
    }
}
