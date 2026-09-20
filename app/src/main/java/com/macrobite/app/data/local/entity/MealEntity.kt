package com.macrobite.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val category: String,
    val foodName: String,
    val portion: String,
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
) {
    fun toDomain(): MealEntry {
        return MealEntry(
            id = id,
            date = date,
            category = MealCategory.fromString(category),
            foodName = foodName,
            portion = portion,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fats = fats,
            fiber = fiber,
            sugar = sugar,
            sodium = sodium,
            saturatedFat = saturatedFat,
            potassium = potassium,
            cholesterol = cholesterol,
            vitaminsAndMinerals = vitaminsAndMinerals,
            photoUri = photoUri,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(entry: MealEntry): MealEntity {
            return MealEntity(
                id = entry.id,
                date = entry.date,
                category = entry.category.name,
                foodName = entry.foodName,
                portion = entry.portion,
                calories = entry.calories,
                protein = entry.protein,
                carbs = entry.carbs,
                fats = entry.fats,
                fiber = entry.fiber,
                sugar = entry.sugar,
                sodium = entry.sodium,
                saturatedFat = entry.saturatedFat,
                potassium = entry.potassium,
                cholesterol = entry.cholesterol,
                vitaminsAndMinerals = entry.vitaminsAndMinerals,
                photoUri = entry.photoUri,
                timestamp = entry.timestamp
            )
        }
    }
}
