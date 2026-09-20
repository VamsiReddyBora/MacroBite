package com.macrobite.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.macrobite.app.data.local.entity.MealEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date ORDER BY timestamp ASC")
    fun getMealsForDate(date: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, timestamp ASC")
    fun getMealsBetweenDates(startDate: String, endDate: String): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals ORDER BY date DESC, timestamp DESC")
    fun getAllMeals(): Flow<List<MealEntity>>

    @Query("SELECT * FROM meals WHERE id = :id LIMIT 1")
    suspend fun getMealById(id: Long): MealEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeals(meals: List<MealEntity>): List<Long>

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMealById(id: Long)
}
