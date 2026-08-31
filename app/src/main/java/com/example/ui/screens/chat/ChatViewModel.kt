package com.example.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.inference.GenerationTokenUpdate
import com.example.core.inference.InferenceConfig
import com.example.core.inference.InferenceSessionManager
import com.example.core.inference.SamplingParams
import com.example.core.thermal.ThermalGovernor
import com.example.data.entity.ChatMessageEntity
import com.example.data.entity.ConversationEntity
import com.example.data.entity.ModelEntity
import com.example.data.repository.ChatRepository
import com.example.data.repository.ModelRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val sessionManager: InferenceSessionManager,
    val thermalGovernor: ThermalGovernor
) : ViewModel() {

    val allConversations: StateFlow<List<ConversationEntity>> = chatRepository.allConversations
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val allModels: StateFlow<List<ModelEntity>> = modelRepository.allModels
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _activeConversationId = MutableStateFlow<Long?>(null)
    val activeConversationId: StateFlow<Long?> = _activeConversationId.asStateFlow()

    private val _selectedModelId = MutableStateFlow<Long?>(null)
    val selectedModelId: StateFlow<Long?> = _selectedModelId.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _liveTokenUpdate = MutableStateFlow<GenerationTokenUpdate?>(null)
    val liveTokenUpdate: StateFlow<GenerationTokenUpdate?> = _liveTokenUpdate.asStateFlow()

    private val _inferenceConfig = MutableStateFlow(InferenceConfig())
    val inferenceConfig: StateFlow<InferenceConfig> = _inferenceConfig.asStateFlow()

    private val _samplingParams = MutableStateFlow(SamplingParams())
    val samplingParams: StateFlow<SamplingParams> = _samplingParams.asStateFlow()

    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            allModels.collect { models ->
                if (_selectedModelId.value == null && models.isNotEmpty()) {
                    _selectedModelId.value = models.first().id
                }
            }
        }

        viewModelScope.launch {
            allConversations.collect { convs ->
                if (_activeConversationId.value == null && convs.isNotEmpty()) {
                    _activeConversationId.value = convs.first().id
                } else if (convs.isEmpty()) {
                    // Create default first conversation if database is empty
                    val targetModelId = _selectedModelId.value ?: allModels.value.firstOrNull()?.id
                    val newId = chatRepository.createConversation(
                        title = "New Chat",
                        modelId = targetModelId,
                        systemPrompt = "You are an optimized on-device assistant running fully locally on device hardware."
                    )
                    _activeConversationId.value = newId
                }
            }
        }
        viewModelScope.launch {
            _activeConversationId.collect { convId ->
                if (convId != null) {
                    loadMessages(convId)
                } else {
                    _messages.value = emptyList()
                }
            }
        }
    }

    fun selectModel(modelId: Long) {
        _selectedModelId.value = modelId
        sessionManager.resetSession()
    }

    fun setConfig(config: InferenceConfig) {
        _inferenceConfig.value = config
    }

    fun setPerformanceProfile(profile: com.example.core.inference.PerformanceProfile) {
        _inferenceConfig.value = _inferenceConfig.value.copy(performanceProfile = profile)
    }

    fun toggleFlashSpeed() {
        val current = _inferenceConfig.value.performanceProfile
        val next = if (current == com.example.core.inference.PerformanceProfile.FLASH_TURBO) {
            com.example.core.inference.PerformanceProfile.HIGH_PERFORMANCE
        } else {
            com.example.core.inference.PerformanceProfile.FLASH_TURBO
        }
        setPerformanceProfile(next)
    }

    fun setSampling(params: SamplingParams) {
        _samplingParams.value = params
    }

    fun selectConversation(convId: Long) {
        stopGeneration()
        _activeConversationId.value = convId
        sessionManager.resetSession()
    }

    fun createNewConversation(title: String = "New Chat") {
        stopGeneration()
        viewModelScope.launch {
            val targetModelId = _selectedModelId.value ?: allModels.value.firstOrNull()?.id
            val newId = chatRepository.createConversation(
                title = title,
                modelId = targetModelId,
                systemPrompt = "You are an optimized on-device assistant running fully locally on device hardware."
            )
            _activeConversationId.value = newId
            sessionManager.resetSession()
        }
    }

    fun renameConversation(convId: Long, newTitle: String) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            chatRepository.updateConversationTitle(convId, newTitle.trim())
        }
    }

    fun deleteConversation(convId: Long) {
        stopGeneration()
        viewModelScope.launch {
            chatRepository.deleteConversation(convId)
            if (_activeConversationId.value == convId) {
                val remaining = allConversations.value.filter { it.id != convId }
                if (remaining.isNotEmpty()) {
                    _activeConversationId.value = remaining.first().id
                } else {
                    createNewConversation("New Chat")
                }
            }
        }
    }

    fun clearAllConversations() {
        stopGeneration()
        viewModelScope.launch {
            chatRepository.clearAllConversations()
            createNewConversation("New Chat")
        }
    }

    private fun loadMessages(convId: Long) {
        viewModelScope.launch {
            chatRepository.getMessagesForConversation(convId).collect { msgList ->
                _messages.value = msgList
            }
        }
    }

    fun stopGeneration() {
        sessionManager.cancelGeneration()
        streamJob?.cancel()
        _isGenerating.value = false
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank() || _isGenerating.value) return

        val convId = _activeConversationId.value ?: return
        val currentModel = allModels.value.find { it.id == _selectedModelId.value }
            ?: allModels.value.firstOrNull() ?: return

        viewModelScope.launch {
            val userMsg = ChatMessageEntity(
                conversationId = convId,
                role = "USER",
                content = userText.trim(),
                timestamp = System.currentTimeMillis()
            )
            chatRepository.insertMessage(userMsg)

            // Auto-generate title for conversation from user's first prompt
            val currentConv = allConversations.value.find { it.id == convId }
            if (currentConv != null && (currentConv.title == "New Chat" || currentConv.title == "New On-Device Chat" || currentConv.title.startsWith("Getting Started") || _messages.value.isEmpty())) {
                val cleanTitle = if (userText.trim().length > 32) userText.trim().take(32) + "..." else userText.trim()
                chatRepository.updateConversationTitle(convId, cleanTitle)
            }

            _isGenerating.value = true
            _liveTokenUpdate.value = null

            // Stream response
            streamJob = launch {
                var fullGeneratedText = ""
                var lastUpdate: GenerationTokenUpdate? = null

                sessionManager.generateResponseStream(
                    model = currentModel,
                    conversationId = convId,
                    historyMessages = _messages.value,
                    userPrompt = userText.trim(),
                    systemPrompt = "You are a specialized on-device AI running locally.",
                    config = _inferenceConfig.value,
                    sampling = _samplingParams.value
                ).catch { e ->
                    _isGenerating.value = false
                }.collect { update ->
                    lastUpdate = update
                    _liveTokenUpdate.value = update
                    fullGeneratedText = update.accumulatedText
                }

                // Insert finished message into Room DB
                if (fullGeneratedText.isNotBlank()) {
                    val assistantMsg = ChatMessageEntity(
                        conversationId = convId,
                        role = "ASSISTANT",
                        content = fullGeneratedText,
                        timestamp = System.currentTimeMillis(),
                        tokenCount = lastUpdate?.totalTokens ?: 0,
                        prefillTokPerSec = lastUpdate?.prefillTokensPerSec ?: 0.0,
                        decodeTokPerSec = lastUpdate?.currentTokensPerSec ?: 0.0,
                        timeToFirstTokenMs = lastUpdate?.timeToFirstTokenMs ?: 0L,
                        totalGenTimeMs = 0L,
                        thermalStatus = lastUpdate?.currentThermalState ?: "NORMAL",
                        activeThreads = lastUpdate?.activeThreads ?: 4,
                        backendUsed = lastUpdate?.backendUsed ?: "Auto",
                        kvCacheTokens = lastUpdate?.kvCacheTokens ?: 0
                    )
                    chatRepository.insertMessage(assistantMsg)
                }

                _isGenerating.value = false
            }
        }
    }
}

class ChatViewModelFactory(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val sessionManager: InferenceSessionManager,
    private val thermalGovernor: ThermalGovernor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ChatViewModel(chatRepository, modelRepository, sessionManager, thermalGovernor) as T
    }
}
