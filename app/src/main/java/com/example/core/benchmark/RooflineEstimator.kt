package com.example.core.benchmark

import com.example.core.inference.KvPrecision
import com.example.data.entity.ModelEntity

data class RooflineEstimate(
    val estimatedTokensPerSec: Double,
    val modelSizeBytes: Long,
    val kvBytesPerToken: Double,
    val effectiveBandwidthGbps: Double,
    val contextLength: Int,
    val formulaExplanation: String
)

object RooflineEstimator {

    /**
     * §16.3 Bandwidth Roofline Estimator
     * Instant, honest expectation-setting at import time.
     * Formula: TG ≈ B_effective / (S_model + S_kv_per_token * N_context)
     */
    fun estimateDecodeSpeed(
        model: ModelEntity,
        effectiveBandwidthGbps: Double,
        contextLength: Int = 2048,
        kvPrecision: KvPrecision = KvPrecision.Q8_0
    ): RooflineEstimate {
        val modelBytes = model.fileSize.toDouble()
        val kvBytesPerToken = (model.layerCount * model.embeddingDim * kvPrecision.bytesPerValue * 2)
        val totalBytesPerToken = modelBytes + (kvBytesPerToken * contextLength)
        val totalGigabytesPerToken = totalBytesPerToken / (1024.0 * 1024.0 * 1024.0)

        val estimatedSpeed = if (totalGigabytesPerToken > 0) {
            (effectiveBandwidthGbps / totalGigabytesPerToken).coerceIn(3.0, 95.0)
        } else {
            15.0
        }

        val explanation = "TG ≈ ${String.format(java.util.Locale.US, "%.1f", effectiveBandwidthGbps)} GB/s / " +
                "(${String.format(java.util.Locale.US, "%.2f", modelBytes / (1024 * 1024 * 1024.0))} GB model + KV Cache)"

        return RooflineEstimate(
            estimatedTokensPerSec = estimatedSpeed,
            modelSizeBytes = model.fileSize,
            kvBytesPerToken = kvBytesPerToken,
            effectiveBandwidthGbps = effectiveBandwidthGbps,
            contextLength = contextLength,
            formulaExplanation = explanation
        )
    }
}
