package com.macrobite.app.data.repository

import com.macrobite.app.data.local.CustomFoodDao
import com.macrobite.app.data.local.entity.CustomFoodEntity
import com.macrobite.app.domain.model.CustomFood
import com.macrobite.app.domain.repository.CustomFoodRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CustomFoodRepositoryImpl @Inject constructor(
    private val customFoodDao: CustomFoodDao
) : CustomFoodRepository {

    override fun getAllCustomFoods(): Flow<List<CustomFood>> {
        return customFoodDao.getAllCustomFoods().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertCustomFood(food: CustomFood): Long {
        return customFoodDao.insertCustomFood(CustomFoodEntity.fromDomain(food))
    }

    override suspend fun deleteCustomFood(foodId: Long) {
        customFoodDao.deleteCustomFoodById(foodId)
    }

    override suspend fun seedDefaultsIfNeeded() {
        if (customFoodDao.getCount() == 0) {
            val defaults = listOf(
                CustomFoodEntity(name = "1 Boiled Egg", portion = "1 whole (50g)", calories = 75, protein = 6f, carbs = 1f, fats = 5f),
                CustomFoodEntity(name = "2 Boiled Eggs", portion = "2 whole (100g)", calories = 150, protein = 12f, carbs = 1f, fats = 10f),
                CustomFoodEntity(name = "1 Scoop Whey", portion = "1 scoop (30g)", calories = 120, protein = 24f, carbs = 3f, fats = 2f),
                CustomFoodEntity(name = "Whole Milk", portion = "1 glass (250ml)", calories = 150, protein = 8f, carbs = 12f, fats = 8f),
                CustomFoodEntity(name = "Banana", portion = "1 medium (118g)", calories = 105, protein = 1f, carbs = 27f, fats = 0f),
                CustomFoodEntity(name = "Peanut Butter", portion = "1 tbsp (16g)", calories = 95, protein = 4f, carbs = 3f, fats = 8f),
                CustomFoodEntity(name = "Plain Roti", portion = "1 roti (40g)", calories = 105, protein = 3f, carbs = 20f, fats = 1f)
            )
            customFoodDao.insertCustomFoods(defaults)
        }
    }
}
