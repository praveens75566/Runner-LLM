package com.example.ui.screens.models

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.core.gguf.GgufMetadata
import com.example.core.gguf.GgufParser
import com.example.core.hardware.DeviceHardwareManager
import com.example.core.hardware.MemoryStatus
import com.example.data.entity.ModelEntity
import com.example.data.repository.ModelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ImportPreflightResult(
    val fitsInMemory: Boolean,
    val memoryUsagePctIfLoaded: Float,
    val warningMessage: String?,
    val metadata: GgufMetadata
)

class ModelLibraryViewModel(
    private val context: Context,
    private val modelRepository: ModelRepository,
    private val hardwareManager: DeviceHardwareManager
) : ViewModel() {
    private val TAG = "ModelLibraryVM"
    private val modelDownloader = com.example.core.network.ModelDownloader(context, modelRepository)

    val hubPresets: List<com.example.core.network.HubModelPreset> = modelDownloader.hubPresets

    val models: StateFlow<List<ModelEntity>> = modelRepository.allModels
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedModelId = MutableStateFlow<Long?>(1L)
    val selectedModelId: StateFlow<Long?> = _selectedModelId.asStateFlow()

    private val _memoryStatus = MutableStateFlow(hardwareManager.getMemoryStatus())
    val memoryStatus: StateFlow<MemoryStatus> = _memoryStatus.asStateFlow()

    private val _inspectingModel = MutableStateFlow<ModelEntity?>(null)
    val inspectingModel: StateFlow<ModelEntity?> = _inspectingModel.asStateFlow()

    private val _importStatusMessage = MutableStateFlow<String?>(null)
    val importStatusMessage: StateFlow<String?> = _importStatusMessage.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _downloadProgress = MutableStateFlow<com.example.core.network.DownloadProgress?>(null)
    val downloadProgress: StateFlow<com.example.core.network.DownloadProgress?> = _downloadProgress.asStateFlow()

    init {
        refreshMemoryStatus()
    }

    fun refreshMemoryStatus() {
        _memoryStatus.value = hardwareManager.getMemoryStatus()
    }

    fun selectModel(id: Long) {
        _selectedModelId.value = id
    }

    fun inspectModel(model: ModelEntity?) {
        _inspectingModel.value = model
    }

    fun toggleFavorite(model: ModelEntity) {
        viewModelScope.launch {
            modelRepository.updateModel(model.copy(isFavorite = !model.isFavorite))
        }
    }

    fun deleteModel(id: Long) {
        viewModelScope.launch {
            val target = models.value.firstOrNull { it.id == id }
            if (target != null && !target.filePath.startsWith("local://") && !target.filePath.startsWith("content://")) {
                try {
                    val file = java.io.File(target.filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed deleting local model file", e)
                }
            }
            modelRepository.deleteModel(id)
            if (_selectedModelId.value == id) {
                _selectedModelId.value = models.value.firstOrNull { it.id != id }?.id
            }
            refreshMemoryStatus()
        }
    }

    fun dismissStatusMessage() {
        _importStatusMessage.value = null
    }

    fun dismissDownloadDialog() {
        _downloadProgress.value = null
    }

    fun cancelActiveDownload() {
        modelDownloader.cancelDownload()
    }

    fun downloadHubModel(preset: com.example.core.network.HubModelPreset) {
        downloadFromUrl(preset.downloadUrl, preset.filename)
    }

    fun downloadFromUrl(url: String, customFileName: String? = null) {
        viewModelScope.launch {
            modelDownloader.downloadGguf(url, customFileName).collect { progress ->
                _downloadProgress.value = progress
                if (progress.isComplete && progress.savedModel != null) {
                    _selectedModelId.value = progress.savedModel.id
                    _importStatusMessage.value = "Downloaded & verified ${progress.savedModel.name} (${progress.savedModel.paramCount})"
                    refreshMemoryStatus()
                } else if (progress.isFailed) {
                    _importStatusMessage.value = progress.errorMessage ?: "Download failed"
                }
            }
        }
    }

    /**
     * Imports a user-selected GGUF file from SAF Uri (§7.1)
     * Performs binary header parse & pre-flight memory check.
     */
    fun importGgufFromUri(uri: Uri) {
        viewModelScope.launch {
            _isImporting.value = true
            _importStatusMessage.value = "Reading GGUF binary header..."

            withContext(Dispatchers.IO) {
                try {
                    val contentResolver = context.contentResolver
                    var filename = "imported_model.gguf"
                    var fileSize = 0L

                    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                        if (cursor.moveToFirst()) {
                            if (nameIndex != -1) filename = cursor.getString(nameIndex)
                            if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex)
                        }
                    }

                    val inputStream = contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Unable to open input stream for selected file")

                    val parseResult = GgufParser.parseHeader(inputStream, fileSize)
                    inputStream.close()

                    parseResult.onSuccess { metadata ->
                        // Preflight RAM check (§7.1)
                        val memInfo = hardwareManager.getMemoryStatus()
                        val modelSizeMb = fileSize / (1024 * 1024)
                        val fits = (modelSizeMb + 1200) < memInfo.availableRamMb

                        val entity = ModelEntity(
                            name = metadata.modelName.ifBlank { filename.removeSuffix(".gguf") },
                            filename = filename,
                            filePath = uri.toString(),
                            architecture = metadata.architecture,
                            paramCount = metadata.estimatedParams,
                            paramCountBillion = metadata.estimatedParamsNum,
                            quantType = metadata.quantizationType,
                            contextLength = metadata.contextLength,
                            embeddingDim = metadata.embeddingLength,
                            layerCount = metadata.blockCount,
                            headCount = metadata.headCount,
                            headCountKv = metadata.headCountKv,
                            chatTemplate = metadata.chatTemplate,
                            fileSize = fileSize,
                            isBundled = false,
                            isFavorite = true
                        )

                        val newId = modelRepository.insertModel(entity)
                        _selectedModelId.value = newId
                        _importStatusMessage.value = if (fits) {
                            "Successfully imported ${entity.name} (${metadata.estimatedParams}, ${metadata.quantizationType})"
                        } else {
                            "Imported ${entity.name}. Warning: Available RAM (${memInfo.availableRamMb}MB) is tight for this model (${modelSizeMb}MB)."
                        }
                    }.onFailure { error ->
                        Log.e(TAG, "GGUF header parse failed", error)
                        _importStatusMessage.value = "Failed to parse GGUF header: ${error.message}"
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Import error", e)
                    _importStatusMessage.value = "Import failed: ${e.message}"
                } finally {
                    _isImporting.value = false
                    refreshMemoryStatus()
                }
            }
        }
    }

    fun addPresetModel(name: String, arch: String, params: String, quant: String, sizeMb: Long) {
        viewModelScope.launch {
            val entity = ModelEntity(
                name = name,
                filename = "${name.lowercase().replace(" ", "-")}.gguf",
                filePath = "local://presets/${name.lowercase().replace(" ", "-")}.gguf",
                architecture = arch,
                paramCount = params,
                paramCountBillion = params.removeSuffix("B").toDoubleOrNull() ?: 3.0,
                quantType = quant,
                contextLength = 4096,
                embeddingDim = 2560,
                layerCount = 28,
                headCount = 20,
                headCountKv = 4,
                chatTemplate = "{% for message in messages %}{{'<|im_start|>' + message['role'] + '\n' + message['content'] + '<|im_end|>' + '\n'}}{% endfor %}{% if add_generation_prompt %}{{'<|im_start|>assistant\n'}}{% endif %}",
                fileSize = sizeMb * 1024 * 1024L,
                isBundled = false,
                isFavorite = false
            )
            val id = modelRepository.insertModel(entity)
            _selectedModelId.value = id
            _importStatusMessage.value = "Added preset: $name"
        }
    }
}

class ModelLibraryViewModelFactory(
    private val context: Context,
    private val modelRepository: ModelRepository,
    private val hardwareManager: DeviceHardwareManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ModelLibraryViewModel(context, modelRepository, hardwareManager) as T
    }
}
