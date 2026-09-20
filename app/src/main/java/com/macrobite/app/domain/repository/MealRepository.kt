package com.macrobite.app.domain.repository

import com.macrobite.app.domain.model.MealEntry
import kotlinx.coroutines.flow.Flow

interface MealRepository {
    fun getMealsForDate(date: String): Flow<List<MealEntry>>
    fun getMealsBetweenDates(startDate: String, endDate: String): Flow<List<MealEntry>>
    fun getAllMeals(): Flow<List<MealEntry>>
    suspend fun insertMeal(meal: MealEntry): Long
    suspend fun insertMeals(meals: List<MealEntry>): List<Long>
    suspend fun updateMeal(meal: MealEntry)
    suspend fun deleteMeal(mealId: Long)
    suspend fun getMealById(mealId: Long): MealEntry?
}
