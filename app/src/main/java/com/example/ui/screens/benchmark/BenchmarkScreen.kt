package com.example.ui.screens.benchmark

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import com.example.data.entity.BenchmarkResultEntity
import com.example.ui.components.MetricCard
import com.example.ui.theme.AmberThermal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenOptimal
import com.example.ui.theme.PurpleNeural
import com.example.ui.theme.RedSevere
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate850
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BenchmarkScreen(
    viewModel: BenchmarkViewModel,
    modifier: Modifier = Modifier
) {
    val models by viewModel.allModels.collectAsStateWithLifecycle()
    val selectedModelId by viewModel.selectedModelId.collectAsStateWithLifecycle()
    val history by viewModel.benchmarkHistory.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val progress by viewModel.currentProgress.collectAsStateWithLifecycle()
    val evalContext by viewModel.evalContextLength.collectAsStateWithLifecycle()
    val bandwidthGbps by viewModel.memoryBandwidthGbps.collectAsStateWithLifecycle()

    var modelDropdownExpanded by remember { mutableStateOf(false) }

    val currentModel = models.find { it.id == selectedModelId } ?: models.firstOrNull()

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
                                imageVector = Icons.Default.Assessment,
                                contentDescription = null,
                                tint = CyanNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Diagnostics & Benchmarks",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                    }
                },
                actions = {
                    if (history.isNotEmpty()) {
                        IconButton(
                            onClick = { viewModel.clearHistory() },
                            modifier = Modifier.testTag("clear_benchmarks_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear History",
                                tint = Slate400
                            )
                        }
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
            // Model Selector Card
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
                            Text(
                                text = "TARGET MODEL UNDER TEST",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = CyanNeon,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            )

                            Box {
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Slate800)
                                        .clickable { modelDropdownExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = currentModel?.name ?: "Select Model",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = Slate400
                                    )
                                }

                                DropdownMenu(
                                    expanded = modelDropdownExpanded,
                                    onDismissRequest = { modelDropdownExpanded = false },
                                    modifier = Modifier.background(Slate900)
                                ) {
                                    models.forEach { m ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = "${m.name} (${m.quantType})",
                                                    color = if (m.id == currentModel?.id) CyanNeon else Color.White
                                                )
                                            },
                                            onClick = {
                                                viewModel.selectModel(m.id)
                                                modelDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Benchmark Run Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.runFullBenchmark() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("run_full_benchmark_btn"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanNeon,
                                    contentColor = Slate950
                                ),
                                enabled = !isRunning && currentModel != null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Run Full Sweep", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { viewModel.runThermalStressTest() },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("run_thermal_stress_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = AmberThermal),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AmberThermal),
                                enabled = !isRunning && currentModel != null,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Thermostat, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thermal Stress", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Live Progress & Telemetry Card
            if (isRunning || progress != null) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .border(1.dp, if (isRunning) CyanNeon else GreenOptimal, RoundedCornerShape(16.dp)),
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
                                    if (isRunning) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            color = CyanNeon,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = progress?.currentStage ?: "Benchmark in Progress",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    )
                                }

                                if (isRunning) {
                                    IconButton(
                                        onClick = { viewModel.cancelBenchmark() },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = "Cancel", tint = RedSevere)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            LinearProgressIndicator(
                                progress = { progress?.progressRatio ?: 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = CyanNeon,
                                trackColor = Slate800
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = progress?.intermediateLog ?: "Measuring performance metrics...",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = Slate400,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }
            }

            // Interactive Roofline Model Analyzer
            currentModel?.let { model ->
                val roofline = viewModel.getRooflineEstimate(model)
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
                                    Icon(Icons.Default.ShowChart, contentDescription = null, tint = CyanNeon, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BANDWIDTH ROOFLINE ANALYZER",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = CyanNeon,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    )
                                }

                                Text(
                                    text = "~${String.format(java.util.Locale.US, "%.1f", roofline.estimatedTokensPerSec)} tok/s",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = GreenOptimal,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = roofline.formulaExplanation,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = Slate400,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Interactive Context Length Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Evaluation Context Window:", style = MaterialTheme.typography.bodySmall.copy(color = Slate400))
                                Text("$evalContext tokens", style = MaterialTheme.typography.bodySmall.copy(color = PurpleNeural, fontWeight = FontWeight.Bold))
                            }

                            Slider(
                                value = evalContext.toFloat(),
                                onValueChange = { viewModel.setEvalContext(it.toInt()) },
                                valueRange = 512f..8192f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = CyanNeon,
                                    activeTrackColor = CyanNeon,
                                    inactiveTrackColor = Slate800
                                )
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Visual Roofline Canvas Chart
                            com.example.ui.components.RooflineChart(
                                bandwidthGbps = bandwidthGbps,
                                estimatedTokPerSec = roofline.estimatedTokensPerSec
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // Visual Thread Scaling & Contention Chart
                            com.example.ui.components.ThreadScalingBarChart(
                                basePeakTokPerSec = roofline.estimatedTokensPerSec
                            )
                        }
                    }
                }
            }

            // Benchmark History Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BENCHMARK HISTORY (${history.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = Slate400,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                }
            }

            // Benchmark History Items
            items(history, key = { it.id }) { item ->
                BenchmarkHistoryCard(item)
            }
        }
    }
}

@Composable
private fun BenchmarkHistoryCard(result: BenchmarkResultEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .border(1.dp, CardBorder, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Slate900)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = result.modelName,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Text(
                        text = "${result.testType} • ${result.backendUsed} (${result.threadCount} threads)",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400)
                    )
                }

                // STI Score Badge (§16.1)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanNeon.copy(alpha = 0.15f))
                        .border(1.dp, CyanNeon.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STI: ",
                        style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                    )
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", result.stiScore),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = CyanNeon,
                            fontFamily = FontFamily.Monospace
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4-Column Stat Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatPill("PREFILL", "${String.format(java.util.Locale.US, "%.0f", result.prefillTokPerSec)} t/s", PurpleNeural)
                StatPill("PEAK DECODE", "${String.format(java.util.Locale.US, "%.1f", result.peakTokPerSec)} t/s", GreenOptimal)
                StatPill("SUSTAINED", "${String.format(java.util.Locale.US, "%.1f", result.sustainedTokPerSec)} t/s", CyanAccent)
                StatPill("BANDWIDTH", "${String.format(java.util.Locale.US, "%.0f", result.memoryBandwidthGbps)} GB/s", AmberThermal)
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, color = Slate400)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                fontFamily = FontFamily.Monospace
            )
        )
    }
}
