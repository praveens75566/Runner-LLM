package com.example.core.inference

enum class ExecutionBackend(val displayName: String, val chipTarget: String) {
    AUTO("Auto (STI Optimized)", "Selects optimal CPU / GPU configuration via STI metric"),
    CPU_NEON("CPU (ARM NEON/dotprod/i8mm)", "Direct SIMD execution on Cortex-X4 / A720 cores"),
    OPENCL_ADRENO("OpenCL (Adreno GPU Offload)", "Hardware tensor acceleration on Adreno 800-series"),
    VULKAN("Vulkan Compute", "Cross-vendor GPU delegate")
}

enum class KvPrecision(val displayName: String, val bytesPerValue: Double) {
    FP16("FP16 (High Quality)", 2.0),
    Q8_0("Q8_0 (Balanced, -50% Bandwidth)", 1.0),
    Q4_0("Q4_0 (Aggressive, -75% Bandwidth)", 0.5)
}

enum class PerformanceProfile(val displayName: String, val tag: String, val description: String) {
    FLASH_TURBO("⚡ Flash Turbo", "FLASH", "Maximum execution speed, instant sub-5ms prefill, ultra-responsive 100+ tok/s streaming"),
    HIGH_PERFORMANCE("🚀 High Performance", "FAST", "Full CPU/GPU throughput with multi-threaded vector dot-product acceleration"),
    BALANCED("⚖️ Balanced", "BALANCED", "Standard thermal governor with dynamic frequency scaling"),
    BATTERY_SAVER("🔋 Battery Eco", "ECO", "Power-efficient execution on high-efficiency cores")
}

data class InferenceConfig(
    val backend: ExecutionBackend = ExecutionBackend.AUTO,
    val threadCount: Int = 4,
    val kvPrecision: KvPrecision = KvPrecision.Q8_0,
    val performanceProfile: PerformanceProfile = PerformanceProfile.FLASH_TURBO,
    val contextLength: Int = 4096,
    val isMmapEnabled: Boolean = true,
    val isMadvisePrewarmEnabled: Boolean = true,
    val flashAttentionEnabled: Boolean = true,
    val targetTempC: Float = 41.0f
)

data class SamplingParams(
    val minP: Float = 0.05f,
    val temperature: Float = 0.7f,
    val topK: Int = 40,
    val topP: Float = 0.9f,
    val repetitionPenalty: Float = 1.15f,
    val maxTokens: Int = 1024,
    val stopSequences: List<String> = listOf("<|im_end|>", "<|eot_id|>", "</s>", "<end_of_turn>")
)

data class GenerationTokenUpdate(
    val token: String,
    val accumulatedText: String,
    val isFinished: Boolean,
    val isPrefillDone: Boolean,
    val currentTokensPerSec: Double,
    val prefillTokensPerSec: Double,
    val timeToFirstTokenMs: Long,
    val totalTokens: Int,
    val currentThermalState: String,
    val activeThreads: Int,
    val kvCacheTokens: Int,
    val kvCacheMemoryMb: Double,
    val backendUsed: String
)
