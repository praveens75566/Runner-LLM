package com.example.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "models")
data class ModelEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val filename: String,
    val filePath: String,
    val architecture: String,
    val paramCount: String,
    val paramCountBillion: Double,
    val quantType: String,
    val contextLength: Int,
    val embeddingDim: Int,
    val layerCount: Int,
    val headCount: Int,
    val headCountKv: Int,
    val chatTemplate: String?,
    val fileSize: Long,
    val isBundled: Boolean = false,
    val dateImported: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)

@Entity(
    tableName = "conversations",
    indices = [Index("modelId")]
)
data class ConversationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val title: String,
    val modelId: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val systemPrompt: String = "You are a helpful, concise AI assistant running fully on-device."
)

@Entity(
    tableName = "chat_messages",
    foreignKeys = [
        ForeignKey(
            entity = ConversationEntity::class,
            parentColumns = ["id"],
            childColumns = ["conversationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("conversationId")]
)
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val conversationId: Long,
    val role: String, // USER, ASSISTANT, SYSTEM
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val tokenCount: Int = 0,
    val prefillTokPerSec: Double = 0.0,
    val decodeTokPerSec: Double = 0.0,
    val timeToFirstTokenMs: Long = 0L,
    val totalGenTimeMs: Long = 0L,
    val thermalStatus: String = "NORMAL",
    val activeThreads: Int = 4,
    val backendUsed: String = "CPU (NEON)",
    val kvCacheTokens: Int = 0
)

@Entity(tableName = "benchmark_results")
data class BenchmarkResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val modelName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val testType: String, // FULL_SWEEP, PREFILL, DECODE_SWEEP, THERMAL_STRESS, MEM_BANDWIDTH
    val threadCount: Int,
    val backendUsed: String,
    val kvPrecision: String,
    val prefillTokPerSec: Double,
    val decodeTokPerSec: Double,
    val peakTokPerSec: Double,
    val sustainedTokPerSec: Double,
    val thermalRisePerMin: Double,
    val throttleFloorTokPerSec: Double,
    val stiScore: Double, // Sustainable Throughput Index (§16.1)
    val memoryBandwidthGbps: Double,
    val deviceSoc: String,
    val batteryLevel: Int,
    val isCharging: Boolean
)

@Entity(tableName = "thermal_logs")
data class ThermalLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val timestamp: Long = System.currentTimeMillis(),
    val thermalStatus: Int,
    val thermalName: String,
    val batteryTempC: Float,
    val activeThreads: Int,
    val decodeTokPerSec: Double,
    val pidEffortBudget: Float
)
