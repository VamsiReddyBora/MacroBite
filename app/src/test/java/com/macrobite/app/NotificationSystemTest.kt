package com.macrobite.app

import com.macrobite.app.domain.model.NotificationPreferences
import com.macrobite.app.notification.NotificationScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class NotificationSystemTest {

    @Test
    fun defaultNotificationPreferences_areEnabledWithCorrectTimes() {
        val prefs = NotificationPreferences()
        assertTrue(prefs.enabled)
        assertTrue(prefs.breakfastEnabled)
        assertEquals("08:30", prefs.breakfastTime)
        assertTrue(prefs.lunchEnabled)
        assertEquals("13:00", prefs.lunchTime)
        assertTrue(prefs.snackEnabled)
        assertEquals("17:30", prefs.snackTime)
        assertTrue(prefs.dinnerEnabled)
        assertEquals("20:30", prefs.dinnerTime)
        assertTrue(prefs.dailySummaryEnabled)
        assertEquals("21:30", prefs.dailySummaryTime)
        assertTrue(prefs.inAppNotificationsEnabled)

        assertTrue(prefs.isMealEnabled(NotificationPreferences.MEAL_BREAKFAST))
        assertTrue(prefs.isMealEnabled(NotificationPreferences.MEAL_LUNCH))
        assertTrue(prefs.isMealEnabled(NotificationPreferences.MEAL_SNACK))
        assertTrue(prefs.isMealEnabled(NotificationPreferences.MEAL_DINNER))
        assertTrue(prefs.isMealEnabled(NotificationPreferences.SUMMARY_DAILY))
    }

    @Test
    fun masterToggleDisabled_disablesAllMealReminders() {
        val prefs = NotificationPreferences(enabled = false)
        assertFalse(prefs.isMealEnabled(NotificationPreferences.MEAL_BREAKFAST))
        assertFalse(prefs.isMealEnabled(NotificationPreferences.MEAL_LUNCH))
        assertFalse(prefs.isMealEnabled(NotificationPreferences.MEAL_SNACK))
        assertFalse(prefs.isMealEnabled(NotificationPreferences.MEAL_DINNER))
        assertFalse(prefs.isMealEnabled(NotificationPreferences.SUMMARY_DAILY))
    }

    @Test
    fun individualMealToggles_areRespected() {
        val prefs = NotificationPreferences(
            enabled = true,
            breakfastEnabled = true,
            lunchEnabled = false,
            snackEnabled = true,
            dinnerEnabled = false
        )
        assertTrue(prefs.isMealEnabled(NotificationPreferences.MEAL_BREAKFAST))
        assertFalse(prefs.isMealEnabled(NotificationPreferences.MEAL_LUNCH))
        assertTrue(prefs.isMealEnabled(NotificationPreferences.MEAL_SNACK))
        assertFalse(prefs.isMealEnabled(NotificationPreferences.MEAL_DINNER))
    }

    @Test
    fun calculateNextTriggerMillis_isAlwaysInFuture() {
        val now = System.currentTimeMillis()
        val trigger = NotificationScheduler.calculateNextTriggerMillis("08:30")
        assertTrue("Trigger time must be strictly after current time", trigger > now)
    }

    @Test
    fun calculateNextTriggerMillis_forceTomorrow_isMoreThan12HoursAhead() {
        val now = Calendar.getInstance()
        val triggerTomorrow = NotificationScheduler.calculateNextTriggerMillis("08:30", forceTomorrow = true)
        val calTomorrow = Calendar.getInstance().apply { timeInMillis = triggerTomorrow }
        assertTrue(triggerTomorrow > now.timeInMillis)
        assertTrue(calTomorrow.get(Calendar.DAY_OF_YEAR) != now.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun calculateNextTriggerMillis_matchesConfiguredHourAndMinute() {
        val trigger = NotificationScheduler.calculateNextTriggerMillis("14:45")
        val calendar = Calendar.getInstance().apply { timeInMillis = trigger }
        assertEquals(14, calendar.get(Calendar.HOUR_OF_DAY))
        assertEquals(45, calendar.get(Calendar.MINUTE))
        assertEquals(0, calendar.get(Calendar.SECOND))
    }
}
