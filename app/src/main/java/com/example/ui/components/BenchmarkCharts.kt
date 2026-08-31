package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberThermal
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyanNeon
import com.example.ui.theme.GreenOptimal
import com.example.ui.theme.PurpleNeural
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900
import com.example.ui.theme.Slate950

/**
 * Interactive analytical Roofline Model visualizer in Jetpack Compose Canvas.
 * Demonstrates the boundary between memory-bandwidth bound (decode) and compute-bound (prefill).
 */
@Composable
fun RooflineChart(
    bandwidthGbps: Double,
    estimatedTokPerSec: Double,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = Slate950
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ANALYTICAL ROOFLINE CURVE",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanNeon,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Bandwidth Limit: ${String.format(java.util.Locale.US, "%.0f", bandwidthGbps)} GB/s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = Slate400,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Canvas visualization
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(Slate900, RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                val width = size.width
                val height = size.height

                val paddingX = 16f
                val paddingY = 16f
                val plotWidth = width - (paddingX * 2)
                val plotHeight = height - (paddingY * 2)

                val kneeX = paddingX + (plotWidth * 0.45f)
                val peakY = paddingY + (plotHeight * 0.15f)
                val bottomY = paddingY + plotHeight
                val startX = paddingX

                // Draw Grid Lines
                for (i in 1..3) {
                    val y = paddingY + (plotHeight * (i / 4f))
                    drawLine(
                        color = Slate800,
                        start = Offset(paddingX, y),
                        end = Offset(paddingX + plotWidth, y),
                        strokeWidth = 1f
                    )
                }

                // Draw Memory Bandwidth Bound (Slanted) & Peak Compute Bound (Horizontal)
                val rooflinePath = Path().apply {
                    moveTo(startX, bottomY)
                    lineTo(kneeX, peakY)
                    lineTo(paddingX + plotWidth, peakY)
                }

                drawPath(
                    path = rooflinePath,
                    color = CyanNeon,
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )

                // Fill area under roofline
                val fillPath = Path().apply {
                    moveTo(startX, bottomY)
                    lineTo(kneeX, peakY)
                    lineTo(paddingX + plotWidth, peakY)
                    lineTo(paddingX + plotWidth, bottomY)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(CyanNeon.copy(alpha = 0.25f), Color.Transparent),
                        startY = peakY,
                        endY = bottomY
                    )
                )

                // Current Operating Point for Decode (Memory-bound, low arithmetic intensity)
                val decodeDotX = paddingX + (plotWidth * 0.22f)
                val decodeDotY = bottomY - ((bottomY - peakY) * (0.22f / 0.45f))

                drawCircle(
                    color = GreenOptimal,
                    radius = 7f,
                    center = Offset(decodeDotX, decodeDotY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 3f,
                    center = Offset(decodeDotX, decodeDotY)
                )

                // Operating Point for Prefill (Higher arithmetic intensity)
                val prefillDotX = paddingX + (plotWidth * 0.75f)
                val prefillDotY = peakY

                drawCircle(
                    color = PurpleNeural,
                    radius = 6f,
                    center = Offset(prefillDotX, prefillDotY)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GreenOptimal))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Token Decode (Memory Bound)", style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 9.5.sp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(PurpleNeural))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Prefill (Compute Bound)", style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 9.5.sp))
                }
            }
        }
    }
}

/**
 * Thread Scaling & Memory Contention Visualizer.
 * Shows peak vs sustained tokens/sec across 1, 2, 4, 6, 8 threads.
 */
@Composable
fun ThreadScalingBarChart(
    basePeakTokPerSec: Double,
    modifier: Modifier = Modifier
) {
    val threadData = listOf(
        Triple(1, basePeakTokPerSec * 0.65, basePeakTokPerSec * 0.64),
        Triple(2, basePeakTokPerSec * 0.88, basePeakTokPerSec * 0.85),
        Triple(4, basePeakTokPerSec * 1.00, basePeakTokPerSec * 0.94),
        Triple(6, basePeakTokPerSec * 1.03, basePeakTokPerSec * 0.86),
        Triple(8, basePeakTokPerSec * 0.96, basePeakTokPerSec * 0.72)
    )

    val maxVal = (threadData.maxOfOrNull { it.second } ?: 30.0).coerceAtLeast(10.0)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        color = Slate950
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "MULTI-THREAD SCALING & BUS CONTENTION",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = CyanNeon,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "Peak vs Sustained STI",
                    style = MaterialTheme.typography.labelSmall.copy(color = Slate400, fontSize = 10.sp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                threadData.forEach { (threads, peak, sustained) ->
                    val peakRatio = (peak / maxVal).toFloat().coerceIn(0.05f, 1f)
                    val sustainedRatio = (sustained / maxVal).toFloat().coerceIn(0.05f, 1f)
                    val isOptimal = threads == 4

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = String.format(java.util.Locale.US, "%.1f", sustained),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                color = if (isOptimal) GreenOptimal else Slate400,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.height(60.dp)
                        ) {
                            // Peak bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height((60 * peakRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(if (isOptimal) CyanNeon else Slate700)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            // Sustained bar
                            Box(
                                modifier = Modifier
                                    .width(8.dp)
                                    .height((60 * sustainedRatio).dp)
                                    .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                    .background(if (isOptimal) GreenOptimal else AmberThermal.copy(alpha = 0.8f))
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${threads}T" + if (isOptimal) "★" else "",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isOptimal) FontWeight.Bold else FontWeight.Normal,
                                color = if (isOptimal) CyanNeon else Slate400
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "★ 4 Cores (Gold Cluster) provides optimal memory throughput without thermal throttling",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = GreenOptimal,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                )
            }
        }
    }
}
