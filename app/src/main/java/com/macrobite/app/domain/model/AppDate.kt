package com.macrobite.app.domain.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class AppDate(
    val year: Int,
    val month: Int, // 1-12
    val dayOfMonth: Int
) : Comparable<AppDate> {

    fun toIsoString(): String {
        return "%04d-%02d-%02d".format(Locale.US, year, month, dayOfMonth)
    }

    fun minusDays(days: Int): AppDate {
        val cal = toCalendar()
        cal.add(Calendar.DAY_OF_YEAR, -days)
        return fromCalendar(cal)
    }

    fun plusDays(days: Int): AppDate {
        val cal = toCalendar()
        cal.add(Calendar.DAY_OF_YEAR, days)
        return fromCalendar(cal)
    }

    fun toDisplayTitle(today: AppDate = today()): String {
        val dayMonthFmt = SimpleDateFormat("d MMM", Locale.US)
        val dateObj = toCalendar().time
        val diffDays = toEpochDay() - today.toEpochDay()
        return when (diffDays) {
            0L -> "Today, ${dayMonthFmt.format(dateObj)}"
            -1L -> "Yesterday, ${dayMonthFmt.format(dateObj)}"
            1L -> "Tomorrow, ${dayMonthFmt.format(dateObj)}"
            else -> {
                val fullFmt = SimpleDateFormat("EEE, d MMM", Locale.US)
                fullFmt.format(dateObj)
            }
        }
    }

    fun toDayOfWeekShort(): String {
        val fmt = SimpleDateFormat("EEE", Locale.US)
        return fmt.format(toCalendar().time)
    }

    private fun toCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal.clear()
        cal.set(year, month - 1, dayOfMonth)
        return cal
    }

    fun toEpochDay(): Long {
        return toCalendar().timeInMillis / 86400000L
    }

    override fun compareTo(other: AppDate): Int {
        return toEpochDay().compareTo(other.toEpochDay())
    }

    override fun toString(): String = toIsoString()

    companion object {
        fun today(): AppDate {
            val cal = Calendar.getInstance()
            return AppDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun todayIso(): String = today().toIsoString()

        fun fromCalendar(cal: Calendar): AppDate {
            return AppDate(
                year = cal.get(Calendar.YEAR),
                month = cal.get(Calendar.MONTH) + 1,
                dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
            )
        }

        fun fromIso(iso: String): AppDate {
            return try {
                val parts = iso.split("-")
                if (parts.size == 3) {
                    AppDate(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                } else {
                    today()
                }
            } catch (e: Exception) {
                today()
            }
        }
    }
}
