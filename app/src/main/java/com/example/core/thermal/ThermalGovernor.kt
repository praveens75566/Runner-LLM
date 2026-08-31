package com.example.core.thermal

import android.content.Context
import android.os.Build
import android.os.PowerManager
import android.util.Log
import com.example.core.hardware.DeviceHardwareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class GovernorMode {
    PID_CONTINUOUS,  // §16.2 Continuous loop to prevent oscillation
    DISCRETE_THRESHOLDS // §7.6 Baseline state machine
}

data class ThermalControlState(
    val currentTempC: Float,
    val targetTempC: Float = 41.0f,
    val throttleTempC: Float = 45.0f,
    val thermalStatus: Int = 0,
    val thermalStatusName: String = "NORMAL",
    val effortBudget: Float = 1.0f, // 0.2 to 1.0
    val activeThreads: Int = 4,
    val isThrottlingActive: Boolean = false,
    val governorMode: GovernorMode = GovernorMode.PID_CONTINUOUS
)

/**
 * §7.6 & §16.2 Thermal-Aware Adaptive Controller
 * Monitors battery & SoC thermal status, continuously computes PID feedback,
 * and dynamically scales active CPU threads & GPU layer offload before the OS hard-throttles.
 */
class ThermalGovernor(
    private val context: Context,
    private val hardwareManager: DeviceHardwareManager
) {
    private val TAG = "ThermalGovernor"

    private val _thermalState = MutableStateFlow(ThermalControlState(currentTempC = 34.0f))
    val thermalState: StateFlow<ThermalControlState> = _thermalState.asStateFlow()

    // PID constants tuned for mobile thermal inertia (§16.2)
    private var kp: Float = 0.08f
    private var ki: Float = 0.005f
    private var kd: Float = 0.04f

    private var integralError: Float = 0.0f
    private var lastError: Float = 0.0f
    private var lastTimestampNs: Long = System.nanoTime()

    private var powerManagerListener: PowerManager.OnThermalStatusChangedListener? = null

    init {
        registerThermalListener()
    }

    private fun registerThermalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val listener = PowerManager.OnThermalStatusChangedListener { status ->
                Log.d(TAG, "OS Thermal status changed: $status")
                updateThermalMetrics()
            }
            pm?.addThermalStatusListener(listener)
            powerManagerListener = listener
        }
    }

    /**
     * Called periodically during generation (e.g. every 1.5 - 2.5s) to evaluate PID control loop.
     */
    fun evaluateThermalStep(baseThreads: Int, targetTempC: Float = 41.0f): ThermalControlState {
        val battInfo = hardwareManager.getBatteryAndThermalStatus()
        val currentTemp = battInfo.batteryTempC
        val now = System.nanoTime()
        val dt = ((now - lastTimestampNs) / 1_000_000_000.0f).coerceIn(0.1f, 5.0f)
        lastTimestampNs = now

        val error = targetTempC - currentTemp
        integralError = (integralError + error * dt).coerceIn(-20.0f, 20.0f)
        val derivative = (error - lastError) / dt
        lastError = error

        val pTerm = kp * error
        val iTerm = ki * integralError
        val dTerm = kd * derivative

        // Higher temp -> lower error -> lower effort budget
        val rawBudget = (1.0f + pTerm + iTerm + dTerm).coerceIn(0.25f, 1.0f)

        // Map effort budget to active thread count
        val calculatedThreads = when {
            rawBudget >= 0.85f -> baseThreads
            rawBudget >= 0.65f -> (baseThreads - 1).coerceAtLeast(2)
            rawBudget >= 0.45f -> (baseThreads - 2).coerceAtLeast(1)
            else -> 1
        }

        val isThrottling = battInfo.thermalStatus >= PowerManager.THERMAL_STATUS_MODERATE || currentTemp >= 42.5f

        val updated = ThermalControlState(
            currentTempC = currentTemp,
            targetTempC = targetTempC,
            thermalStatus = battInfo.thermalStatus,
            thermalStatusName = battInfo.thermalStatusName,
            effortBudget = rawBudget,
            activeThreads = calculatedThreads,
            isThrottlingActive = isThrottling,
            governorMode = _thermalState.value.governorMode
        )

        _thermalState.value = updated
        return updated
    }

    fun startMonitoring() {
        registerThermalListener()
    }

    fun stopMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            powerManagerListener?.let { pm?.removeThermalStatusListener(it) }
            powerManagerListener = null
        }
    }

    fun setGovernorMode(mode: GovernorMode) {
        _thermalState.value = _thermalState.value.copy(governorMode = mode)
    }

    private fun updateThermalMetrics() {
        evaluateThermalStep(baseThreads = _thermalState.value.activeThreads)
    }
}
