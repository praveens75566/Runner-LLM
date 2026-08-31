package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.entity.BenchmarkResultEntity
import com.example.data.entity.ChatMessageEntity
import com.example.data.entity.ConversationEntity
import com.example.data.entity.ModelEntity
import com.example.data.entity.ThermalLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModelDao {
    @Query("SELECT * FROM models ORDER BY isFavorite DESC, dateImported DESC")
    fun getAllModels(): Flow<List<ModelEntity>>

    @Query("SELECT * FROM models WHERE id = :id LIMIT 1")
    suspend fun getModelById(id: Long): ModelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertModel(model: ModelEntity): Long

    @Update
    suspend fun updateModel(model: ModelEntity)

    @Query("DELETE FROM models WHERE id = :id")
    suspend fun deleteModelById(id: Long)

    @Query("SELECT COUNT(*) FROM models")
    suspend fun getModelCount(): Int
}

@Dao
interface ConversationDao {
    @Query("SELECT * FROM conversations ORDER BY updatedAt DESC")
    fun getAllConversations(): Flow<List<ConversationEntity>>

    @Query("SELECT * FROM conversations WHERE id = :id LIMIT 1")
    suspend fun getConversationById(id: Long): ConversationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversation(conversation: ConversationEntity): Long

    @Update
    suspend fun updateConversation(conversation: ConversationEntity)

    @Query("DELETE FROM conversations WHERE id = :id")
    suspend fun deleteConversationById(id: Long)

    @Query("UPDATE conversations SET title = :title, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateConversationTitle(id: Long, title: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM conversations")
    suspend fun clearAllConversations()
}

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE conversationId = :conversationId ORDER BY timestamp ASC")
    fun getMessagesForConversation(conversationId: Long): Flow<List<ChatMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessageEntity): Long

    @Query("DELETE FROM chat_messages WHERE conversationId = :conversationId")
    suspend fun deleteMessagesForConversation(conversationId: Long)

    @Query("DELETE FROM chat_messages WHERE id = :id")
    suspend fun deleteMessageById(id: Long)
}

@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM benchmark_results ORDER BY timestamp DESC")
    fun getAllBenchmarks(): Flow<List<BenchmarkResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmark(result: BenchmarkResultEntity): Long

    @Query("DELETE FROM benchmark_results WHERE id = :id")
    suspend fun deleteBenchmarkById(id: Long)

    @Query("DELETE FROM benchmark_results")
    suspend fun clearAllBenchmarks()
}

@Dao
interface ThermalLogDao {
    @Query("SELECT * FROM thermal_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentLogs(): Flow<List<ThermalLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ThermalLogEntity): Long

    @Query("DELETE FROM thermal_logs")
    suspend fun clearLogs()
}
