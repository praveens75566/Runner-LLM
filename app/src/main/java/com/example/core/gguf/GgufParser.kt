package com.example.core.gguf

import android.util.Log
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.StandardCharsets

object GgufParser {
    private const val TAG = "GgufParser"
    private const val GGUF_MAGIC = "GGUF"

    /**
     * Parses the header and metadata key-value table of a GGUF file from an InputStream.
     * Safely reads without loading the full model weight tensors into memory.
     */
    fun parseHeader(inputStream: InputStream, totalFileSize: Long = 0L): Result<GgufMetadata> {
        return runCatching {
            val headerBytes = ByteArray(4)
            readFully(inputStream, headerBytes)
            val magic = String(headerBytes, StandardCharsets.US_ASCII)

            if (magic != GGUF_MAGIC) {
                throw IllegalArgumentException("Invalid GGUF Magic header: $magic. Expected 'GGUF'")
            }

            val version = readInt32LE(inputStream)
            val tensorCount = readInt64LE(inputStream)
            val metadataKvCount = readInt64LE(inputStream)

            Log.d(TAG, "GGUF Magic: $magic, Version: $version, Tensors: $tensorCount, KV entries: $metadataKvCount")

            val rawMetadata = mutableMapOf<String, Any>()

            // Safely iterate through metadata key-value pairs (cap at reasonable count to avoid infinite loops on corrupted files)
            val countToRead = metadataKvCount.coerceAtMost(2000L)
            for (i in 0 until countToRead) {
                val key = readString(inputStream)
                if (key.isEmpty()) break
                val valueType = readInt32LE(inputStream)
                val value = readValue(inputStream, GgufValueType.fromId(valueType))
                rawMetadata[key] = value
            }

            // Extract core architecture information
            val arch = (rawMetadata["general.architecture"] as? String)?.lowercase() ?: "llama"
            val modelName = (rawMetadata["general.name"] as? String)
                ?: (rawMetadata["$arch.name"] as? String)
                ?: "Custom $arch Model"
            val author = (rawMetadata["general.author"] as? String) ?: "Community"
            val description = (rawMetadata["general.description"] as? String) ?: ""
            val license = (rawMetadata["general.license"] as? String) ?: "Unknown"

            val contextLength = (rawMetadata["$arch.context_length"] as? Number)?.toInt()
                ?: (rawMetadata["general.context_length"] as? Number)?.toInt()
                ?: 4096

            val embeddingLength = (rawMetadata["$arch.embedding_length"] as? Number)?.toInt() ?: 2048
            val blockCount = (rawMetadata["$arch.block_count"] as? Number)?.toInt() ?: 24
            val headCount = (rawMetadata["$arch.attention.head_count"] as? Number)?.toInt() ?: 16
            val headCountKv = (rawMetadata["$arch.attention.head_count_kv"] as? Number)?.toInt() ?: headCount
            val feedForwardLength = (rawMetadata["$arch.feed_forward_length"] as? Number)?.toInt() ?: (embeddingLength * 3.5).toInt()
            val vocabSize = (rawMetadata["tokenizer.ggml.tokens"] as? List<*>)?.size
                ?: (rawMetadata["tokenizer.ggml.vocab_size"] as? Number)?.toInt()
                ?: 32000

            val chatTemplate = (rawMetadata["tokenizer.chat_template"] as? String)

            // Deduce quantization format
            val fileTypeNum = (rawMetadata["general.file_type"] as? Number)?.toInt() ?: 0
            val quantType = parseQuantizationType(fileTypeNum, rawMetadata)

            // Estimate parameter count from architecture and layer shapes
            val estimatedParamsNum = calculateEstimatedParams(
                embeddingLength = embeddingLength,
                blockCount = blockCount,
                feedForwardLength = feedForwardLength,
                vocabSize = vocabSize,
                tensorCount = tensorCount,
                fileSize = totalFileSize
            )

            val estimatedParams = formatParams(estimatedParamsNum)

            GgufMetadata(
                magic = magic,
                version = version,
                tensorCount = tensorCount,
                metadataKvCount = metadataKvCount,
                architecture = arch,
                modelName = modelName,
                author = author,
                description = description,
                license = license,
                quantizationType = quantType,
                contextLength = contextLength,
                embeddingLength = embeddingLength,
                blockCount = blockCount,
                headCount = headCount,
                headCountKv = headCountKv,
                feedForwardLength = feedForwardLength,
                vocabSize = vocabSize,
                chatTemplate = chatTemplate,
                fileSize = totalFileSize,
                estimatedParams = estimatedParams,
                estimatedParamsNum = estimatedParamsNum,
                rawMetadata = rawMetadata
            )
        }
    }

