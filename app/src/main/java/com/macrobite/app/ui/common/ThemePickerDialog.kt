package com.macrobite.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrobite.app.R
import com.macrobite.app.ui.theme.AppThemePreset

// Vibrant Material Color Palette for 1-tap custom picking
private val customColorPalette = listOf(
    Color(0xFFF59E0B), // Amber
    Color(0xFFFF5722), // Deep Orange
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF3F51B5), // Indigo
    Color(0xFF2196F3), // Blue
    Color(0xFF03A9F4), // Light Blue
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF4CAF50), // Green
    Color(0xFF8BC34A), // Light Green
    Color(0xFFCDDC39), // Lime
    Color(0xFFFFEB3B), // Yellow
    Color(0xFFFFC107), // Golden Amber
    Color(0xFF795548), // Brown
    Color(0xFF607D8B), // Blue Grey
    Color(0xFFE11D48)  // Crimson
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ThemePickerDialog(
    currentThemeId: String,
    onSelectTheme: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember {
        mutableIntStateOf(if (currentThemeId.startsWith("#")) 1 else 0)
    }

    val resolvedCurrent = remember(currentThemeId) {
        AppThemePreset.resolveTheme(currentThemeId)
    }

    // Custom RGB sliders state
    var redValue by remember { mutableFloatStateOf(resolvedCurrent.primaryColor.red) }
    var greenValue by remember { mutableFloatStateOf(resolvedCurrent.primaryColor.green) }
    var blueValue by remember { mutableFloatStateOf(resolvedCurrent.primaryColor.blue) }

    val currentColorPreview = remember(redValue, greenValue, blueValue) {
        Color(red = redValue, green = greenValue, blue = blueValue)
    }

    val currentHexCode = remember(redValue, greenValue, blueValue) {
        String.format(
            "#%02X%02X%02X",
            (redValue * 255).toInt().coerceIn(0, 255),
            (greenValue * 255).toInt().coerceIn(0, 255),
            (blueValue * 255).toInt().coerceIn(0, 255)
        )
    }

    var manualHexInput by remember(currentHexCode) { mutableStateOf(currentHexCode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_palette),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "App Theme Color",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Presets", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Custom Color", fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    )
                }

                if (selectedTab == 0) {
                    // Presets Tab
                    Text(
                        text = "Choose a preset accent color:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))

                    AppThemePreset.entries.forEach { preset ->
                        val isSelected = preset.id.equals(currentThemeId, ignoreCase = true)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectTheme(preset.id)
                                    onDismiss()
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) preset.primaryColor.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 0.8.dp,
                                color = if (isSelected) preset.primaryColor else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(preset.primaryColor)
                                            .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = preset.title + if (preset.isDefault) " (Default)" else "",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 14.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = preset.primaryColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Custom Color Picker Tab
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Customize your own unique accent color:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Live Color Preview & Hex Display Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(1.dp, currentColorPreview.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(currentColorPreview)
                                        .border(2.dp, Color.White, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Selected Color",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = currentHexCode,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    onSelectTheme(currentHexCode)
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = currentColorPreview,
                                    contentColor = if ((0.299 * currentColorPreview.red + 0.587 * currentColorPreview.green + 0.114 * currentColorPreview.blue) > 0.55) Color.Black else Color.White
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Apply", fontWeight = FontWeight.Bold)
                            }
                        }

                        // 1-Tap Material Color Swatches Palette
                        Text(
                            text = "Quick Palette:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            customColorPalette.forEach { swatchColor ->
                                val isSwatchSelected = currentHexCode.equals(
                                    String.format("#%02X%02X%02X", (swatchColor.red * 255).toInt(), (swatchColor.green * 255).toInt(), (swatchColor.blue * 255).toInt()),
                                    ignoreCase = true
                                )
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(swatchColor)
                                        .border(
                                            width = if (isSwatchSelected) 2.5.dp else 1.dp,
                                            color = if (isSwatchSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            redValue = swatchColor.red
                                            greenValue = swatchColor.green
                                            blueValue = swatchColor.blue
                                            manualHexInput = String.format("#%02X%02X%02X", (swatchColor.red * 255).toInt(), (swatchColor.green * 255).toInt(), (swatchColor.blue * 255).toInt())
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSwatchSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Fine-tuning RGB Sliders
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Red Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Red", fontSize = 12.sp, color = Color(0xFFEF5350), fontWeight = FontWeight.Bold)
                                Text("${(redValue * 255).toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = redValue,
                                onValueChange = {
                                    redValue = it
                                    manualHexInput = currentHexCode
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEF5350),
                                    activeTrackColor = Color(0xFFEF5350)
                                )
                            )

                            // Green Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Green", fontSize = 12.sp, color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold)
                                Text("${(greenValue * 255).toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = greenValue,
                                onValueChange = {
                                    greenValue = it
                                    manualHexInput = currentHexCode
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF66BB6A),
                                    activeTrackColor = Color(0xFF66BB6A)
                                )
                            )

                            // Blue Slider
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Blue", fontSize = 12.sp, color = Color(0xFF42A5F5), fontWeight = FontWeight.Bold)
                                Text("${(blueValue * 255).toInt()}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = blueValue,
                                onValueChange = {
                                    blueValue = it
                                    manualHexInput = currentHexCode
                                },
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF42A5F5),
                                    activeTrackColor = Color(0xFF42A5F5)
                                )
                            )
                        }

                        // Manual Hex Input
                        OutlinedTextField(
                            value = manualHexInput,
                            onValueChange = { input ->
                                manualHexInput = input
                                try {
                                    val clean = input.trim().removePrefix("#")
                                    if (clean.length == 6) {
                                        val parsed = android.graphics.Color.parseColor("#$clean")
                                        val c = Color(parsed)
                                        redValue = c.red
                                        greenValue = c.green
                                        blueValue = c.blue
                                    }
                                } catch (_: Throwable) {}
                            },
                            label = { Text("Enter HEX Code") },
                            placeholder = { Text("#FF5722") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}
