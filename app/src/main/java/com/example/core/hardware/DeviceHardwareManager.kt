package com.example.core.hardware

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import java.io.File
import java.io.RandomAccessFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CpuCoreInfo(
    val coreIndex: Int,
    val maxFreqKHz: Long,
    val minFreqKHz: Long,
    val isPrimeOrGold: Boolean // ≥ 2.8 GHz
)

data class CpuTopology(
    val totalCores: Int,
    val primeAndGoldCoresCount: Int, // e.g. 4 cores (X4 + 3.0GHz A720)
    val maxFrequencyGhz: Double,
    val cores: List<CpuCoreInfo>,
    val recommendedDefaultThreads: Int,
    val socDescriptor: String
)

data class MemoryStatus(
    val totalRamMb: Long,
    val availableRamMb: Long,
    val usedRamMb: Long,
    val isLowMemory: Boolean,
    val lowMemoryThresholdMb: Long,
    val memoryUsageRatio: Float
)

data class BatteryAndThermalStatus(
    val batteryPct: Int,
    val isCharging: Boolean,
    val batteryTempC: Float,
    val thermalStatus: Int, // PowerManager.THERMAL_STATUS_*
    val thermalStatusName: String
)

class DeviceHardwareManager(private val context: Context) {
    private val TAG = "HardwareManager"

    fun getCpuTopology(): CpuTopology {
        val cores = mutableListOf<CpuCoreInfo>()
        val runtimeCores = Runtime.getRuntime().availableProcessors()
        var maxFreqGlobal = 0L

        for (i in 0 until runtimeCores) {
            val maxFreq = readCpuFreq("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            val minFreq = readCpuFreq("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_min_freq")
            if (maxFreq > maxFreqGlobal) maxFreqGlobal = maxFreq
            cores.add(
                CpuCoreInfo(
                    coreIndex = i,
                    maxFreqKHz = maxFreq,
                    minFreqKHz = minFreq,
                    isPrimeOrGold = maxFreq >= 2_800_000L || (runtimeCores <= 4 && maxFreq >= 2_000_000L)
                )
            )
        }

        val primeAndGoldCount = cores.count { it.isPrimeOrGold }.coerceAtLeast(1)
        val recommendedThreads = when {
            primeAndGoldCount in 2..6 -> primeAndGoldCount
            runtimeCores >= 8 -> 4 // Avoid saturating mobile memory bus with 8 threads
            runtimeCores >= 4 -> 4
            else -> runtimeCores.coerceAtLeast(1)
        }

        val soc = detectSocDescriptor()

        return CpuTopology(
            totalCores = runtimeCores,
            primeAndGoldCoresCount = primeAndGoldCount,
            maxFrequencyGhz = if (maxFreqGlobal > 0) maxFreqGlobal / 1_000_000.0 else 3.2,
            cores = cores,
            recommendedDefaultThreads = recommendedThreads,
            socDescriptor = soc
        )
    }

    fun getMemoryStatus(): MemoryStatus {
        val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)

        val totalMb = memInfo.totalMem / (1024 * 1024)
        val availMb = memInfo.availMem / (1024 * 1024)
        val usedMb = (memInfo.totalMem - memInfo.availMem) / (1024 * 1024)
        val thresholdMb = memInfo.threshold / (1024 * 1024)
        val ratio = if (totalMb > 0) usedMb.toFloat() / totalMb.toFloat() else 0.5f

        return MemoryStatus(
            totalRamMb = totalMb,
            availableRamMb = availMb,
            usedRamMb = usedMb,
            isLowMemory = memInfo.lowMemory,
            lowMemoryThresholdMb = thresholdMb,
            memoryUsageRatio = ratio
        )
    }