    private fun readValue(inputStream: InputStream, type: GgufValueType): Any {
        return when (type) {
            GgufValueType.UINT8, GgufValueType.INT8 -> inputStream.read()
            GgufValueType.UINT16, GgufValueType.INT16 -> readInt16LE(inputStream)
            GgufValueType.UINT32, GgufValueType.INT32 -> readInt32LE(inputStream)
            GgufValueType.FLOAT32 -> java.lang.Float.intBitsToFloat(readInt32LE(inputStream))
            GgufValueType.BOOL -> (inputStream.read() != 0)
            GgufValueType.STRING -> readString(inputStream)
            GgufValueType.UINT64, GgufValueType.INT64 -> readInt64LE(inputStream)
            GgufValueType.FLOAT64 -> java.lang.Double.longBitsToDouble(readInt64LE(inputStream))
            GgufValueType.ARRAY -> {
                val itemType = GgufValueType.fromId(readInt32LE(inputStream))
                val arrayLen = readInt64LE(inputStream).coerceAtMost(5000L).toInt()
                val list = mutableListOf<Any>()
                for (j in 0 until arrayLen) {
                    list.add(readValue(inputStream, itemType))
                }
                list
            }
        }
    }

    private fun readString(inputStream: InputStream): String {
        val len = readInt64LE(inputStream)
        if (len <= 0 || len > 2_000_000) return ""
        val bytes = ByteArray(len.toInt())
        readFully(inputStream, bytes)
        return String(bytes, StandardCharsets.UTF_8)
    }

    private fun readInt16LE(inputStream: InputStream): Int {
        val b = ByteArray(2)
        readFully(inputStream, b)
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
    }

    private fun readInt32LE(inputStream: InputStream): Int {
        val b = ByteArray(4)
        readFully(inputStream, b)
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).int
    }

    private fun readInt64LE(inputStream: InputStream): Long {
        val b = ByteArray(8)
        readFully(inputStream, b)
        return ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).long
    }

    private fun readFully(inputStream: InputStream, buffer: ByteArray) {
        var bytesRead = 0
        while (bytesRead < buffer.size) {
            val count = inputStream.read(buffer, bytesRead, buffer.size - bytesRead)
            if (count < 0) throw java.io.EOFException("Unexpected end of GGUF stream")
            bytesRead += count
        }
    }

    private fun parseQuantizationType(fileType: Int, metadata: Map<String, Any>): String {
        val quantName = (metadata["general.quantization_version"] as? String)
            ?: (metadata["general.file_type_name"] as? String)
        if (quantName != null) return quantName

        return when (fileType) {
            0 -> "ALL_F32"
            1 -> "MOSTLY_F16"
            2 -> "MOSTLY_Q4_0"
            3 -> "MOSTLY_Q4_1"
            7 -> "MOSTLY_Q8_0"
            8 -> "MOSTLY_Q5_0"
            9 -> "MOSTLY_Q5_1"
            12 -> "MOSTLY_Q4_K_S"
            13 -> "MOSTLY_Q4_K_M"
            14 -> "MOSTLY_Q5_K_S"
            15 -> "MOSTLY_Q5_K_M"
            16 -> "MOSTLY_Q6_K"
            17 -> "MOSTLY_Q8_K"
            else -> "Q4_K_M" // Default standard for mobile LLMs
        }
    }

    private fun calculateEstimatedParams(
        embeddingLength: Int,
        blockCount: Int,
        feedForwardLength: Int,
        vocabSize: Int,
        tensorCount: Long,
        fileSize: Long
    ): Double {
        // Analytical formula for transformer weights count
        val attnWeightsPerLayer = 4L * embeddingLength * embeddingLength
        val ffnWeightsPerLayer = 3L * embeddingLength * feedForwardLength
        val layerWeights = (attnWeightsPerLayer + ffnWeightsPerLayer) * blockCount
        val embeddingWeights = 1L * vocabSize * embeddingLength
        val totalAnalyticalWeights = (layerWeights + embeddingWeights).toDouble()

        if (totalAnalyticalWeights > 100_000_000) {
            return totalAnalyticalWeights / 1_000_000_000.0
        }

        // Fallback based on file size and ~4.5 bits/param for Q4
        if (fileSize > 0) {
            val estimatedTotalBytes = fileSize.toDouble()
            val paramsFromSize = (estimatedTotalBytes * 8.0) / 4.5
            return paramsFromSize / 1_000_000_000.0
        }

        return 3.82 // Default Qwen3.5 4B reference
    }

    private fun formatParams(paramsInBillion: Double): String {
        return if (paramsInBillion < 1.0) {
            val millions = (paramsInBillion * 1000).toInt()
            "${millions}M"
        } else {
            String.format(java.util.Locale.US, "%.1fB", paramsInBillion)
        }
    }
}
