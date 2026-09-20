package com.macrobite.app.data.repository

import com.macrobite.app.data.local.WeightDao
import com.macrobite.app.data.local.entity.WeightEntity
import com.macrobite.app.domain.model.WeightLog
import com.macrobite.app.domain.repository.WeightRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeightRepositoryImpl @Inject constructor(
    private val weightDao: WeightDao
) : WeightRepository {

    override fun getAllWeights(): Flow<List<WeightLog>> {
        return weightDao.getAllWeights().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun insertWeight(weightLog: WeightLog): Long {
        return weightDao.insertWeight(WeightEntity.fromDomain(weightLog))
    }

    override suspend fun deleteWeight(id: Long) {
        weightDao.deleteWeightById(id)
    }
}