    fun getBatteryAndThermalStatus(): BatteryAndThermalStatus {
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 50
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
        val batteryPct = if (scale > 0) (level * 100) / scale else level

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 300) ?: 300
        val batteryTempC = tempTenths / 10.0f

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val thermalStatus = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager.currentThermalStatus
        } else {
            0
        }

        val thermalName = when (thermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> "NORMAL"
            PowerManager.THERMAL_STATUS_LIGHT -> "LIGHT"
            PowerManager.THERMAL_STATUS_MODERATE -> "MODERATE"
            PowerManager.THERMAL_STATUS_SEVERE -> "SEVERE"
            PowerManager.THERMAL_STATUS_CRITICAL -> "CRITICAL"
            PowerManager.THERMAL_STATUS_EMERGENCY -> "EMERGENCY"
            PowerManager.THERMAL_STATUS_SHUTDOWN -> "SHUTDOWN"
            else -> "NOMINAL"
        }

        return BatteryAndThermalStatus(
            batteryPct = batteryPct,
            isCharging = isCharging,
            batteryTempC = batteryTempC,
            thermalStatus = thermalStatus,
            thermalStatusName = thermalName
        )
    }

    private var cachedBandwidthGbps: Double? = null
    private var lastBandwidthMeasureTime: Long = 0L

    /**
     * Fast non-blocking getter for estimated memory bandwidth.
     */
    fun getEstimatedBandwidthGbps(): Double {
        return cachedBandwidthGbps ?: 58.0
    }

    /**
     * Measures achievable on-device memory bandwidth in GB/s using multi-threaded STREAM kernels.
     * Caches results to ensure zero overhead during high-speed token generation loops.
     */
    suspend fun measureMemoryBandwidthGbps(forceRefresh: Boolean = false): Double = withContext(Dispatchers.Default) {
        val now = System.currentTimeMillis()
        if (!forceRefresh && cachedBandwidthGbps != null && (now - lastBandwidthMeasureTime) < 60_000L) {
            return@withContext cachedBandwidthGbps!!
        }

        try {
            val cores = Runtime.getRuntime().availableProcessors().coerceIn(2, 8)
            val chunkSize = 4 * 1024 * 1024 // 4MB per worker chunk for fast, low-overhead calibration
            val iterations = 6
            val threads = mutableListOf<Thread>()
            val measuredSpeeds = java.util.concurrent.CopyOnWriteArrayList<Double>()

            for (c in 0 until cores) {
                val thread = Thread {
                    val a = FloatArray(chunkSize / 4) { it.toFloat() }
                    val b = FloatArray(chunkSize / 4) { it.toFloat() * 1.5f }
                    val cArr = FloatArray(chunkSize / 4)
                    val scalar = 3.14159f

                    // Warmup
                    for (i in 0 until a.size step 32) {
                        cArr[i] = a[i] + scalar * b[i]
                    }

                    val start = System.nanoTime()
                    for (iter in 0 until iterations) {
                        // Triad kernel: C[i] = A[i] + scalar * B[i]
                        var i = 0
                        while (i < a.size) {
                            cArr[i] = a[i] + scalar * b[i]
                            i++
                        }
                    }
                    val elapsedNs = System.nanoTime() - start
                    val elapsedSec = elapsedNs / 1_000_000_000.0
                    val bytesProcessed = (a.size.toDouble() * 12.0 * iterations)
                    val gbProcessed = bytesProcessed / (1024.0 * 1024.0 * 1024.0)
                    if (elapsedSec > 0) {
                        measuredSpeeds.add(gbProcessed / elapsedSec)
                    }
                }
                threads.add(thread)
                thread.start()
            }

            threads.forEach { it.join(1500) }

            val totalAggregateGbps = measuredSpeeds.sum()
            val finalGbps = totalAggregateGbps.coerceIn(22.0, 120.0)
            cachedBandwidthGbps = finalGbps
            lastBandwidthMeasureTime = now
            Log.d(TAG, "Measured STREAM memory bandwidth: $finalGbps GB/s across $cores threads")
            finalGbps
        } catch (e: Exception) {
            Log.e(TAG, "Error measuring memory bandwidth", e)
            val fallback = 58.0
            cachedBandwidthGbps = fallback
            fallback
        }
    }

    private fun readCpuFreq(path: String): Long {
        return try {
            val file = File(path)
            if (file.exists()) {
                RandomAccessFile(file, "r").use { raf ->
                    raf.readLine()?.trim()?.toLongOrNull() ?: 0L
                }
            } else {
                0L
            }
        } catch (e: Exception) {
            0L
        }
    }

    private fun detectSocDescriptor(): String {
        val hardware = Build.HARDWARE
        val board = Build.BOARD
        val model = Build.MODEL
        val manufacturer = Build.MANUFACTURER

        return when {
            hardware.contains("qcom", ignoreCase = true) || board.contains("qcom", ignoreCase = true) || board.contains("sm8735", ignoreCase = true) ->
                "Snapdragon 8s Gen 4 (Adreno 825)"
            hardware.contains("exynos", ignoreCase = true) -> "Samsung Exynos (Xclipse GPU)"
            hardware.contains("mt", ignoreCase = true) || hardware.contains("dimensity", ignoreCase = true) -> "MediaTek Dimensity"
            hardware.contains("tensor", ignoreCase = true) -> "Google Tensor"
            else -> "$manufacturer $model ($hardware)"
        }
    }
}
