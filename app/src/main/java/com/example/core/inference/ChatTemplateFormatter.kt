package com.example.core.inference

import com.example.data.entity.ChatMessageEntity
import com.example.data.entity.ModelEntity

object ChatTemplateFormatter {

    fun formatPrompt(
        model: ModelEntity,
        messages: List<ChatMessageEntity>,
        systemPrompt: String,
        appendGenerationPrompt: Boolean = true
    ): FormattedPrompt {
        val arch = model.architecture.lowercase()
        val template = model.chatTemplate ?: ""

        val promptBuilder = StringBuilder()
        val stopTokens = mutableListOf<String>()

        when {
            template.contains("<|im_start|>") || arch.contains("qwen") -> {
                // ChatML Format
                stopTokens.add("<|im_end|>")
                if (systemPrompt.isNotBlank()) {
                    promptBuilder.append("<|im_start|>system\n$systemPrompt<|im_end|>\n")
                }
                for (msg in messages) {
                    val role = msg.role.lowercase()
                    promptBuilder.append("<|im_start|>$role\n${msg.content}<|im_end|>\n")
                }
                if (appendGenerationPrompt) {
                    promptBuilder.append("<|im_start|>assistant\n")
                }
            }

            template.contains("<|start_header_id|>") || arch.contains("llama") -> {
                // Llama 3 format
                stopTokens.add("<|eot_id|>")
                stopTokens.add("<|end_of_text|>")
                if (systemPrompt.isNotBlank()) {
                    promptBuilder.append("<|start_header_id|>system<|end_header_id|>\n\n$systemPrompt<|eot_id|>")
                }
                for (msg in messages) {
                    val role = msg.role.lowercase()
                    promptBuilder.append("<|start_header_id|>$role<|end_header_id|>\n\n${msg.content}<|eot_id|>")
                }
                if (appendGenerationPrompt) {
                    promptBuilder.append("<|start_header_id|>assistant<|end_header_id|>\n\n")
                }
            }

            arch.contains("gemma") -> {
                // Gemma format
                stopTokens.add("<end_of_turn>")
                for (msg in messages) {
                    val role = if (msg.role.equals("USER", true)) "user" else "model"
                    promptBuilder.append("<start_of_turn>$role\n${msg.content}<end_of_turn>\n")
                }
                if (appendGenerationPrompt) {
                    promptBuilder.append("<start_of_turn>model\n")
                }
            }

            arch.contains("mistral") -> {
                // Mistral format
                stopTokens.add("</s>")
                for (msg in messages) {
                    if (msg.role.equals("USER", true)) {
                        promptBuilder.append("[INST] ${msg.content} [/INST]")
                    } else {
                        promptBuilder.append(" ${msg.content}</s>")
                    }
                }
            }

            else -> {
                // Universal default
                stopTokens.add("</s>")
                stopTokens.add("<|im_end|>")
                if (systemPrompt.isNotBlank()) {
                    promptBuilder.append("System: $systemPrompt\n\n")
                }
                for (msg in messages) {
                    val role = if (msg.role.equals("USER", true)) "User" else "Assistant"
                    promptBuilder.append("$role: ${msg.content}\n\n")
                }
                if (appendGenerationPrompt) {
                    promptBuilder.append("Assistant: ")
                }
            }
        }

        return FormattedPrompt(
            fullPromptText = promptBuilder.toString(),
            stopTokens = stopTokens
        )
    }
}

data class FormattedPrompt(
    val fullPromptText: String,
    val stopTokens: List<String>
)
