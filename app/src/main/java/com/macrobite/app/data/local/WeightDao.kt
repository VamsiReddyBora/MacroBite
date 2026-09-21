package com.macrobite.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.macrobite.app.data.local.entity.WeightEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_logs ORDER BY date ASC, timestamp ASC")
    fun getAllWeights(): Flow<List<WeightEntity>>

    @Query("SELECT * FROM weight_logs ORDER BY date ASC, timestamp ASC")
    suspend fun getAllWeightsDirect(): List<WeightEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeight(weight: WeightEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeights(weights: List<WeightEntity>): List<Long>

    @Query("DELETE FROM weight_logs WHERE id = :id")
    suspend fun deleteWeightById(id: Long)
}
