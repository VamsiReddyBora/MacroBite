package com.macrobite.app.ui.chat.jarvis

import com.macrobite.app.domain.model.JarvisActionPayload
import com.macrobite.app.domain.model.JarvisActionType
import java.util.regex.Pattern

data class AliasCommand(val alias: String, val target: String)

object LocalJarvisActionParser {

    fun parseAliasCommand(input: String): AliasCommand? {
        val trimmed = input.trim()
        if (trimmed.length < 5) return null

        // 1. Explicit key-value forms: "set alias dad = daddy", "alias mom to mummy", "my dad is daddy"
        val pattern = Pattern.compile(
            """^(?:set\s+alias|add\s+alias|alias|my)\s+([a-zA-Z0-9\s]+?)\s+(?:=|is|to|as)\s+([a-zA-Z0-9\s]+)$""",
            Pattern.CASE_INSENSITIVE
        )
        val matcher = pattern.matcher(trimmed)
        if (matcher.find()) {
            val a = matcher.group(1)?.trim().orEmpty()
            val t = matcher.group(2)?.trim().orEmpty()
            if (a.isNotBlank() && t.isNotBlank()) {
                return AliasCommand(a, t)
            }
        }

        // 2. Short form: "alias dad daddy"
        val shortPattern = Pattern.compile("""^alias\s+([a-zA-Z0-9]+)\s+([a-zA-Z0-9]+)$""", Pattern.CASE_INSENSITIVE)
        val shortMatcher = shortPattern.matcher(trimmed)
        if (shortMatcher.find()) {
            val a = shortMatcher.group(1)?.trim().orEmpty()
            val t = shortMatcher.group(2)?.trim().orEmpty()
            if (a.isNotBlank() && t.isNotBlank()) {
                return AliasCommand(a, t)
            }
        }

        return null
    }

