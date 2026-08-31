package com.example.data.repository

import com.example.data.dao.BenchmarkDao
import com.example.data.dao.ChatMessageDao
import com.example.data.dao.ConversationDao
import com.example.data.dao.ModelDao
import com.example.data.dao.ThermalLogDao
import com.example.data.entity.BenchmarkResultEntity
import com.example.data.entity.ChatMessageEntity
import com.example.data.entity.ConversationEntity
import com.example.data.entity.ModelEntity
import com.example.data.entity.ThermalLogEntity
import kotlinx.coroutines.flow.Flow

class ModelRepository(private val modelDao: ModelDao) {
    val allModels: Flow<List<ModelEntity>> = modelDao.getAllModels()

    suspend fun getModelById(id: Long): ModelEntity? = modelDao.getModelById(id)
    suspend fun insertModel(model: ModelEntity): Long = modelDao.insertModel(model)
    suspend fun updateModel(model: ModelEntity) = modelDao.updateModel(model)
    suspend fun deleteModel(id: Long) = modelDao.deleteModelById(id)
    suspend fun getModelCount(): Int = modelDao.getModelCount()
}

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: ChatMessageDao
) {
    val allConversations: Flow<List<ConversationEntity>> = conversationDao.getAllConversations()

    fun getMessagesForConversation(convId: Long): Flow<List<ChatMessageEntity>> =
        messageDao.getMessagesForConversation(convId)

    suspend fun getConversationById(id: Long): ConversationEntity? =
        conversationDao.getConversationById(id)

    suspend fun createConversation(title: String, modelId: Long?, systemPrompt: String): Long {
        val conv = ConversationEntity(
            title = title,
            modelId = modelId,
            systemPrompt = systemPrompt
        )
        return conversationDao.insertConversation(conv)
    }

    suspend fun updateConversation(conversation: ConversationEntity) =
        conversationDao.updateConversation(conversation)

    suspend fun updateConversationTitle(id: Long, title: String) =
        conversationDao.updateConversationTitle(id, title)

    suspend fun deleteConversation(id: Long) =
        conversationDao.deleteConversationById(id)

    suspend fun clearAllConversations() =
        conversationDao.clearAllConversations()

    suspend fun insertMessage(message: ChatMessageEntity): Long =
        messageDao.insertMessage(message)

    suspend fun deleteMessagesForConversation(convId: Long) =
        messageDao.deleteMessagesForConversation(convId)
}

class BenchmarkRepository(
    private val benchmarkDao: BenchmarkDao,
    private val thermalLogDao: ThermalLogDao
) {
    val allBenchmarks: Flow<List<BenchmarkResultEntity>> = benchmarkDao.getAllBenchmarks()
    val recentThermalLogs: Flow<List<ThermalLogEntity>> = thermalLogDao.getRecentLogs()

    suspend fun insertBenchmark(result: BenchmarkResultEntity): Long =
        benchmarkDao.insertBenchmark(result)

    suspend fun deleteBenchmark(id: Long) =
        benchmarkDao.deleteBenchmarkById(id)

    suspend fun clearAllBenchmarks() =
        benchmarkDao.clearAllBenchmarks()

    suspend fun logThermal(log: ThermalLogEntity) =
        thermalLogDao.insertLog(log)

    suspend fun clearThermalLogs() =
        thermalLogDao.clearLogs()
}
