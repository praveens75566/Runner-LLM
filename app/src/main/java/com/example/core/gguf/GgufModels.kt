package com.example.core.gguf

data class GgufMetadata(
    val magic: String,
    val version: Int,
    val tensorCount: Long,
    val metadataKvCount: Long,
    val architecture: String,
    val modelName: String,
    val author: String,
    val description: String,
    val license: String,
    val quantizationType: String,
    val contextLength: Int,
    val embeddingLength: Int, // Hidden dimension
    val blockCount: Int,      // Layer count
    val headCount: Int,
    val headCountKv: Int,
    val feedForwardLength: Int,
    val vocabSize: Int,
    val chatTemplate: String?,
    val fileSize: Long,
    val estimatedParams: String,
    val estimatedParamsNum: Double, // in Billions (e.g. 3.82)
    val rawMetadata: Map<String, Any>
)

enum class GgufValueType(val id: Int) {
    UINT8(0),
    INT8(1),
    UINT16(2),
    INT16(3),
    UINT32(4),
    INT32(5),
    FLOAT32(6),
    BOOL(7),
    STRING(8),
    ARRAY(9),
    UINT64(10),
    INT64(11),
    FLOAT64(12);

    companion object {
        fun fromId(id: Int): GgufValueType = entries.find { it.id == id } ?: STRING
    }
}