    fun parse(input: String): JarvisActionPayload? {
        val trimmed = input.trim()
        if (trimmed.length < 3) return null

        // Strip polite conversational prefixes like "please", "can you", "jarvis"
        val clean = trimmed
            .replace(Regex("""^(?:please\s+|can you\s+|could you\s+|jarvis\s+|raaya\s+|assistant\s+|hey\s+|hi\s+)+""", RegexOption.IGNORE_CASE), "")
            .trim()

        val lower = clean.lowercase()

        // 1. Flashlight / Torch
        if (lower.contains("flashlight") || lower.contains("torch")) {
            val turnOn = !lower.contains("off") && !lower.contains("disable") && !lower.contains("stop")
            return JarvisActionPayload(
                actionType = JarvisActionType.FLASHLIGHT,
                title = if (turnOn) "Turn On Flashlight" else "Turn Off Flashlight",
                turnOn = turnOn
            )
        }

        // 2. Battery status
        if (lower.contains("battery") && (lower.contains("check") || lower.contains("status") || lower.contains("level") || lower.contains("percent") || lower.contains("how much") || lower == "battery")) {
            return JarvisActionPayload(
                actionType = JarvisActionType.BATTERY,
                title = "Check Battery"
            )
        }

        // 3. YouTube search & direct open
        if (lower == "youtube" || lower == "open youtube" || lower == "open the youtube" || lower == "launch youtube") {
            return JarvisActionPayload(
                actionType = JarvisActionType.YOUTUBE,
                title = "Open YouTube",
                appName = "youtube"
            )
        }
        val ytPattern = Pattern.compile("""(?:search|play|watch|find)\s+(.+?)\s+on\s+youtube""", Pattern.CASE_INSENSITIVE)
        val ytMatcher = ytPattern.matcher(clean)
        if (ytMatcher.find()) {
            val query = ytMatcher.group(1)?.trim().orEmpty()
            return JarvisActionPayload(
                actionType = JarvisActionType.YOUTUBE,
                title = "Search YouTube: $query",
                query = query
            )
        }
        if (lower.startsWith("youtube ") || lower.startsWith("play on youtube ")) {
            val query = clean.substringAfter("youtube", "").trim()
            if (query.isNotBlank()) {
                return JarvisActionPayload(
                    actionType = JarvisActionType.YOUTUBE,
                    title = "Search YouTube: $query",
                    query = query
                )
            }
        }

        // 4. Maps / Navigation
        val mapsPattern = Pattern.compile("""(?:navigate to|directions to|route to|how to reach|where is|find nearby)\s+(.+)""", Pattern.CASE_INSENSITIVE)
        val mapsMatcher = mapsPattern.matcher(clean)
        if (mapsMatcher.find()) {
            val place = mapsMatcher.group(1)?.trim().orEmpty()
            return JarvisActionPayload(
                actionType = JarvisActionType.MAPS,
                title = "Navigate to $place",
                query = place
            )
        }

        // 5. Timer: "set a timer for 13 min", "timer 13 min", "13 min timer", "set timer of 5 mins"
        val timerPattern1 = Pattern.compile(
            """(?:set|start|begin|create)?\s*(?:a\s+)?timer(?:\s+(?:for|of))?\s+(\d+)\s*(min(?:ute)?s?|sec(?:ond)?s?|hours?|hr|m|s)?""",
            Pattern.CASE_INSENSITIVE
        )
        val timerMatcher1 = timerPattern1.matcher(clean)
        if (timerMatcher1.find()) {
            val num = timerMatcher1.group(1)?.toIntOrNull() ?: 1
            val unitRaw = timerMatcher1.group(2)?.lowercase().orEmpty()
            val unit = if (unitRaw.isBlank()) "mins" else unitRaw
            val seconds = when {
                unit.startsWith("h") -> num * 3600
                unit.startsWith("s") -> num
                else -> num * 60
            }
            return JarvisActionPayload(
                actionType = JarvisActionType.TIMER,
                title = "Timer for $num $unit",
                timerSeconds = seconds
            )
        }
        val timerPattern2 = Pattern.compile(
            """(\d+)\s*(min(?:ute)?s?|sec(?:ond)?s?|hours?|hr|m|s)\s+timer""",
            Pattern.CASE_INSENSITIVE
        )
        val timerMatcher2 = timerPattern2.matcher(clean)
        if (timerMatcher2.find()) {
            val num = timerMatcher2.group(1)?.toIntOrNull() ?: 1
            val unit = timerMatcher2.group(2)?.lowercase().orEmpty()
            val seconds = when {
                unit.startsWith("h") -> num * 3600
                unit.startsWith("s") -> num
                else -> num * 60
            }
            return JarvisActionPayload(
                actionType = JarvisActionType.TIMER,
                title = "Timer for $num $unit",
                timerSeconds = seconds
            )
        }

        // 6. Alarm: "set an alarm for 7 am", "wake me up at 6:30 am", "alarm 7 pm"
        val alarmPattern = Pattern.compile(
            """(?:(?:set|start|create)?\s*(?:an?\s+)?alarm(?:\s+(?:for|at))?|wake\s+me\s+up(?:\s+at)?)\s+(\d{1,2})(?::(\d{2}))?\s*(am|pm)?""",
            Pattern.CASE_INSENSITIVE
        )
        val alarmMatcher = alarmPattern.matcher(clean)
        if (alarmMatcher.find()) {
            var hour = alarmMatcher.group(1)?.toIntOrNull() ?: 7
            val minute = alarmMatcher.group(2)?.toIntOrNull() ?: 0
            val amPm = alarmMatcher.group(3)?.lowercase()
            if (amPm == "pm" && hour < 12) hour += 12
            if (amPm == "am" && hour == 12) hour = 0
            return JarvisActionPayload(
                actionType = JarvisActionType.ALARM,
                title = "Wake Up Alarm",
                timeHour = hour,
                timeMinute = minute
            )
        }

        // 7. Phone call: "call dad", "call someone", "call to john", "dial 9876543210", "make a call to mom"
        val callPattern = Pattern.compile(
            """(?:make\s+a\s+call\s+to|call\s+to|call|phone|dial|ring)\s+([a-zA-Z0-9+\s]+)""",
            Pattern.CASE_INSENSITIVE
        )
        val callMatcher = callPattern.matcher(clean)
        if (callMatcher.find() && !lower.contains("alarm") && !lower.contains("timer")) {
            var target = callMatcher.group(1)?.trim().orEmpty()
            if (target.lowercase().startsWith("to ")) {
                target = target.substring(3).trim()
            }
            target = target.replace(Regex("""(?:\s+please|\s+now|\s+urgently)$""", RegexOption.IGNORE_CASE), "").trim()
            if (target.isNotBlank() && target.length > 1) {
                val isNum = target.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }
                return JarvisActionPayload(
                    actionType = JarvisActionType.CALL,
                    title = "Call $target",
                    contactName = if (!isNum) target else null,
                    phoneNumber = if (isNum) target else null
                )
            }
        }

