package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.core.benchmark.BenchmarkRunner
import com.example.core.hardware.DeviceHardwareManager
import com.example.core.inference.InferenceSessionManager
import com.example.core.thermal.ThermalGovernor
import com.example.data.db.AppDatabase
import com.example.data.repository.BenchmarkRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ModelRepository
import com.example.ui.navigation.AppNavigation
import com.example.ui.screens.benchmark.BenchmarkViewModel
import com.example.ui.screens.benchmark.BenchmarkViewModelFactory
import com.example.ui.screens.chat.ChatViewModel
import com.example.ui.screens.chat.ChatViewModelFactory
import com.example.ui.screens.models.ModelLibraryViewModel
import com.example.ui.screens.models.ModelLibraryViewModelFactory
import com.example.ui.screens.settings.SettingsViewModel
import com.example.ui.screens.settings.SettingsViewModelFactory
import com.example.ui.theme.LLMRunnerTheme
import com.example.ui.theme.Slate950

class MainActivity : ComponentActivity() {

    private lateinit var hardwareManager: DeviceHardwareManager
    private lateinit var thermalGovernor: ThermalGovernor
    private lateinit var sessionManager: InferenceSessionManager
    private lateinit var benchmarkRunner: BenchmarkRunner

    private lateinit var database: AppDatabase
    private lateinit var modelRepository: ModelRepository
    private lateinit var chatRepository: ChatRepository
    private lateinit var benchmarkRepository: BenchmarkRepository

    private val chatViewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(chatRepository, modelRepository, sessionManager, thermalGovernor)
    }

    private val modelLibraryViewModel: ModelLibraryViewModel by viewModels {
        ModelLibraryViewModelFactory(applicationContext, modelRepository, hardwareManager)
    }

    private val benchmarkViewModel: BenchmarkViewModel by viewModels {
        BenchmarkViewModelFactory(benchmarkRepository, modelRepository, benchmarkRunner, hardwareManager)
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModelFactory(hardwareManager, thermalGovernor)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Singletons & Architecture Components
        database = AppDatabase.getInstance(applicationContext)
        modelRepository = ModelRepository(database.modelDao())
        chatRepository = ChatRepository(database.conversationDao(), database.chatMessageDao())
        benchmarkRepository = BenchmarkRepository(database.benchmarkDao(), database.thermalLogDao())

        hardwareManager = DeviceHardwareManager(applicationContext)
        thermalGovernor = ThermalGovernor(applicationContext, hardwareManager)
        sessionManager = InferenceSessionManager(applicationContext, hardwareManager, thermalGovernor)
        benchmarkRunner = BenchmarkRunner(applicationContext, hardwareManager, benchmarkRepository)

        // 2. Start thermal telemetry background loop
        thermalGovernor.startMonitoring()

        setContent {
            LLMRunnerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Slate950
                ) {
                    AppNavigation(
                        chatViewModel = chatViewModel,
                        modelLibraryViewModel = modelLibraryViewModel,
                        benchmarkViewModel = benchmarkViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        thermalGovernor.stopMonitoring()
    }
}
