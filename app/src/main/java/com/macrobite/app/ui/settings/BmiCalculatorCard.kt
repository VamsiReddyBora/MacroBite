package com.macrobite.app.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrobite.app.R
import com.macrobite.app.ui.theme.CalorieAmber
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.ProteinBlue
import java.util.Locale
import kotlin.math.roundToInt

enum class HeightUnit(val label: String) {
    CM("cm"),
    INCH("inches"),
    FT_IN("ft + in")
}

enum class WeightUnit(val label: String) {
    KG("kg"),
    LBS("lbs")
}

@Composable
fun BmiCalculatorCard(
    initialWeightKg: Float? = null,
    isScreenActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Height states - start empty so calculations are blank until user enters
    var selectedHeightUnit by remember { mutableStateOf(HeightUnit.CM) }
    var heightCmInput by remember { mutableStateOf("") }
    var heightInchInput by remember { mutableStateOf("") }
    var heightFtInput by remember { mutableStateOf("") }
    var heightInPartInput by remember { mutableStateOf("") }

    // Weight states - start empty
    var selectedWeightUnit by remember { mutableStateOf(WeightUnit.KG) }
    var weightInput by remember { mutableStateOf("") }

    // When navigating away from settings, immediately collapse and clear all data
    LaunchedEffect(isScreenActive) {
        if (!isScreenActive) {
            isExpanded = false
            heightCmInput = ""
            heightInchInput = ""
            heightFtInput = ""
            heightInPartInput = ""
            weightInput = ""
        }
    }

    // Convert height to meters
    val heightInMeters: Double? = when (selectedHeightUnit) {
        HeightUnit.CM -> {
            val cm = heightCmInput.toDoubleOrNull()
            if (cm != null && cm > 0) cm / 100.0 else null
        }
        HeightUnit.INCH -> {
            val inches = heightInchInput.toDoubleOrNull()
            if (inches != null && inches > 0) inches * 0.0254 else null
        }
        HeightUnit.FT_IN -> {
            val feet = heightFtInput.toDoubleOrNull() ?: 0.0
            val inches = heightInPartInput.toDoubleOrNull() ?: 0.0
            val totalInches = feet * 12.0 + inches
            if (totalInches > 0) totalInches * 0.0254 else null
        }
    }

    // Convert weight to kg
    val weightInKg: Double? = when (selectedWeightUnit) {
        WeightUnit.KG -> {
            val kg = weightInput.toDoubleOrNull()
            if (kg != null && kg > 0) kg else null
        }
        WeightUnit.LBS -> {
            val lbs = weightInput.toDoubleOrNull()
            if (lbs != null && lbs > 0) lbs * 0.45359237 else null
        }
    }

    // Calculate BMI
    val bmiValue: Double? = if (heightInMeters != null && weightInKg != null && heightInMeters > 0.4) {
        weightInKg / (heightInMeters * heightInMeters)
    } else {
        null
    }

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "bmi_arrow_rotation"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Clickable Dropdown Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_monitor_weight),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "BMI Calculator",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (bmiValue != null) {
                                val category = getBmiCategory(bmiValue)
                                "BMI: ${String.format(Locale.US, "%.1f", bmiValue)} • ${category.label}"
                            } else {
                                "Calculate Body Mass Index & ideal weight"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = if (bmiValue != null) getBmiCategory(bmiValue).color else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_keyboard_arrow_down),
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(arrowRotation)
                    )
                }
            }

            // Dropdown Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                        .padding(bottom = 18.dp)
                ) {
                    // Height Section
                    Text(
                        text = "Height:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Height Unit Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        HeightUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = selectedHeightUnit == unit,
                                onClick = {
                                    if (selectedHeightUnit != unit) {
                                        // Auto convert inputs between units for convenience
                                        heightInMeters?.let { meters ->
                                            when (unit) {
                                                HeightUnit.CM -> heightCmInput = (meters * 100).roundToInt().toString()
                                                HeightUnit.INCH -> heightInchInput = String.format(Locale.US, "%.1f", meters / 0.0254)
                                                HeightUnit.FT_IN -> {
                                                    val totalInches = (meters / 0.0254).roundToInt()
                                                    heightFtInput = (totalInches / 12).toString()
                                                    heightInPartInput = (totalInches % 12).toString()
                                                }
                                            }
                                        }
                                        selectedHeightUnit = unit
                                    }
                                },
                                label = { Text(unit.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Height Input Fields based on unit
                    when (selectedHeightUnit) {
                        HeightUnit.CM -> {
                            OutlinedTextField(
                                value = heightCmInput,
                                onValueChange = { heightCmInput = it },
                                label = { Text("Height (cm)") },
                                placeholder = { Text("e.g. 175") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        HeightUnit.INCH -> {
                            OutlinedTextField(
                                value = heightInchInput,
                                onValueChange = { heightInchInput = it },
                                label = { Text("Height (inches)") },
                                placeholder = { Text("e.g. 69") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        HeightUnit.FT_IN -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = heightFtInput,
                                    onValueChange = { heightFtInput = it },
                                    label = { Text("Feet (ft)") },
                                    placeholder = { Text("5") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = heightInPartInput,
                                    onValueChange = { heightInPartInput = it },
                                    label = { Text("Inches (in)") },
                                    placeholder = { Text("9") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Weight Section
                    Text(
                        text = "Weight:",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Weight Unit Selector Chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WeightUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = selectedWeightUnit == unit,
                                onClick = {
                                    if (selectedWeightUnit != unit) {
                                        weightInKg?.let { kg ->
                                            weightInput = if (unit == WeightUnit.LBS) {
                                                String.format(Locale.US, "%.1f", kg * 2.20462)
                                            } else {
                                                String.format(Locale.US, "%.1f", kg)
                                            }
                                        }
                                        selectedWeightUnit = unit
                                    }
                                },
                                label = { Text(unit.label, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val weightPlaceholder = when {
                        initialWeightKg != null && initialWeightKg > 0 -> {
                            if (selectedWeightUnit == WeightUnit.KG) {
                                "e.g. ${String.format(Locale.US, "%.1f", initialWeightKg)}"
                            } else {
                                "e.g. ${String.format(Locale.US, "%.1f", initialWeightKg * 2.20462)}"
                            }
                        }
                        selectedWeightUnit == WeightUnit.KG -> "e.g. 70"
                        else -> "e.g. 154"
                    }

                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Weight (${selectedWeightUnit.label})") },
                        placeholder = { Text(weightPlaceholder) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // BMI Calculation Result Card
                    if (bmiValue != null && bmiValue in 10.0..65.0 && heightInMeters != null) {
                        val category = getBmiCategory(bmiValue)
                        val minHealthyKg = 18.5 * heightInMeters * heightInMeters
                        val maxHealthyKg = 24.9 * heightInMeters * heightInMeters

                        val healthyRangeText = if (selectedWeightUnit == WeightUnit.LBS) {
                            val minLbs = (minHealthyKg * 2.20462).roundToInt()
                            val maxLbs = (maxHealthyKg * 2.20462).roundToInt()
                            "$minLbs – $maxLbs lbs"
                        } else {
                            val minKg = String.format(Locale.US, "%.1f", minHealthyKg)
                            val maxKg = String.format(Locale.US, "%.1f", maxHealthyKg)
                            "$minKg – $maxKg kg"
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, category.color.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "YOUR BMI SCORE",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 0.8.sp,
                                                fontSize = 10.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = String.format(Locale.US, "%.1f", bmiValue),
                                            style = MaterialTheme.typography.headlineMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = category.color
                                        )
                                    }

                                    // Category Pill Badge
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = category.color.copy(alpha = 0.15f),
                                        border = BorderStroke(1.dp, category.color.copy(alpha = 0.4f))
                                    ) {
                                        Text(
                                            text = category.label,
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            ),
                                            color = category.color,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Colored BMI Gauge Scale Bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                ) {
                                    Box(modifier = Modifier.weight(18.5f).background(ProteinBlue))
                                    Box(modifier = Modifier.weight(6.5f).background(CarbsGreen))
                                    Box(modifier = Modifier.weight(5.0f).background(CalorieAmber))
                                    Box(modifier = Modifier.weight(10.0f).background(FatsCoral))
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Ideal Healthy Weight Range
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Healthy weight for height:",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = healthyRangeText,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Enter valid height and weight above to calculate BMI.",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class BmiCategory(
    val label: String,
    val color: Color
)

private fun getBmiCategory(bmi: Double): BmiCategory {
    return when {
        bmi < 18.5 -> BmiCategory("Underweight", ProteinBlue)
        bmi < 25.0 -> BmiCategory("Normal Weight", CarbsGreen)
        bmi < 30.0 -> BmiCategory("Overweight", CalorieAmber)
        else -> BmiCategory("Obese", FatsCoral)
    }
}
