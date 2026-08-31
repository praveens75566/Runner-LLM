package com.example.core.benchmark

import com.example.core.inference.ExecutionBackend
import com.example.core.inference.KvPrecision
import kotlin.math.max

data class StiConfigCandidate(
    val threadCount: Int,
    val backend: ExecutionBackend,
    val kvPrecision: KvPrecision,
    val measuredTokenGenSpeed: Double, // TG(c)
    val thermalRiseSlopePerMin: Double, // deg C per min
    val ambientTempC: Double = 31.0,
    val throttleTempC: Double = 43.0,
    val isCharging: Boolean = false,
    val batteryPct: Int = 80
)

object StiCalculator {

    /**
     * §16.1 Sustainable Throughput Index (STI)
     * Real objective function for picking sustainable runtime configuration.
     */
    fun calculateSti(config: StiConfigCandidate): Double {
        val tg = config.measuredTokenGenSpeed
        val projected5MinTemp = config.ambientTempC + (config.thermalRiseSlopePerMin * 5.0)

        val denominator = max(0.1, config.throttleTempC - config.ambientTempC)
        val numerator = config.throttleTempC - projected5MinTemp
        val thermalMargin = (numerator / denominator).coerceIn(0.0, 1.0)

        val budgetWeight = if (config.isCharging || config.batteryPct > 50) 1.0 else 0.85

        return tg * thermalMargin * budgetWeight
    }
}
