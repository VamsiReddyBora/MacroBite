package com.macrobite.app.domain.repository

import com.macrobite.app.domain.model.CustomFood
import kotlinx.coroutines.flow.Flow

interface CustomFoodRepository {
    fun getAllCustomFoods(): Flow<List<CustomFood>>
    suspend fun insertCustomFood(food: CustomFood): Long
    suspend fun deleteCustomFood(foodId: Long)
    suspend fun seedDefaultsIfNeeded()
}
