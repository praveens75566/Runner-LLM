package com.example.ui.screens.models

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.ModelCard
import com.example.ui.theme.AmberThermal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenOptimal
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelLibraryScreen(
    viewModel: ModelLibraryViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToBenchmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val models by viewModel.models.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val memoryStatus by viewModel.memoryStatus.collectAsStateWithLifecycle()
    val inspectingModel by viewModel.inspectingModel.collectAsStateWithLifecycle()
    val statusMessage by viewModel.importStatusMessage.collectAsStateWithLifecycle()
    val isImporting by viewModel.isImporting.collectAsStateWithLifecycle()

    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()

    var showHubSection by remember { mutableStateOf(false) }
    var showCustomUrlDialog by remember { mutableStateOf(false) }
    var customUrlInput by remember { mutableStateOf("") }

    // SAF Document Picker for .gguf files (§7.1)
    val safLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importGgufFromUri(it) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Slate950,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyanNeon.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Model Library",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { safLauncher.launch(arrayOf("*/*")) },
                containerColor = CyanNeon,
                contentColor = Slate950,
                icon = { Icon(Icons.Default.FileUpload, contentDescription = "Import") },
                text = { Text("Import .GGUF", fontWeight = FontWeight.Bold) },
                modifier = Modifier.testTag("import_gguf_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Memory & System Preflight Status Banner
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .border(1.dp, CardBorder, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Storage,
                                    contentDescription = null,
                                    tint = CyanNeon,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "DEVICE MEMORY BUDGET",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyanNeon,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                            }
                            Text(
                                text = "${memoryStatus.availableRamMb} MB Available / ${memoryStatus.totalRamMb} MB",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate400,
                                    fontFamily = FontFamily.Monospace
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        LinearProgressIndicator(
                            progress = { memoryStatus.memoryUsageRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (memoryStatus.memoryUsageRatio > 0.85f) AmberThermal else GreenOptimal,
                            trackColor = Slate800
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Quantized GGUF models are loaded via zero-copy mmap with dedicated KV cache allocation and active thermal governance.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = Slate400,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Quick Actions: SAF Import & HuggingFace Hub & URL
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { safLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("import_saf_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Slate850,
                            contentColor = CyanNeon
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pick Local File", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { showHubSection = !showHubSection },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("model_hub_button"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (showHubSection) CyanNeon else Slate700),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hugging Face Hub", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Download Progress Live Card
            downloadProgress?.let { progress ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        colors = CardDefaults.cardColors(containerColor = Slate900)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (progress.isComplete) "DOWNLOAD COMPLETE" else if (progress.isFailed) "DOWNLOAD FAILED" else "DOWNLOADING GGUF MODEL",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (progress.isFailed) AmberThermal else CyanNeon,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (!progress.isComplete && !progress.isFailed) {
                                    Button(
                                        onClick = { viewModel.cancelActiveDownload() },
                                        colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = Slate400),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Cancel", fontSize = 11.sp)
                                    }
                                } else {
                                    IconButton(
                                        onClick = { viewModel.dismissDownloadDialog() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Slate400)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = progress.filename,
                                style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                            )

                            if (progress.isFailed) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = progress.errorMessage ?: "Network error",
                                    style = MaterialTheme.typography.bodySmall.copy(color = AmberThermal)
                                )
                            } else {
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress.progressRatio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = CyanNeon,
                                    trackColor = Slate800
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    val downloadedMb = progress.bytesDownloaded / (1024 * 1024)
                                    val totalMb = if (progress.totalBytes > 0) progress.totalBytes / (1024 * 1024) else 0
                                    Text(
                                        text = if (totalMb > 0) "$downloadedMb MB / $totalMb MB (${(progress.progressRatio * 100).toInt()}%)" else "$downloadedMb MB downloaded",
                                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontFamily = FontFamily.Monospace)
                                    )
                                    if (progress.speedMbPerSec > 0) {
                                        Text(
                                            text = "${String.format(java.util.Locale.US, "%.1f", progress.speedMbPerSec)} MB/s",
                                            style = MaterialTheme.typography.labelSmall.copy(color = GreenOptimal, fontFamily = FontFamily.Monospace)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Hugging Face Hub Online Browser
            if (showHubSection) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .border(1.dp, Slate700, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Slate900)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "VERIFIED HUGGING FACE GGUF MODELS",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = CyanNeon,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Button(
                                    onClick = { showCustomUrlDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Slate800, contentColor = CyanNeon),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Custom URL", fontSize = 11.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            viewModel.hubPresets.forEach { preset ->
                                HubPresetItem(
                                    preset = preset,
                                    onDownload = {
                                        viewModel.downloadHubModel(preset)
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            // Import Progress / Status Message
            if (isImporting || statusMessage != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate800)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isImporting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = CyanNeon,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = statusMessage ?: "Processing...",
                                    style = MaterialTheme.typography.bodySmall.copy(color = Color.White)
                                )
                            }
                            if (!isImporting) {
                                IconButton(onClick = { viewModel.dismissStatusMessage() }) {
                                    Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = Slate400)
                                }
                            }
                        }
                    }
                }
            }

            // Model List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "INSTALLED MODELS (${models.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate400,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // Models List
            items(models, key = { it.id }) { model ->
                ModelCard(
                    model = model,
                    isSelected = model.id == selectedId,
                    onSelect = {
                        viewModel.selectModel(model.id)
                    },
                    onInspectMetadata = {
                        viewModel.inspectModel(model)
                    },
                    onBenchmark = {
                        viewModel.selectModel(model.id)
                        onNavigateToBenchmark()
                    },
                    onToggleFavorite = {
                        viewModel.toggleFavorite(model)
                    },
                    onDelete = {
                        viewModel.deleteModel(model.id)
                    }
                )
            }
        }
    }

    // Inspect GGUF Metadata Sheet / Dialog
    inspectingModel?.let { model ->
        ModelDetailDialog(
            model = model,
            onDismiss = { viewModel.inspectModel(null) }
        )
    }

    // Custom URL Downloader Dialog
    if (showCustomUrlDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showCustomUrlDialog = false },
            containerColor = Slate900,
            title = {
                Text("Download GGUF from URL", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        "Enter the direct download URL for any GGUF quantized model (e.g. from Hugging Face resolve/main link):",
                        style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = customUrlInput,
                        onValueChange = { customUrlInput = it },
                        placeholder = { Text("https://huggingface.co/.../model.gguf", color = Slate700) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = CyanNeon,
                            unfocusedBorderColor = Slate700
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = customUrlInput.trim()
                        if (url.isNotBlank()) {
                            viewModel.downloadFromUrl(url)
                            showCustomUrlDialog = false
                            customUrlInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950)
                ) {
                    Text("Download", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showCustomUrlDialog = false },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Slate400)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HubPresetItem(
    preset: com.example.core.network.HubModelPreset,
    onDownload: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Slate850),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "${preset.paramCount} • ${preset.quantType} • ~${preset.approxSizeMb} MB",
                        style = MaterialTheme.typography.labelSmall.copy(color = CyanNeon)
                    )
                }
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = CyanNeon, contentColor = Slate950),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = preset.description,
                style = MaterialTheme.typography.bodySmall.copy(color = Slate400, fontSize = 11.sp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "HF: ${preset.huggingFaceRepo}",
                style = MaterialTheme.typography.labelSmall.copy(color = Slate700, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            )
        }
    }
}
