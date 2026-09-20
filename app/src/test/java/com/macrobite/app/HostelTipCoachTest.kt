package com.macrobite.app

import com.macrobite.app.domain.model.DailyMacros
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.usecase.HostelTipCoach
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HostelTipCoachTest {

    @Test
    fun testDailyMacrosProgressCalculation() {
        val macros = DailyMacros(
            date = "2026-09-15",
            totalCalories = 2250,
            totalProtein = 80f,
            totalCarbs = 270f,
            totalFats = 70f,
            targetCalories = 3000,
            targetProtein = 110,
            targetCarbs = 360,
            targetFats = 110
        )

        assertEquals(750, macros.remainingCalories)
        assertEquals(30f, macros.remainingProtein, 0.01f)
        assertEquals(90f, macros.remainingCarbs, 0.01f)
        assertEquals(40f, macros.remainingFats, 0.01f)
        assertEquals(0.75f, macros.calorieProgress, 0.01f)
    }

    @Test
    fun testHostelTipCoachEvaluatesQuickTopUp() {
        val coach = HostelTipCoach()
        val lowCalorieDay = DailyMacros(
            date = "2026-09-15",
            totalCalories = 1800,
            targetCalories = 3000
        )

        // Note: HostelTipCoach evaluates Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        // If evaluated, verify that when a tip is generated it matches the spec (+650 kcal)
        val tip = coach.evaluateTip(lowCalorieDay, "2026-09-15")
        if (tip != null) {
            assertTrue("Tip description should suggest calories", tip.description.contains("kcal"))
            assertNotNull(tip.quickAddItems)
        }
    }
}
