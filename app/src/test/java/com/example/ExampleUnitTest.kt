package com.example

import com.example.core.inference.ChatTemplateFormatter
import com.example.data.entity.ChatMessageEntity
import com.example.data.entity.ModelEntity
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testChatMLFormatting() {
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

    val messages = listOf(
        ChatMessageEntity(id = 1L, conversationId = 1L, role = "USER", content = "How does quantization work?", timestamp = 1000L)
    )
    val formatted = ChatTemplateFormatter.formatPrompt(
        model = model,
        messages = messages,
        systemPrompt = "You are an on-device AI."
    )
    assertTrue("Formatted prompt should include system prompt", formatted.fullPromptText.contains("You are an on-device AI."))
    assertTrue("Formatted prompt should include ChatML markers", formatted.fullPromptText.contains("<|im_start|>user"))
    assertTrue("Formatted prompt should include assistant generation marker", formatted.fullPromptText.contains("<|im_start|>assistant"))
    assertTrue("Stop tokens should include <|im_end|>", formatted.stopTokens.contains("<|im_end|>"))
  }
}
