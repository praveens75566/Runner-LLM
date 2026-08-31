package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.core.hardware.CpuTopology
import com.example.core.hardware.DeviceHardwareManager
import com.example.core.hardware.MemoryStatus
import com.example.core.inference.ExecutionBackend
import com.example.core.inference.InferenceConfig
import com.example.core.inference.KvPrecision
import com.example.core.inference.SamplingParams
import com.example.core.thermal.GovernorMode
import com.example.core.thermal.ThermalGovernor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(
    private val hardwareManager: DeviceHardwareManager,
    private val thermalGovernor: ThermalGovernor
) : ViewModel() {

    private val _config = MutableStateFlow(InferenceConfig())
    val config: StateFlow<InferenceConfig> = _config.asStateFlow()

    private val _sampling = MutableStateFlow(SamplingParams())
    val sampling: StateFlow<SamplingParams> = _sampling.asStateFlow()

    private val _governorMode = MutableStateFlow(GovernorMode.PID_CONTINUOUS)
    val governorMode: StateFlow<GovernorMode> = _governorMode.asStateFlow()

    private val _cpuTopology = MutableStateFlow(hardwareManager.getCpuTopology())
    val cpuTopology: StateFlow<CpuTopology> = _cpuTopology.asStateFlow()

    private val _memoryStatus = MutableStateFlow(hardwareManager.getMemoryStatus())
    val memoryStatus: StateFlow<MemoryStatus> = _memoryStatus.asStateFlow()

    fun setBackend(backend: ExecutionBackend) {
        _config.value = _config.value.copy(backend = backend)
    }

    fun setThreadCount(threads: Int) {
        _config.value = _config.value.copy(threadCount = threads)
    }

    fun setKvPrecision(precision: KvPrecision) {
        _config.value = _config.value.copy(kvPrecision = precision)
    }

    fun setPerformanceProfile(profile: com.example.core.inference.PerformanceProfile) {
        _config.value = _config.value.copy(performanceProfile = profile)
    }

    fun setContextLength(ctx: Int) {
        _config.value = _config.value.copy(contextLength = ctx)
    }

    fun setMmapEnabled(enabled: Boolean) {
        _config.value = _config.value.copy(isMmapEnabled = enabled)
    }

    fun setMadvisePrewarm(enabled: Boolean) {
        _config.value = _config.value.copy(isMadvisePrewarmEnabled = enabled)
    }

    fun setFlashAttention(enabled: Boolean) {
        _config.value = _config.value.copy(flashAttentionEnabled = enabled)
    }

    fun setTargetTemp(tempC: Float) {
        _config.value = _config.value.copy(targetTempC = tempC)
    }

    fun setGovernorMode(mode: GovernorMode) {
        _governorMode.value = mode
        thermalGovernor.setGovernorMode(mode)
    }

    fun updateSampling(
        minP: Float = _sampling.value.minP,
        temp: Float = _sampling.value.temperature,
        topK: Int = _sampling.value.topK,
        topP: Float = _sampling.value.topP,
        repPenalty: Float = _sampling.value.repetitionPenalty,
        maxTokens: Int = _sampling.value.maxTokens
    ) {
        _sampling.value = _sampling.value.copy(
            minP = minP,
            temperature = temp,
            topK = topK,
            topP = topP,
            repetitionPenalty = repPenalty,
            maxTokens = maxTokens
        )
    }
}

class SettingsViewModelFactory(
    private val hardwareManager: DeviceHardwareManager,
    private val thermalGovernor: ThermalGovernor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(hardwareManager, thermalGovernor) as T
    }
}
