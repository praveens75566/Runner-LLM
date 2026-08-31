package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.thermal.ThermalControlState
import com.example.ui.theme.AmberThermal
import com.example.ui.theme.GreenOptimal
import com.example.ui.theme.RedSevere
import com.example.ui.theme.Slate800
import com.example.ui.theme.Slate900

@Composable
fun ThermalBadge(
    thermalState: ThermalControlState,
    modifier: Modifier = Modifier
) {
    val (statusColor, statusLabel) = when {
        thermalState.currentTempC >= 43.0f || thermalState.thermalStatus >= 3 ->
            RedSevere to "SEVERE"
        thermalState.currentTempC >= 40.0f || thermalState.thermalStatus >= 2 ->
            AmberThermal to "MODERATE"
        thermalState.currentTempC >= 37.0f || thermalState.thermalStatus >= 1 ->
            Color(0xFFFFD166) to "LIGHT"
        else ->
            GreenOptimal to "NOMINAL"
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Slate800)
            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Thermostat,
            contentDescription = "Thermal Sensor",
            tint = statusColor,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "${String.format(java.util.Locale.US, "%.1f", thermalState.currentTempC)}°C",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                color = statusColor,
                fontWeight = FontWeight.Bold
            )
        )
    }
}
