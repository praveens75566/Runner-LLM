package com.example.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.core.inference.ExecutionBackend
import com.example.core.inference.KvPrecision
import com.example.core.thermal.GovernorMode
import com.example.ui.theme.AmberThermal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenOptimal
import com.example.ui.theme.PurpleNeural
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val sampling by viewModel.sampling.collectAsStateWithLifecycle()
    val governorMode by viewModel.governorMode.collectAsStateWithLifecycle()
    val cpuTopology by viewModel.cpuTopology.collectAsStateWithLifecycle()
    val memoryStatus by viewModel.memoryStatus.collectAsStateWithLifecycle()

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
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Settings & Tuning",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Hardware Profile Summary Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DETECTED HARDWARE TOPOLOGY",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyanNeon,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cpuTopology.socDescriptor,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${cpuTopology.totalCores} Cores (Max ${String.format(java.util.Locale.US, "%.2f", cpuTopology.maxFrequencyGhz)} GHz) • ${memoryStatus.totalRamMb} MB LPDDR5 RAM",
                            style = MaterialTheme.typography.bodySmall.copy(color = Slate400)
                        )
                    }
                }
            }

            // Section 1: Performance Engine & Flash Speed Profile
            item {
                SettingsSectionHeader("PERFORMANCE & EXECUTION SPEED")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Execution Profile:",
                            style = MaterialTheme.typography.bodyMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        com.example.core.inference.PerformanceProfile.entries.forEach { profile ->
                            val isSelected = config.performanceProfile == profile
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(
                                        width = if (isSelected) 1.5.dp else 1.dp,
                                        color = if (isSelected) CyanNeon else Slate800,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { viewModel.setPerformanceProfile(profile) },
                                color = if (isSelected) CyanNeon.copy(alpha = 0.12f) else Slate850
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = profile.displayName,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (isSelected) CyanNeon else Color.White,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = profile.description,
                                            style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CyanNeon)
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = "ACTIVE",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = Slate950,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 9.sp
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Execution Backend (§6.3)
            item {
                SettingsSectionHeader("EXECUTION BACKEND & ACCELERATOR")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ExecutionBackend.entries.forEach { backend ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = backend.displayName,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = if (config.backend == backend) CyanNeon else Color.White,
                                            fontWeight = if (config.backend == backend) FontWeight.Bold else FontWeight.Normal
                                        )
                                    )
                                    Text(
                                        text = backend.chipTarget,
                                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                                    )
                                }

                                FilterChip(
                                    selected = config.backend == backend,
                                    onClick = { viewModel.setBackend(backend) },
                                    label = { Text(if (config.backend == backend) "ACTIVE" else "SELECT") },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = CyanNeon,
                                        selectedLabelColor = Slate950
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Threading & Core Affinity (§7.4)
            item {
                SettingsSectionHeader("CPU THREADING & CORE AFFINITY")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Active Inference Threads:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                            Text(
                                "${config.threadCount} Threads (Optimum: ${cpuTopology.recommendedDefaultThreads})",
                                style = MaterialTheme.typography.bodyMedium.copy(color = CyanNeon, fontWeight = FontWeight.Bold)
                            )
                        }

                        Slider(
                            value = config.threadCount.toFloat(),
                            onValueChange = { viewModel.setThreadCount(it.toInt()) },
                            valueRange = 1f..cpuTopology.totalCores.toFloat().coerceAtLeast(8f),
                            steps = cpuTopology.totalCores.coerceAtLeast(8) - 2,
                            colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
                        )

                        Text(
                            text = "Note: Allocating all 8 cores causes memory bus contention and increases thermal throttle speed. 4 Prime/Gold cores provide maximum sustainable throughput.",
                            style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                        )
                    }
                }
            }

            // Section 3: Memory & KV Cache Management (§7.5)
            item {
                SettingsSectionHeader("MEMORY & KV CACHE PRECISION")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("KV Cache Quantization:", style = MaterialTheme.typography.bodySmall.copy(color = Slate400))
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KvPrecision.entries.forEach { prec ->
                                FilterChip(
                                    selected = config.kvPrecision == prec,
                                    onClick = { viewModel.setKvPrecision(prec) },
                                    label = { Text(prec.name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PurpleNeural,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Max Context Length (n_ctx):", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                            Text("${config.contextLength} tokens", style = MaterialTheme.typography.bodyMedium.copy(color = PurpleNeural, fontWeight = FontWeight.Bold))
                        }

                        Slider(
                            value = config.contextLength.toFloat(),
                            onValueChange = { viewModel.setContextLength(it.toInt()) },
                            valueRange = 512f..16384f,
                            steps = 14,
                            colors = SliderDefaults.colors(thumbColor = PurpleNeural, activeTrackColor = PurpleNeural)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        SettingToggleRow("mmap Zero-Copy Model Loading", "Eliminates duplicate RAM allocations", config.isMmapEnabled) {
                            viewModel.setMmapEnabled(it)
                        }

                        SettingToggleRow("madvise(MADV_WILLNEED) Pre-warm", "Pages in weights during idle", config.isMadvisePrewarmEnabled) {
                            viewModel.setMadvisePrewarm(it)
                        }

                        SettingToggleRow("Flash Attention Kernel", "Reduces attention memory bandwidth traffic", config.flashAttentionEnabled) {
                            viewModel.setFlashAttention(it)
                        }
                    }
                }
            }

            // Section 4: Thermal Adaptive Governor (§7.6 & §16.2)
            item {
                SettingsSectionHeader("THERMAL GOVERNOR & CONTROL LOOP")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Governor Mode:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(
                                    selected = governorMode == GovernorMode.PID_CONTINUOUS,
                                    onClick = { viewModel.setGovernorMode(GovernorMode.PID_CONTINUOUS) },
                                    label = { Text("PID Continuous", fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AmberThermal,
                                        selectedLabelColor = Slate950
                                    )
                                )
                                FilterChip(
                                    selected = governorMode == GovernorMode.DISCRETE_THRESHOLDS,
                                    onClick = { viewModel.setGovernorMode(GovernorMode.DISCRETE_THRESHOLDS) },
                                    label = { Text("Discrete", fontSize = 11.sp) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Target Ceiling Temperature:", style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
                            Text("${String.format(java.util.Locale.US, "%.1f", config.targetTempC)}°C", style = MaterialTheme.typography.bodyMedium.copy(color = AmberThermal, fontWeight = FontWeight.Bold))
                        }

                        Slider(
                            value = config.targetTempC,
                            onValueChange = { viewModel.setTargetTemp(it) },
                            valueRange = 38f..45f,
                            steps = 14,
                            colors = SliderDefaults.colors(thumbColor = AmberThermal, activeTrackColor = AmberThermal)
                        )
                    }
                }
            }

            // Section 5: Sampling Parameters (§17.5)
            item {
                SettingsSectionHeader("SAMPLING & GENERATION PARAMETERS")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate900)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SamplingSliderRow("Min-P Sampling (Adaptive Cutoff)", String.format(java.util.Locale.US, "%.2f", sampling.minP), sampling.minP, 0.01f..0.20f) {
                            viewModel.updateSampling(minP = it)
                        }
                        SamplingSliderRow("Temperature", String.format(java.util.Locale.US, "%.2f", sampling.temperature), sampling.temperature, 0.0f..1.5f) {
                            viewModel.updateSampling(temp = it)
                        }
                        SamplingSliderRow("Repetition Penalty", String.format(java.util.Locale.US, "%.2f", sampling.repetitionPenalty), sampling.repetitionPenalty, 1.0f..1.5f) {
                            viewModel.updateSampling(repPenalty = it)
                        }
                        SamplingSliderRow("Max Generation Tokens", "${sampling.maxTokens}", sampling.maxTokens.toFloat(), 128f..4096f) {
                            viewModel.updateSampling(maxTokens = it.toInt())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelSmall.copy(
            color = Slate400,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        ),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(color = Color.White))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall.copy(color = Slate400))
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = CyanNeon, checkedTrackColor = Slate800)
        )
    }
}

@Composable
private fun SamplingSliderRow(
    title: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodySmall.copy(color = Slate400))
            Text(valueText, style = MaterialTheme.typography.bodySmall.copy(color = Color.White, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace))
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(thumbColor = CyanNeon, activeTrackColor = CyanNeon)
        )
    }
}
