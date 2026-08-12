package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PredictionHistoryDao {
    @Query("SELECT * FROM prediction_history ORDER BY dateMs DESC")
    fun getAllHistory(): Flow<List<PredictionHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: PredictionHistory)
    
    @Query("DELETE FROM prediction_history")
    suspend fun clearHistory()
    
    @Query("DELETE FROM prediction_history WHERE id = :id")
    suspend fun deleteItem(id: Int)
}