        // 8. WhatsApp message
        val waPattern = Pattern.compile("""(?:whatsapp|send whatsapp to|message on whatsapp to)\s+([a-zA-Z0-9+\s]+?)(?:\s*(?::|that|saying)\s*(.+))?$""", Pattern.CASE_INSENSITIVE)
        val waMatcher = waPattern.matcher(trimmed)
        if (waMatcher.find()) {
            val contact = waMatcher.group(1)?.trim().orEmpty()
            val msg = waMatcher.group(2)?.trim()
            if (contact.isNotBlank()) {
                val isNum = contact.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }
                return JarvisActionPayload(
                    actionType = JarvisActionType.WHATSAPP,
                    title = "WhatsApp $contact",
                    contactName = if (!isNum) contact else null,
                    phoneNumber = if (isNum) contact else null,
                    message = msg
                )
            }
        }

        // 9. SMS message
        val smsPattern = Pattern.compile("""(?:sms|send sms to|text)\s+([a-zA-Z0-9+\s]+?)(?:\s*(?::|that|saying)\s*(.+))?$""", Pattern.CASE_INSENSITIVE)
        val smsMatcher = smsPattern.matcher(trimmed)
        if (smsMatcher.find()) {
            val contact = smsMatcher.group(1)?.trim().orEmpty()
            val msg = smsMatcher.group(2)?.trim()
            if (contact.isNotBlank()) {
                val isNum = contact.all { it.isDigit() || it == '+' || it == ' ' || it == '-' }
                return JarvisActionPayload(
                    actionType = JarvisActionType.SMS,
                    title = "Text $contact",
                    contactName = if (!isNum) contact else null,
                    phoneNumber = if (isNum) contact else null,
                    message = msg
                )
            }
        }

        // 10. Open app
        val openAppPattern = Pattern.compile(
            """^(?:please\s+|can you\s+|could you\s+|jarvis\s+|raaya\s+)?(?:open|launch|start)\s+(?:the\s+)?([a-zA-Z0-9\s]+?)(?:\s+app)?$""",
            Pattern.CASE_INSENSITIVE
        )
        val openAppMatcher = openAppPattern.matcher(trimmed)
        if (openAppMatcher.find() && !lower.contains("chat") && !lower.contains("meal") && !lower.contains("food") && !lower.contains("alarm") && !lower.contains("timer")) {
            val app = openAppMatcher.group(1)?.trim().orEmpty()
            if (app.isNotBlank() && app.length >= 2) {
                if (app.equals("youtube", ignoreCase = true)) {
                    return JarvisActionPayload(
                        actionType = JarvisActionType.YOUTUBE,
                        title = "Open YouTube",
                        appName = "youtube"
                    )
                }
                return JarvisActionPayload(
                    actionType = JarvisActionType.OPEN_APP,
                    title = "Open ${app.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }}",
                    appName = app
                )
            }
        }

        return null
    }
}
