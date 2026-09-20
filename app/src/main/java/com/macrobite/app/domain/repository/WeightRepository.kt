package com.macrobite.app.domain.repository

import com.macrobite.app.domain.model.WeightLog
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    fun getAllWeights(): Flow<List<WeightLog>>
    suspend fun insertWeight(weightLog: WeightLog): Long
    suspend fun deleteWeight(id: Long)
}
