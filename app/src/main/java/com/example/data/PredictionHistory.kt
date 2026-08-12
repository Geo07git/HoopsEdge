package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prediction_history")
data class PredictionHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateMs: Long = System.currentTimeMillis(),
    val competition: String,
    val homeTeam: String,
    val awayTeam: String,
    val season: String,
    val homePred: Int,
    val awayPred: Int,
    val marketTotal: Double,
    val edge: Double,
    val homeStatsJson: String = "",
    val awayStatsJson: String = ""
)
