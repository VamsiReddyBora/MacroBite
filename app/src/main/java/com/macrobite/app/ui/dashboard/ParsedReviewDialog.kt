package com.macrobite.app.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.res.painterResource
import com.macrobite.app.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.macrobite.app.data.parser.ParseResult
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.model.MealItem
import com.macrobite.app.domain.model.formatMacroOneDecimal
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.ProteinBlue

@Composable
fun ParsedReviewDialog(
    parseResult: ParseResult,
    targetDate: String,
    photoUri: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (List<MealEntry>) -> Unit,
    onRegenerateItem: (index: Int, name: String, portion: String) -> Unit = { _, _, _ -> },
    onRegenerateAll: (List<MealItem>) -> Unit = {}
) {
    var selectedCategory by remember { mutableStateOf(parseResult.category) }
    val editableItems = remember {
        mutableStateListOf<MealItem>().apply { addAll(parseResult.items) }
    }

    LaunchedEffect(parseResult) {
        editableItems.clear()
        editableItems.addAll(parseResult.items)
        selectedCategory = parseResult.category
    }

    val totalCal = editableItems.sumOf { it.calories }
    val totalProt = editableItems.fold(0f) { acc, item -> acc + item.protein }
    val totalCarbs = editableItems.fold(0f) { acc, item -> acc + item.carbs }
    val totalFats = editableItems.fold(0f) { acc, item -> acc + item.fats }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 16.dp),
        title = {
            Text(
                text = "Review Logged Meal",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Selector
                Text(
                    text = "Category:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    MealCategory.entries.forEach { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Standard calibration banner
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_tune),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Standard cup = 250ml. If your bowl/cup size differs, tap quick ml chips or adjust numbers below for maximum accuracy.",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Summary Totals
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("$totalCal kcal", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        Text("${totalProt.formatMacroOneDecimal()}g P", color = ProteinBlue, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        Text("${totalCarbs.formatMacroOneDecimal()}g C", color = CarbsGreen, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                        Text("${totalFats.formatMacroOneDecimal()}g F", color = FatsCoral, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Header with Re-generate All option
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Meal Items (${editableItems.size})",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(
                        onClick = { onRegenerateAll(editableItems.toList()) }
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_refresh),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Re-generate All",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Editable items list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(editableItems) { index, item ->
                        EditableMealItemRow(
                            item = item,
                            onUpdate = { updated -> editableItems[index] = updated },
                            onDelete = {
                                if (editableItems.size > 1) {
                                    editableItems.removeAt(index)
                                }
                            },
                            onRegenerate = {
                                onRegenerateItem(index, item.name, item.portion)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val entries = editableItems.map { item ->
                        MealEntry(
                            date = targetDate,
                            category = selectedCategory,
                            foodName = item.name,
                            portion = item.portion,
                            calories = item.calories,
                            protein = item.protein,
                            carbs = item.carbs,
                            fats = item.fats,
                            fiber = item.fiber,
                            sugar = item.sugar,
                            sodium = item.sodium,
                            saturatedFat = item.saturatedFat,
                            potassium = item.potassium,
                            cholesterol = item.cholesterol,
                            vitaminsAndMinerals = item.vitaminsAndMinerals,
                            photoUri = photoUri
                        )
                    }
                    onConfirm(entries)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Confirm & Log", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}

@Composable
private fun EditableMealItemRow(
    item: MealItem,
    onUpdate: (MealItem) -> Unit,
    onDelete: () -> Unit,
    onRegenerate: () -> Unit
) {
    val detectedMl = remember(item.portion) {
        Regex("(\\d+)\\s*ml").find(item.portion)?.groupValues?.get(1)?.toIntOrNull()
    }
    val hasVolume = item.baseMl != null || detectedMl != null
    val baseMl = item.baseMl ?: detectedMl ?: 250
    val currentMl = detectedMl ?: item.baseMl ?: 250
    val baseCal = item.baseCalories ?: if (currentMl > 0) ((item.calories.toFloat() / currentMl) * baseMl).roundToInt() else item.calories
    val baseProt = item.baseProtein ?: if (currentMl > 0) ((item.protein / currentMl) * baseMl) else item.protein
    val baseCarb = item.baseCarbs ?: if (currentMl > 0) ((item.carbs / currentMl) * baseMl) else item.carbs
    val baseFat = item.baseFats ?: if (currentMl > 0) ((item.fats / currentMl) * baseMl) else item.fats

    val isAssumed = item.portion.contains("assumed", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { onUpdate(item.copy(name = it)) },
                    label = { Text("Dish / Item Name", fontSize = 10.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )

                // Re-generate button with small reload icon
                FilledTonalButton(
                    onClick = onRegenerate,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(42.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_refresh),
                        contentDescription = "Re-generate",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Re-generate",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Portion display / Assumption badge
            if (isAssumed) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tune),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${item.portion} • Tap your bowl size below:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Quick volume adjustment chips for volume/cup-based items
            if (hasVolume) {
                val standardSizes = listOf(150, 200, 250, 300)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    standardSizes.forEach { ml ->
                        val isSelected = currentMl == ml && !isAssumed
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                val ratio = ml.toFloat() / baseMl.toFloat()
                                val newCal = (baseCal * ratio).roundToInt()
                                val newProt = ((baseProt * ratio) * 10f).roundToInt() / 10f
                                val newCarb = ((baseCarb * ratio) * 10f).roundToInt() / 10f
                                val newFat = ((baseFat * ratio) * 10f).roundToInt() / 10f
                                val newPortion = if (ml == 250) "1 cup (250ml)" else "${ml}ml"
                                onUpdate(
                                    item.copy(
                                        portion = newPortion,
                                        calories = newCal,
                                        protein = newProt,
                                        carbs = newCarb,
                                        fats = newFat,
                                        baseMl = baseMl,
                                        baseCalories = baseCal,
                                        baseProtein = baseProt,
                                        baseCarbs = baseCarb,
                                        baseFats = baseFat
                                    )
                                )
                            },
                            label = {
                                Text(
                                    text = if (ml == 250) "250ml (Std)" else "${ml}ml",
                                    fontSize = 10.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Portion description text field
            OutlinedTextField(
                value = item.portion,
                onValueChange = { newPortion ->
                    val typedMl = Regex("(\\d+)\\s*ml").find(newPortion)?.groupValues?.get(1)?.toIntOrNull()
                    if (typedMl != null && hasVolume && typedMl > 0) {
                        val ratio = typedMl.toFloat() / baseMl.toFloat()
                        onUpdate(
                            item.copy(
                                portion = newPortion,
                                calories = (baseCal * ratio).roundToInt(),
                                protein = ((baseProt * ratio) * 10f).roundToInt() / 10f,
                                carbs = ((baseCarb * ratio) * 10f).roundToInt() / 10f,
                                fats = ((baseFat * ratio) * 10f).roundToInt() / 10f
                            )
                        )
                    } else {
                        onUpdate(item.copy(portion = newPortion))
                    }
                },
                label = { Text("Grams / Portion size (e.g. 200g, 1 bowl, 2 rotis)", fontSize = 10.sp) },
                placeholder = { Text("e.g. 200g or 2 rotis", fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextField(
                    value = if (item.calories == 0) "" else item.calories.toString(),
                    onValueChange = { onUpdate(item.copy(calories = it.toIntOrNull() ?: 0)) },
                    label = { Text("kcal", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = if (item.protein == 0f) "" else item.protein.formatMacroOneDecimal(),
                    onValueChange = { onUpdate(item.copy(protein = it.toFloatOrNull() ?: 0f)) },
                    label = { Text("P (g)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = if (item.carbs == 0f) "" else item.carbs.formatMacroOneDecimal(),
                    onValueChange = { onUpdate(item.copy(carbs = it.toFloatOrNull() ?: 0f)) },
                    label = { Text("C (g)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = if (item.fats == 0f) "" else item.fats.formatMacroOneDecimal(),
                    onValueChange = { onUpdate(item.copy(fats = it.toFloatOrNull() ?: 0f)) },
                    label = { Text("F (g)", fontSize = 10.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }
}
