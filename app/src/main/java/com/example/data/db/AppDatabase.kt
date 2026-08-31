package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ModelEntity::class,
        ConversationEntity::class,
        ChatMessageEntity::class,
        BenchmarkResultEntity::class,
        ThermalLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun modelDao(): ModelDao
    abstract fun conversationDao(): ConversationDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun benchmarkDao(): BenchmarkDao
    abstract fun thermalLogDao(): ThermalLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "llm_runner.db"
                )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Pre-populate sample benchmark-calibrated models
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val database = getInstance(context)
                                    prepopulateModels(database.modelDao(), database.conversationDao())
                                } catch (e: Exception) {
                                    android.util.Log.e("AppDatabase", "Error prepopulating models", e)
                                }
                            }
                        }
                    }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun prepopulateModels(modelDao: ModelDao, conversationDao: ConversationDao) {
            val qwenModel = ModelEntity(
                id = 1L,
                name = "Qwen 3.5 4B (Instruct)",
                filename = "qwen3.5-4b-instruct-q4_k_m.gguf",
                filePath = "local://models/qwen3.5-4b-instruct-q4_k_m.gguf",
                architecture = "qwen2",
                paramCount = "3.8B",
                paramCountBillion = 3.82,
                quantType = "Q4_K_M",
                contextLength = 4096,
                embeddingDim = 2560,
                layerCount = 32,
                headCount = 20,
                headCountKv = 4,
                chatTemplate = "{% for message in messages %}{{'<|im_start|>' + message['role'] + '\n' + message['content'] + '<|im_end|>' + '\n'}}{% endfor %}{% if add_generation_prompt %}{{'<|im_start|>assistant\n'}}{% endif %}",
                fileSize = 2_470_000_000L, // 2.47 GB
                isBundled = true,
                isFavorite = true
            )

            val llamaModel = ModelEntity(
                id = 2L,
                name = "Llama 3.2 3B (Instruct)",
                filename = "llama-3.2-3b-instruct-q4_k_m.gguf",
                filePath = "local://models/llama-3.2-3b-instruct-q4_k_m.gguf",
                architecture = "llama",
                paramCount = "3.2B",
                paramCountBillion = 3.21,
                quantType = "Q4_K_M",
                contextLength = 4096,
                embeddingDim = 3072,
                layerCount = 28,
                headCount = 24,
                headCountKv = 8,
                chatTemplate = "<|start_header_id|>system<|end_header_id|>\n\n{{system_prompt}}<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n{{prompt}}<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n",
                fileSize = 1_980_000_000L, // 1.98 GB
                isBundled = true,
                isFavorite = false
            )

            val deepseekModel = ModelEntity(
                id = 3L,
                name = "DeepSeek R1 Distill 1.5B",
                filename = "deepseek-r1-distill-qwen-1.5b-q8_0.gguf",
                filePath = "local://models/deepseek-r1-distill-qwen-1.5b-q8_0.gguf",
                architecture = "qwen2",
                paramCount = "1.7B",
                paramCountBillion = 1.77,
                quantType = "Q8_0",
                contextLength = 4096,
                embeddingDim = 1536,
                layerCount = 28,
                headCount = 12,
                headCountKv = 2,
                chatTemplate = "{% for message in messages %}{{'<|im_start|>' + message['role'] + '\n' + message['content'] + '<|im_end|>' + '\n'}}{% endfor %}{% if add_generation_prompt %}{{'<|im_start|>assistant\n'}}{% endif %}",
                fileSize = 1_890_000_000L, // 1.89 GB
                isBundled = true,
                isFavorite = false
            )

            modelDao.insertModel(qwenModel)
            modelDao.insertModel(llamaModel)
            modelDao.insertModel(deepseekModel)

            // Prepopulate initial conversation
            val convId = conversationDao.insertConversation(
                ConversationEntity(
                    id = 1L,
                    title = "Getting Started with On-Device LLMs",
                    modelId = 1L,
                    systemPrompt = "You are a specialized on-device AI running locally on Snapdragon hardware with zero cloud dependency."
                )
            )
        }
    }
}
