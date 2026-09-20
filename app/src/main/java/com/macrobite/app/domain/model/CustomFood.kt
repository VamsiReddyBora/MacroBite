package com.macrobite.app.domain.model

data class CustomFood(
    val id: Long = 0,
    val name: String,
    val portion: String = "1 serving",
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float
)
