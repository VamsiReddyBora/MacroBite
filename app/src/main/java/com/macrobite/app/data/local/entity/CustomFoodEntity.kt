package com.macrobite.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.macrobite.app.domain.model.CustomFood

@Entity(tableName = "custom_foods")
data class CustomFoodEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val portion: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): CustomFood {
        return CustomFood(
            id = id,
            name = name,
            portion = portion,
            calories = calories,
            protein = protein,
            carbs = carbs,
            fats = fats
        )
    }

    companion object {
        fun fromDomain(food: CustomFood): CustomFoodEntity {
            return CustomFoodEntity(
                id = food.id,
                name = food.name,
                portion = food.portion,
                calories = food.calories,
                protein = food.protein,
                carbs = food.carbs,
                fats = food.fats
            )
        }
    }
}
