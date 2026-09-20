package com.macrobite.app.domain.usecase

import com.macrobite.app.domain.model.DailyMacros
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

data class HostelTip(
    val title: String,
    val description: String,
    val quickAddItems: List<MealEntry>? = null
)

@Singleton
class HostelTipCoach @Inject constructor() {

    fun evaluateTip(dailyMacros: DailyMacros, currentDate: String): HostelTip? {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val remainingCal = dailyMacros.remainingCalories
        val remainingProt = dailyMacros.remainingProtein

        // Condition 1: Late evening requirement - after 7 PM (19:00) and large calorie deficit (> 1000 kcal)
        if (hour >= 19 && remainingCal > 1000) {
            val quickItems = listOf(
                MealEntry(
                    date = currentDate,
                    category = MealCategory.SNACKS,
                    foodName = "1 glass whole milk + 2 tbsp peanut butter + banana",
                    portion = "1 serving",
                    calories = 650,
                    protein = 20f,
                    carbs = 76f,
                    fats = 26f
                )
            )
            return HostelTip(
                title = "Evening Calorie Top-up",
                description = "1 glass whole milk + 2 tbsp peanut butter + banana (+650 kcal)",
                quickAddItems = quickItems
            )
        }

        // Condition 2: High protein deficit late in the evening (after 7 PM and > 40g protein left)
        if (hour >= 19 && remainingProt > 40f) {
            val quickItems = listOf(
                MealEntry(
                    date = currentDate,
                    category = MealCategory.DINNER,
                    foodName = "4 Boiled Eggs (or 100g Paneer)",
                    portion = "4 eggs",
                    calories = 312,
                    protein = 24f,
                    carbs = 4f,
                    fats = 20f
                )
            )
            return HostelTip(
                title = "Evening Protein Boost",
                description = "4 Boiled eggs or 100g Paneer (+24g P, 312 kcal)",
                quickAddItems = quickItems
            )
        }

        // Mid-day reminder removed per user request
        return null
    }
}
