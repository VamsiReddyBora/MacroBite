package com.macrobite.app.domain.model

import java.util.Locale
import kotlin.math.roundToInt

fun Float.formatMacroOneDecimal(): String {
    val rounded = (this * 10f).roundToInt() / 10f
    return if (rounded % 1f == 0f) {
        rounded.toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", rounded)
    }
}

data class MealItem(
    val name: String,
    val portion: String = "1 serving",
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fats: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val saturatedFat: Float = 0f,
    val potassium: Float = 0f,
    val cholesterol: Float = 0f,
    val vitaminsAndMinerals: String = "",
    val baseMl: Int? = null,
    val baseCalories: Int? = null,
    val baseProtein: Float? = null,
    val baseCarbs: Float? = null,
    val baseFats: Float? = null
)

data class MealEntry(
    val id: Long = 0,
    val date: String, // format yyyy-MM-dd
    val category: MealCategory,
    val foodName: String,
    val portion: String = "1 serving",
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val saturatedFat: Float = 0f,
    val potassium: Float = 0f,
    val cholesterol: Float = 0f,
    val vitaminsAndMinerals: String = "",
    val photoUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
