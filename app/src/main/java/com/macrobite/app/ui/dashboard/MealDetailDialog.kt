package com.macrobite.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.macrobite.app.R
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.model.formatMacroOneDecimal
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.CarbsGreenSubtle
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.FatsCoralSubtle
import com.macrobite.app.ui.theme.ProteinBlue
import com.macrobite.app.ui.theme.ProteinBlueSubtle

@Composable
fun MealDetailDialog(
    meal: MealEntry,
    onDismiss: () -> Unit,
    onRegenerate: (updatedPortion: String, updatedName: String) -> Unit,
    onDelete: () -> Unit
) {
    var editedName by remember(meal.id, meal.foodName) { mutableStateOf(meal.foodName) }
    var editedPortion by remember(meal.id, meal.portion) { mutableStateOf(meal.portion) }

    LaunchedEffect(meal.id, meal.foodName, meal.portion) {
        editedName = meal.foodName
        editedPortion = meal.portion
    }

    val categoryEmoji = when (meal.category) {
        MealCategory.BREAKFAST -> "🍳"
        MealCategory.LUNCH -> "🍛"
        MealCategory.SNACKS -> "☕"
        MealCategory.DINNER -> "🍲"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 20.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$categoryEmoji ${meal.category.displayName}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Food Details",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Photo (if available)
                if (!meal.photoUri.isNullOrBlank()) {
                    AsyncImage(
                        model = meal.photoUri,
                        contentDescription = meal.foodName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }

                // Food Name Input
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Food Item Name", fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                // Portion & Quantity Input
                OutlinedTextField(
                    value = editedPortion,
                    onValueChange = { editedPortion = it },
                    label = { Text("Portion / Quantity (e.g. 20g packet, 1 cup, 70g)", fontSize = 11.sp) },
                    placeholder = { Text("e.g. 20g packet, 2 eggs, 250ml", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    singleLine = true
                )

                // Instruction helper hint
                Text(
                    text = "💡 Enter the exact weight (e.g. 20g packet) and tap 'Regenerate Nutrition' below to recalculate accurately via Gemini.",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Primary Macro Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MacroCardItem(
                        label = "Calories",
                        value = "${meal.calories}",
                        unit = "kcal",
                        color = MaterialTheme.colorScheme.primary,
                        bgColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.weight(1f)
                    )
                    MacroCardItem(
                        label = "Protein",
                        value = meal.protein.formatMacroOneDecimal(),
                        unit = "g",
                        color = ProteinBlue,
                        bgColor = ProteinBlueSubtle,
                        modifier = Modifier.weight(1f)
                    )
                    MacroCardItem(
                        label = "Carbs",
                        value = meal.carbs.formatMacroOneDecimal(),
                        unit = "g",
                        color = CarbsGreen,
                        bgColor = CarbsGreenSubtle,
                        modifier = Modifier.weight(1f)
                    )
                    MacroCardItem(
                        label = "Fats",
                        value = meal.fats.formatMacroOneDecimal(),
                        unit = "g",
                        color = FatsCoral,
                        bgColor = FatsCoralSubtle,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Extended Micronutrients & Breakdown Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Detailed Nutritional Breakdown",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.8.dp
                        )

                        // 2-column grid of extended macros
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutrientTextRow(label = "Dietary Fiber", value = "${meal.fiber.formatMacroOneDecimal()} g")
                            NutrientTextRow(label = "Total Sugars", value = "${meal.sugar.formatMacroOneDecimal()} g")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutrientTextRow(label = "Sodium", value = "${meal.sodium.formatMacroOneDecimal()} mg")
                            NutrientTextRow(label = "Saturated Fat", value = "${meal.saturatedFat.formatMacroOneDecimal()} g")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            NutrientTextRow(label = "Potassium", value = "${meal.potassium.formatMacroOneDecimal()} mg")
                            NutrientTextRow(label = "Cholesterol", value = "${meal.cholesterol.formatMacroOneDecimal()} mg")
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 0.8.dp
                        )

                        // Vitamins & Minerals
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Vitamins & Minerals:",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val vitaminsText = meal.vitaminsAndMinerals.ifBlank {
                                "None listed yet. Tap 'Regenerate Nutrition' to fetch comprehensive vitamin & mineral data from Gemini."
                            }
                            Text(
                                text = vitaminsText,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                color = if (meal.vitaminsAndMinerals.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Regenerate with Gemini Button
                Button(
                    onClick = {
                        onRegenerate(editedPortion, editedName)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Regenerate Nutrition",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text("Done", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete Log", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun MacroCardItem(
    label: String,
    value: String,
    unit: String,
    color: Color,
    bgColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = color.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                ),
                color = color
            )
            Text(
                text = unit,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = color.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun NutrientTextRow(
    label: String,
    value: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
