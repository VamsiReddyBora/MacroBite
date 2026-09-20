package com.macrobite.app.data.repository

import com.macrobite.app.data.local.MealDao
import com.macrobite.app.data.local.entity.MealEntity
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.repository.MealRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MealRepositoryImpl @Inject constructor(
    private val mealDao: MealDao
) : MealRepository {

    override fun getMealsForDate(date: String): Flow<List<MealEntry>> {
        return mealDao.getMealsForDate(date).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getMealsBetweenDates(startDate: String, endDate: String): Flow<List<MealEntry>> {
        return mealDao.getMealsBetweenDates(startDate, endDate).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllMeals(): Flow<List<MealEntry>> {
        return mealDao.getAllMeals().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertMeal(meal: MealEntry): Long {
        return mealDao.insertMeal(MealEntity.fromDomain(meal))
    }

    override suspend fun insertMeals(meals: List<MealEntry>): List<Long> {
        return mealDao.insertMeals(meals.map { MealEntity.fromDomain(it) })
    }

    override suspend fun updateMeal(meal: MealEntry) {
        mealDao.updateMeal(MealEntity.fromDomain(meal))
    }

    override suspend fun deleteMeal(mealId: Long) {
        mealDao.deleteMealById(mealId)
    }

    override suspend fun getMealById(mealId: Long): MealEntry? {
        return mealDao.getMealById(mealId)?.toDomain()
    }
}
