package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.benchmark.RooflineEstimator
import com.example.core.benchmark.StiCalculator
import com.example.core.benchmark.StiConfigCandidate
import com.example.core.hardware.DeviceHardwareManager
import com.example.core.inference.ExecutionBackend
import com.example.core.inference.KvPrecision
import com.example.core.thermal.ThermalGovernor
import com.example.data.db.AppDatabase
import com.example.data.entity.ChatMessageEntity
import com.example.data.entity.ConversationEntity
import com.example.data.entity.ModelEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  private lateinit var context: Context
  private lateinit var database: AppDatabase

  @Before
  fun setup() {
    context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
        .allowMainThreadQueries()
        .build()
  }

  @After
  fun teardown() {
    database.close()
  }

  @Test
  fun `read string from context matches app name`() {
    val appName = context.getString(R.string.app_name)
    assertEquals("LLM Runner", appName)
  }

  @Test
  fun `roofline estimator calculates valid decode bandwidth estimate`() {
    val model = ModelEntity(
        id = 1L,
        name = "Qwen 3.5 4B",
        filename = "qwen.gguf",
        filePath = "local://qwen.gguf",
        architecture = "qwen2",
        paramCount = "3.8B",
        paramCountBillion = 3.8,
        quantType = "Q4_K_M",
        contextLength = 4096,
        embeddingDim = 2560,
        layerCount = 32,
        headCount = 20,
        headCountKv = 4,
        chatTemplate = "<|im_start|>",
        fileSize = 2_400_000_000L
    )

    val estimate = RooflineEstimator.estimateDecodeSpeed(model, effectiveBandwidthGbps = 52.0)
    assertTrue("Estimate should be positive", estimate.estimatedTokensPerSec > 0)
    assertTrue("Estimate should be in realistic bounds", estimate.estimatedTokensPerSec in 5.0..70.0)
  }

  @Test
  fun `sustainable throughput index correctly penalizes high thermal rise`() {
    val coolConfig = StiConfigCandidate(
        threadCount = 4,
        backend = ExecutionBackend.AUTO,
        kvPrecision = KvPrecision.Q8_0,
        measuredTokenGenSpeed = 25.0,
        thermalRiseSlopePerMin = 0.8,
        ambientTempC = 30.0,
        throttleTempC = 43.0
    )

    val hotConfig = StiConfigCandidate(
        threadCount = 8,
        backend = ExecutionBackend.AUTO,
        kvPrecision = KvPrecision.Q8_0,
        measuredTokenGenSpeed = 27.0,
        thermalRiseSlopePerMin = 3.2,
        ambientTempC = 30.0,
        throttleTempC = 43.0
    )

    val coolSti = StiCalculator.calculateSti(coolConfig)
    val hotSti = StiCalculator.calculateSti(hotConfig)

    assertTrue("Cool STI ($coolSti) should exceed Hot STI ($hotSti)", coolSti > hotSti)
  }

  @Test
  fun `database model and message insertion works properly`() = runBlocking {
    val model = ModelEntity(
        name = "DeepSeek R1 Distill Qwen 1.5B",
        filename = "deepseek_r1_1.5b.gguf",
        filePath = "local://deepseek_r1_1.5b.gguf",
        architecture = "qwen2",
        paramCount = "1.5B",
        paramCountBillion = 1.5,
        quantType = "Q4_K_M",
        contextLength = 4096,
        embeddingDim = 1536,
        layerCount = 28,
        headCount = 12,
        headCountKv = 2,
        chatTemplate = "<|im_start|>",
        fileSize = 1_100_000_000L
    )

    val modelId = database.modelDao().insertModel(model)
    assertTrue("Model ID should be positive", modelId > 0)

    val models = database.modelDao().getAllModels().first()
    assertEquals(1, models.size)
    assertEquals("DeepSeek R1 Distill Qwen 1.5B", models[0].name)

    val conv = ConversationEntity(
        title = "Test Session",
        modelId = modelId,
        systemPrompt = "You are an on-device AI."
    )
    val convId = database.conversationDao().insertConversation(conv)

    val message = ChatMessageEntity(
        conversationId = convId,
        role = "USER",
        content = "What is KV cache quantization?"
    )
    database.chatMessageDao().insertMessage(message)

    val messages = database.chatMessageDao().getMessagesForConversation(convId).first()
    assertEquals(1, messages.size)
    assertEquals("What is KV cache quantization?", messages[0].content)

    // Test renaming conversation
    database.conversationDao().updateConversationTitle(convId, "Renamed Session")
    val updatedConv = database.conversationDao().getConversationById(convId)
    assertEquals("Renamed Session", updatedConv?.title)

    // Test clear all conversations
    database.conversationDao().clearAllConversations()
    val convsAfterClear = database.conversationDao().getAllConversations().first()
    assertEquals(0, convsAfterClear.size)
  }

  @Test
  fun `thermal governor updates state and clamps thread effort properly`() {
    val hardwareManager = DeviceHardwareManager(context)
    val governor = ThermalGovernor(context, hardwareManager)

    governor.startMonitoring()
    val state = governor.evaluateThermalStep(baseThreads = 4, targetTempC = 40.0f)
    assertNotNull(state)
    assertTrue("Active threads should be between 1 and 8", state.activeThreads in 1..8)
    assertTrue("Effort budget should be in valid fraction", state.effortBudget in 0.2f..1.0f)
    governor.stopMonitoring()
  }
}
