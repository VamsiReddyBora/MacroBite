package com.macrobite.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.macrobite.app.data.local.entity.CustomFoodEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomFoodDao {
    @Query("SELECT * FROM custom_foods ORDER BY id ASC")
    fun getAllCustomFoods(): Flow<List<CustomFoodEntity>>

    @Query("SELECT * FROM custom_foods ORDER BY id ASC")
    suspend fun getAllCustomFoodsDirect(): List<CustomFoodEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFood(food: CustomFoodEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomFoods(foods: List<CustomFoodEntity>): List<Long>

    @Query("DELETE FROM custom_foods WHERE id = :id")
    suspend fun deleteCustomFoodById(id: Long)

    @Query("SELECT COUNT(*) FROM custom_foods")
    suspend fun getCount(): Int
}
