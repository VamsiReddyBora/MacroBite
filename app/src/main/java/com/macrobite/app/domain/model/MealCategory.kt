package com.macrobite.app.domain.model

enum class MealCategory(val displayName: String) {
    BREAKFAST("Breakfast"),
    LUNCH("Lunch"),
    SNACKS("Snacks"),
    DINNER("Dinner");

    companion object {
        fun fromString(value: String): MealCategory {
            return entries.find { it.name.equals(value, ignoreCase = true) || it.displayName.equals(value, ignoreCase = true) }
                ?: SNACKS
        }
    }
}
