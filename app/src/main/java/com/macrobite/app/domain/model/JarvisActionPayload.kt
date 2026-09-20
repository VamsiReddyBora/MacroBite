package com.macrobite.app.domain.model

import com.google.gson.annotations.SerializedName

enum class JarvisActionType {
    @SerializedName("CALL") CALL,
    @SerializedName("WHATSAPP") WHATSAPP,
    @SerializedName("SMS") SMS,
    @SerializedName("ALARM") ALARM,
    @SerializedName("TIMER") TIMER,
    @SerializedName("OPEN_APP") OPEN_APP,
    @SerializedName("MAPS") MAPS,
    @SerializedName("YOUTUBE") YOUTUBE,
    @SerializedName("FLASHLIGHT") FLASHLIGHT,
    @SerializedName("BATTERY") BATTERY,
    @SerializedName("NOTE") NOTE;

    companion object {
        fun fromString(value: String?): JarvisActionType {
            if (value == null) return NOTE
            val normalized = value.trim().uppercase()
            return entries.firstOrNull { it.name == normalized } ?: when {
                normalized.contains("CALL") || normalized.contains("PHONE") -> CALL
                normalized.contains("WHATSAPP") -> WHATSAPP
                normalized.contains("SMS") || normalized.contains("TEXT") -> SMS
                normalized.contains("ALARM") -> ALARM
                normalized.contains("TIMER") -> TIMER
                normalized.contains("APP") || normalized.contains("LAUNCH") -> OPEN_APP
                normalized.contains("MAP") || normalized.contains("NAVIGAT") -> MAPS
                normalized.contains("YOUTUBE") || normalized.contains("VIDEO") -> YOUTUBE
                normalized.contains("FLASH") || normalized.contains("TORCH") -> FLASHLIGHT
                normalized.contains("BATTERY") -> BATTERY
                else -> NOTE
            }
        }
    }
}

data class ContactCandidate(
    @SerializedName("name") val name: String,
    @SerializedName("number") val number: String
)

data class JarvisActionPayload(
    @SerializedName("action_type") val actionType: JarvisActionType,
    @SerializedName("title") val title: String? = null,
    @SerializedName("contact_name") val contactName: String? = null,
    @SerializedName("phone_number") val phoneNumber: String? = null,
    @SerializedName("message") val message: String? = null,
    @SerializedName("time_hour") val timeHour: Int? = null,
    @SerializedName("time_minute") val timeMinute: Int? = null,
    @SerializedName("timer_seconds") val timerSeconds: Int? = null,
    @SerializedName("query") val query: String? = null,
    @SerializedName("app_name") val appName: String? = null,
    @SerializedName("package_name") val packageName: String? = null,
    @SerializedName("turn_on") val turnOn: Boolean? = true,
    @SerializedName("auto_execute") val autoExecute: Boolean = false,
    @SerializedName("candidates") val candidates: List<ContactCandidate>? = null
) {
    val hasMultipleCandidates: Boolean get() = !candidates.isNullOrEmpty()

    val displayTitle: String
        get() = when (actionType) {
            JarvisActionType.CALL -> {
                if (hasMultipleCandidates) "Multiple Contacts Found"
                else if (!contactName.isNullOrBlank()) "Call $contactName"
                else if (!phoneNumber.isNullOrBlank()) "Call $phoneNumber"
                else "Phone Call"
            }
            JarvisActionType.WHATSAPP -> if (!contactName.isNullOrBlank()) "WhatsApp $contactName" else "WhatsApp Message"
            JarvisActionType.SMS -> if (!contactName.isNullOrBlank()) "Text $contactName" else "SMS Message"
            JarvisActionType.ALARM -> {
                val h = timeHour ?: 7
                val m = timeMinute ?: 0
                val amPm = if (h < 12) "AM" else "PM"
                val displayHour = if (h == 0) 12 else if (h > 12) h - 12 else h
                String.format("Alarm for %02d:%02d %s", displayHour, m, amPm)
            }
            JarvisActionType.TIMER -> {
                val sec = timerSeconds ?: 60
                if (sec >= 60) "Timer for ${sec / 60} min" else "Timer for ${sec}s"
            }
            JarvisActionType.OPEN_APP -> "Open ${appName ?: "App"}"
            JarvisActionType.MAPS -> if (!query.isNullOrBlank()) "Directions: $query" else "Google Maps"
            JarvisActionType.YOUTUBE -> if (!query.isNullOrBlank()) "YouTube: $query" else "YouTube"
            JarvisActionType.FLASHLIGHT -> if (turnOn != false) "Turn On Flashlight" else "Turn Off Flashlight"
            JarvisActionType.BATTERY -> "Battery Status"
            JarvisActionType.NOTE -> title ?: "Quick Note"
        }

    val displaySubtitle: String
        get() = when (actionType) {
            JarvisActionType.CALL -> {
                if (hasMultipleCandidates) "${candidates!!.size} contacts match - select who to call"
                else if (!phoneNumber.isNullOrBlank()) "Number: $phoneNumber"
                else "Search contact & dial"
            }
            JarvisActionType.WHATSAPP -> message?.takeIf { it.isNotBlank() }?.let { "\"$it\"" } ?: (phoneNumber ?: "Send via WhatsApp")
            JarvisActionType.SMS -> message?.takeIf { it.isNotBlank() }?.let { "\"$it\"" } ?: (phoneNumber ?: "Send text")
            JarvisActionType.ALARM -> title ?: "Wake up & hit macros"
            JarvisActionType.TIMER -> title ?: "Countdown reminder"
            JarvisActionType.OPEN_APP -> packageName ?: "Launch application"
            JarvisActionType.MAPS -> query ?: "Find nearby locations"
            JarvisActionType.YOUTUBE -> query ?: "Watch fitness & nutrition videos"
            JarvisActionType.FLASHLIGHT -> "Device LED torch"
            JarvisActionType.BATTERY -> "System power & charge level"
            JarvisActionType.NOTE -> message ?: ""
        }
}
