package com.macrobite.app

import com.macrobite.app.data.scanner.ScannedFoodResult
import com.macrobite.app.domain.model.DailyActivityData
import com.macrobite.app.domain.model.DailyMacros
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceAndBarcodeScannerTest {

    @Test
    fun testScannedFoodResultCreationAndMappingToMealEntry() {
        val scanned = ScannedFoodResult(
            barcode = "8901030865412",
            name = "Greek Yogurt High Protein",
            brand = "Epigamia",
            servingSize = "100g",
            calories = 110,
            protein = 8.5f,
            carbs = 9.0f,
            fats = 4.0f,
            fiber = 0.5f,
            sugar = 6.0f,
            sodium = 45f,
            imageUrl = "https://example.com/yogurt.jpg"
        )

        assertEquals("Greek Yogurt High Protein", scanned.name)
        assertEquals("Epigamia", scanned.brand)
        assertEquals(110, scanned.calories)
        assertEquals(8.5f, scanned.protein, 0.01f)

        // Convert to MealEntry
        val entry = MealEntry(
            date = "2026-09-18",
            category = MealCategory.SNACKS,
            foodName = scanned.name,
            portion = scanned.servingSize,
            calories = scanned.calories,
            protein = scanned.protein,
            carbs = scanned.carbs,
            fats = scanned.fats,
            fiber = scanned.fiber ?: 0f,
            sugar = scanned.sugar ?: 0f,
            sodium = scanned.sodium ?: 0f
        )

        assertEquals("2026-09-18", entry.date)
        assertEquals(MealCategory.SNACKS, entry.category)
        assertEquals("Greek Yogurt High Protein", entry.foodName)
        assertEquals(110, entry.calories)
        assertEquals(8.5f, entry.protein, 0.01f)
        assertEquals(0.5f, entry.fiber, 0.01f)
    }

    @Test
    fun testNetCaloriesAndStepBurnAllowanceCalculation() {
        val dailyMacros = DailyMacros(
            date = "2026-09-18",
            totalCalories = 1850,
            totalProtein = 135f,
            totalCarbs = 180f,
            totalFats = 55f,
            targetCalories = 2000
        )

        val activityData = DailyActivityData(
            steps = 8400L,
            caloriesBurned = 380,
            distanceKm = 5.8f,
            isConnected = true
        )

        val eaten = dailyMacros.totalCalories
        val burned = activityData.caloriesBurned
        val netCalories = eaten - burned

        assertEquals(1850, eaten)
        assertEquals(380, burned)
        assertEquals(1470, netCalories)

        // Allowance earned from footsteps (1 kcal per ~20 steps)
        val stepAllowance = (activityData.steps / 20).toInt()
        assertEquals(420, stepAllowance)

        val adjustedTarget = dailyMacros.targetCalories + stepAllowance
        assertEquals(2420, adjustedTarget)

        val remainingBudget = adjustedTarget - netCalories
        assertEquals(950, remainingBudget)
        assertTrue("Net intake is within adjusted active budget", netCalories < adjustedTarget)
    }

    @Test
    fun testMinionBananaLanguagePhrases() {
        val minionPhrases = listOf(
            "Bello!",
            "Banana! 🍌",
            "Tulaliloo ti amo!",
            "Tank yu!",
            "Poopaye!",
            "Bi-do bi-do!"
        )

        for (phrase in minionPhrases) {
            assertNotNull(phrase)
            assertTrue(phrase.isNotEmpty())
        }
    }

    @Test
    fun testMinionSoundResourceIdsExist() {
        assertTrue(com.macrobite.app.R.raw.minion_hello > 0)
        assertTrue(com.macrobite.app.R.raw.minion_banana > 0)
        assertTrue(com.macrobite.app.R.raw.minion_tulaliloo > 0)
        assertTrue(com.macrobite.app.R.raw.minion_hehe > 0)
        assertTrue(com.macrobite.app.R.raw.minion_yay > 0)
        assertTrue(com.macrobite.app.R.raw.minion_tada > 0)
    }

    @Test
    fun testMinionesePhoneticAdaptations() {
        fun sanitize(input: String): String {
            var text = input
                .replace(Regex("```[\\s\\S]*?```"), "")
                .replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
                .replace(Regex("[#*_•~`]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
            return text
                .replace(Regex("(?i)\\bhello\\b"), "Bello")
                .replace(Regex("(?i)\\bgoodbye\\b"), "Poopaye")
                .replace(Regex("(?i)\\bthank you\\b"), "Tank yu")
                .replace(Regex("(?i)\\bi love you\\b"), "Tulaliloo ti amo")
                .replace(Regex("(?i)\\bice cream\\b"), "Gelato")
                .replace(Regex("(?i)\\bcalories\\b"), "calori-na")
        }

        assertEquals("Bello my friend", sanitize("Hello my friend"))
        assertEquals("Poopaye for now", sanitize("Goodbye for now"))
        assertEquals("Tank yu so much", sanitize("Thank you so much"))
        assertEquals("Tulaliloo ti amo", sanitize("I love you"))
        assertEquals("Sweet Gelato with 150 calori-na", sanitize("Sweet ice cream with 150 calories"))
    }
}
