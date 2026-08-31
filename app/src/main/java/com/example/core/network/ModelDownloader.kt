package com.example.core.network

import android.content.Context
import android.util.Log
import com.example.core.gguf.GgufParser
import com.example.data.entity.ModelEntity
import com.example.data.repository.ModelRepository
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

data class DownloadProgress(
    val url: String,
    val filename: String,
    val bytesDownloaded: Long,
    val totalBytes: Long,
    val progressRatio: Float, // 0.0 to 1.0
    val speedMbPerSec: Double,
    val isComplete: Boolean = false,
    val isFailed: Boolean = false,
    val errorMessage: String? = null,
    val savedModel: ModelEntity? = null
)

data class HubModelPreset(
    val name: String,
    val filename: String,
    val downloadUrl: String,
    val architecture: String,
    val paramCount: String,
    val quantType: String,
    val approxSizeMb: Long,
    val description: String,
    val huggingFaceRepo: String
)

class ModelDownloader(
    private val context: Context,
    private val modelRepository: ModelRepository
) {
    private val TAG = "ModelDownloader"
    private val cancelRequested = AtomicBoolean(false)
    private val isDownloading = AtomicBoolean(false)

    fun isDownloadActive(): Boolean = isDownloading.get()

    fun cancelDownload() {
        Log.d(TAG, "Download cancellation requested")
        cancelRequested.set(true)
    }

    /**
     * Popular verified GGUF quantized models on Hugging Face suitable for on-device execution.
     */
    val hubPresets: List<HubModelPreset> = listOf(
        HubModelPreset(
            name = "SmolLM2 360M Instruct",
            filename = "smollm2-360m-instruct-q4_k_m.gguf",
            downloadUrl = "https://huggingface.co/HuggingFaceTB/SmolLM2-360M-Instruct-GGUF/resolve/main/smollm2-360m-instruct-q4_k_m.gguf",
            architecture = "llama",
            paramCount = "0.36B",
            quantType = "Q4_K_M",
            approxSizeMb = 229L,
            description = "Ultra-compact high-speed model by Hugging Face TB. Perfect for fast mobile testing.",
            huggingFaceRepo = "HuggingFaceTB/SmolLM2-360M-Instruct-GGUF"
        ),
        HubModelPreset(
            name = "Qwen 2.5 0.5B Instruct",
            filename = "qwen2.5-0.5b-instruct-q4_k_m.gguf",
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct-GGUF/resolve/main/qwen2.5-0.5b-instruct-q4_k_m.gguf",
            architecture = "qwen2",
            paramCount = "0.49B",
            quantType = "Q4_K_M",
            approxSizeMb = 398L,
            description = "Alibaba Qwen 2.5 tiny instruct model. Highly capable for code & structured reasoning.",
            huggingFaceRepo = "Qwen/Qwen2.5-0.5B-Instruct-GGUF"
        ),
        HubModelPreset(
            name = "TinyLlama 1.1B Chat",
            filename = "tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF/resolve/main/tinyllama-1.1b-chat-v1.0.Q4_K_M.gguf",
            architecture = "llama",
            paramCount = "1.1B",
            quantType = "Q4_K_M",
            approxSizeMb = 669L,
            description = "Standard compact Llama architecture benchmark. Great general chat capabilities.",
            huggingFaceRepo = "TheBloke/TinyLlama-1.1B-Chat-v1.0-GGUF"
        ),
        HubModelPreset(
            name = "Llama 3.2 1B Instruct",
            filename = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            architecture = "llama",
            paramCount = "1.23B",
            quantType = "Q4_K_M",
            approxSizeMb = 750L,
            description = "Meta Llama 3.2 lightweight edge model. 128k context support and strong multilingual skills.",
            huggingFaceRepo = "bartowski/Llama-3.2-1B-Instruct-GGUF"
        ),
        HubModelPreset(
            name = "DeepSeek R1 Distill Qwen 1.5B",
            filename = "DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            downloadUrl = "https://huggingface.co/unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF/resolve/main/DeepSeek-R1-Distill-Qwen-1.5B-Q4_K_M.gguf",
            architecture = "qwen2",
            paramCount = "1.78B",
            quantType = "Q4_K_M",
            approxSizeMb = 1120L,
            description = "DeepSeek R1 distilled reasoning model. Generates thorough chain-of-thought solutions.",
            huggingFaceRepo = "unsloth/DeepSeek-R1-Distill-Qwen-1.5B-GGUF"
        )
    )

    /**
     * Downloads a real GGUF file from a URL, saves to local app directory, parses GGUF binary header,
     * and inserts the resulting model entity into Room DB.
     */
    fun downloadGguf(urlStr: String, customFileName: String? = null): Flow<DownloadProgress> = flow {
        if (!isDownloading.compareAndSet(false, true)) {
            emit(DownloadProgress(urlStr, "", 0L, 0L, 0f, 0.0, isFailed = true, errorMessage = "Another download is already in progress"))
            return@flow
        }
        cancelRequested.set(false)

        val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
        val effectiveFileName = customFileName ?: extractFileNameFromUrl(urlStr)
        val targetFile = File(modelsDir, effectiveFileName)

        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        var outputStream: FileOutputStream? = null

        try {
            emit(DownloadProgress(urlStr, effectiveFileName, 0L, -1L, 0f, 0.0))

            val url = URL(urlStr)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", "LLMRunner-Android/1.0")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw IllegalStateException("HTTP $responseCode: ${connection.responseMessage}")
            }

            val totalBytes = connection.contentLengthLong
            inputStream = connection.inputStream
            outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(64 * 1024) // 64KB chunk buffer
            var bytesDownloaded = 0L
            var lastProgressEmitTime = System.currentTimeMillis()
            var bytesSinceLastSample = 0L
            var currentSpeedMbPerSec = 0.0

            while (true) {
                if (cancelRequested.get()) {
                    Log.d(TAG, "Download canceled by user")
                    targetFile.delete()
                    emit(
                        DownloadProgress(
                            urlStr, effectiveFileName, bytesDownloaded, totalBytes,
                            if (totalBytes > 0) bytesDownloaded.toFloat() / totalBytes else 0f,
                            0.0, isFailed = true, errorMessage = "Download canceled by user"
                        )
                    )
                    return@flow
                }

                val read = inputStream.read(buffer)
                if (read == -1) break

                outputStream.write(buffer, 0, read)
                bytesDownloaded += read
                bytesSinceLastSample += read

                val now = System.currentTimeMillis()
                val deltaMs = now - lastProgressEmitTime
                if (deltaMs >= 500L) {
                    val deltaSec = deltaMs / 1000.0
                    currentSpeedMbPerSec = (bytesSinceLastSample / (1024.0 * 1024.0)) / deltaSec
                    lastProgressEmitTime = now
                    bytesSinceLastSample = 0L

                    val ratio = if (totalBytes > 0) (bytesDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0f
                    emit(
                        DownloadProgress(
                            url = urlStr,
                            filename = effectiveFileName,
                            bytesDownloaded = bytesDownloaded,
                            totalBytes = totalBytes,
                            progressRatio = ratio,
                            speedMbPerSec = currentSpeedMbPerSec
                        )
                    )
                }
            }

            outputStream.flush()
            outputStream.close()
            outputStream = null

            // Step 2: Parse real downloaded GGUF binary header
            Log.d(TAG, "Download finished. Parsing GGUF header for: ${targetFile.absolutePath}")
            val parseResult = targetFile.inputStream().use { stream ->
                GgufParser.parseHeader(stream, targetFile.length())
            }

            val metadata = parseResult.getOrNull()
            val modelEntity = ModelEntity(
                name = metadata?.modelName?.ifBlank { null } ?: effectiveFileName.removeSuffix(".gguf").replace("-", " "),
                filename = effectiveFileName,
                filePath = targetFile.absolutePath,
                architecture = metadata?.architecture ?: "llama",
                paramCount = metadata?.estimatedParams ?: "1.0B",
                paramCountBillion = metadata?.estimatedParamsNum ?: 1.0,
                quantType = metadata?.quantizationType ?: "Q4_K_M",
                contextLength = metadata?.contextLength ?: 4096,
                embeddingDim = metadata?.embeddingLength ?: 2048,
                layerCount = metadata?.blockCount ?: 24,
                headCount = metadata?.headCount ?: 16,
                headCountKv = metadata?.headCountKv ?: 8,
                chatTemplate = metadata?.chatTemplate,
                fileSize = targetFile.length(),
                isBundled = false,
                isFavorite = true
            )

            val insertedId = modelRepository.insertModel(modelEntity)
            val finalSaved = modelEntity.copy(id = insertedId)

            emit(
                DownloadProgress(
                    url = urlStr,
                    filename = effectiveFileName,
                    bytesDownloaded = bytesDownloaded,
                    totalBytes = bytesDownloaded,
                    progressRatio = 1.0f,
                    speedMbPerSec = currentSpeedMbPerSec,
                    isComplete = true,
                    savedModel = finalSaved
                )
            )

        } catch (e: Exception) {
            Log.e(TAG, "Failed downloading GGUF model", e)
            targetFile.delete()
            emit(
                DownloadProgress(
                    url = urlStr,
                    filename = effectiveFileName,
                    bytesDownloaded = 0L,
                    totalBytes = 0L,
                    progressRatio = 0f,
                    speedMbPerSec = 0.0,
                    isFailed = true,
                    errorMessage = e.message ?: "Download failed"
                )
            )
        } finally {
            isDownloading.set(false)
            try { inputStream?.close() } catch (_: Exception) {}
            try { outputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun extractFileNameFromUrl(urlStr: String): String {
        return try {
            val path = URL(urlStr).path
            val lastSegment = path.substringAfterLast("/")
            if (lastSegment.endsWith(".gguf", ignoreCase = true)) {
                lastSegment
            } else {
                "${lastSegment.ifBlank { "model" }}.gguf"
            }
        } catch (_: Exception) {
            "downloaded_model.gguf"
        }
    }
}
