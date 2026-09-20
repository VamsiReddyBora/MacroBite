package com.macrobite.app.domain.model

data class UserTargets(
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fats: Int = 0
)

data class WeightLog(
    val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val weightKg: Float,
    val timestamp: Long = System.currentTimeMillis()
)

data class PresetFood(
    val name: String,
    val portion: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val category: MealCategory = MealCategory.SNACKS,
    val tag: String = "Hostel Staple"
)

data class DailyMacros(
    val date: String,
    val totalCalories: Int = 0,
    val totalProtein: Float = 0f,
    val totalCarbs: Float = 0f,
    val totalFats: Float = 0f,
    val targetCalories: Int = 0,
    val targetProtein: Int = 0,
    val targetCarbs: Int = 0,
    val targetFats: Int = 0,
    val meals: List<MealEntry> = emptyList()
) {
    val remainingCalories: Int get() = targetCalories - totalCalories
    val remainingProtein: Float get() = targetProtein.toFloat() - totalProtein
    val remainingCarbs: Float get() = targetCarbs.toFloat() - totalCarbs
    val remainingFats: Float get() = targetFats.toFloat() - totalFats

    val calorieProgress: Float get() = if (targetCalories > 0) (totalCalories.toFloat() / targetCalories).coerceIn(0f, 1f) else 0f
    val proteinProgress: Float get() = if (targetProtein > 0) (totalProtein / targetProtein.toFloat()).coerceIn(0f, 1f) else 0f
    val carbsProgress: Float get() = if (targetCarbs > 0) (totalCarbs / targetCarbs.toFloat()).coerceIn(0f, 1f) else 0f
    val fatsProgress: Float get() = if (targetFats > 0) (totalFats / targetFats.toFloat()).coerceIn(0f, 1f) else 0f
}
