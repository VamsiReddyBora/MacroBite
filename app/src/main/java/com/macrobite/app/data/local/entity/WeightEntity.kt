package com.macrobite.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.macrobite.app.domain.model.WeightLog

@Entity(tableName = "weight_logs")
data class WeightEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val weightKg: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toDomain(): WeightLog {
        return WeightLog(
            id = id,
            date = date,
            weightKg = weightKg,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(log: WeightLog): WeightEntity {
            return WeightEntity(
                id = log.id,
                date = log.date,
                weightKg = log.weightKg,
                timestamp = log.timestamp
            )
        }
    }
}
