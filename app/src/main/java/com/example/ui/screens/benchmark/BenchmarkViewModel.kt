package com.example.ui.screens.benchmark

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.benchmark.BenchmarkProgress
import com.example.core.benchmark.BenchmarkRunner
import com.example.core.benchmark.RooflineEstimate
import com.example.core.benchmark.RooflineEstimator
import com.example.core.hardware.DeviceHardwareManager
import com.example.core.inference.ExecutionBackend
import com.example.core.inference.KvPrecision
import com.example.data.entity.BenchmarkResultEntity
import com.example.data.entity.ModelEntity
import com.example.data.repository.BenchmarkRepository
import com.example.data.repository.ModelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BenchmarkViewModel(
    private val benchmarkRepository: BenchmarkRepository,
    private val modelRepository: ModelRepository,
    private val benchmarkRunner: BenchmarkRunner,
    private val hardwareManager: DeviceHardwareManager
) : ViewModel() {

    val allModels: StateFlow<List<ModelEntity>> = modelRepository.allModels
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val benchmarkHistory: StateFlow<List<BenchmarkResultEntity>> = benchmarkRepository.allBenchmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedModelId = MutableStateFlow<Long?>(1L)
    val selectedModelId: StateFlow<Long?> = _selectedModelId.asStateFlow()

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _currentProgress = MutableStateFlow<BenchmarkProgress?>(null)
    val currentProgress: StateFlow<BenchmarkProgress?> = _currentProgress.asStateFlow()

    private val _memoryBandwidthGbps = MutableStateFlow(52.0)
    val memoryBandwidthGbps: StateFlow<Double> = _memoryBandwidthGbps.asStateFlow()

    private val _evalContextLength = MutableStateFlow(2048)
    val evalContextLength: StateFlow<Int> = _evalContextLength.asStateFlow()

    private var benchmarkJob: Job? = null

    init {
        viewModelScope.launch {
            val bw = hardwareManager.measureMemoryBandwidthGbps()
            _memoryBandwidthGbps.value = bw
        }
    }

    fun selectModel(id: Long) {
        _selectedModelId.value = id
    }

    fun setEvalContext(ctx: Int) {
        _evalContextLength.value = ctx
    }

    fun getRooflineEstimate(model: ModelEntity): RooflineEstimate {
        return RooflineEstimator.estimateDecodeSpeed(
            model = model,
            effectiveBandwidthGbps = _memoryBandwidthGbps.value,
            contextLength = _evalContextLength.value
        )
    }

    fun runFullBenchmark() {
        val model = allModels.value.find { it.id == _selectedModelId.value }
            ?: allModels.value.firstOrNull() ?: return

        _isRunning.value = true
        _currentProgress.value = null

        benchmarkJob = viewModelScope.launch {
            benchmarkRunner.runFullSweep(
                model = model,
                backend = ExecutionBackend.AUTO,
                kvPrecision = KvPrecision.Q8_0
            ).collect { progress ->
                _currentProgress.value = progress
                if (progress.isComplete) {
                    _isRunning.value = false
                }
            }
        }
    }

    fun runThermalStressTest() {
        val model = allModels.value.find { it.id == _selectedModelId.value }
            ?: allModels.value.firstOrNull() ?: return

        _isRunning.value = true
        _currentProgress.value = null

        benchmarkJob = viewModelScope.launch {
            benchmarkRunner.runThermalStressTest(
                model = model,
                durationMinutes = 1
            ).collect { progress ->
                _currentProgress.value = progress
                if (progress.isComplete) {
                    _isRunning.value = false
                }
            }
        }
    }

    fun cancelBenchmark() {
        benchmarkJob?.cancel()
        _isRunning.value = false
        _currentProgress.value = null
    }

    fun clearHistory() {
        viewModelScope.launch {
            benchmarkRepository.clearAllBenchmarks()
        }
    }
}

class BenchmarkViewModelFactory(
    private val benchmarkRepository: BenchmarkRepository,
    private val modelRepository: ModelRepository,
    private val benchmarkRunner: BenchmarkRunner,
    private val hardwareManager: DeviceHardwareManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return BenchmarkViewModel(benchmarkRepository, modelRepository, benchmarkRunner, hardwareManager) as T
    }
}
