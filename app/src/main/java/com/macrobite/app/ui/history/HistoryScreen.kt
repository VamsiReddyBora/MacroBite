package com.macrobite.app.ui.history

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrobite.app.R
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.model.formatMacroOneDecimal
import com.macrobite.app.ui.common.ModernFloatingSnackbarHost
import com.macrobite.app.ui.dashboard.MealDetailDialog
import com.macrobite.app.ui.dashboard.MealItemCard
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.ProteinBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    isPageActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedMealForDetail by remember { mutableStateOf<MealEntry?>(null) }

    // Reset back to Today whenever user leaves History tab
    LaunchedEffect(isPageActive) {
        if (!isPageActive) {
            viewModel.selectDate(AppDate.today())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.selectDate(AppDate.today())
        }
    }

    LaunchedEffect(uiState.exportShareIntent) {
        uiState.exportShareIntent?.let { intent ->
            context.startActivity(Intent.createChooser(intent, "Export MacroBite CSV"))
            viewModel.clearExportIntent()
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "History & Trends",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        IconButton(
                            onClick = { viewModel.exportToCsv() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_file_download),
                                contentDescription = "Export CSV",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { ModernFloatingSnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Weekly Bar Chart
            item {
                WeeklyBarChart(
                    daysData = uiState.weekCalorieData,
                    selectedDate = uiState.selectedDate,
                    onSelectDay = { viewModel.selectDate(it) }
                )
            }

            // Selected Day Summary Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = uiState.selectedDate.toDisplayTitle(),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val calProg = if (uiState.targetCalories > 0) {
                                (uiState.totalCaloriesForSelectedDate.toFloat() / uiState.targetCalories).coerceIn(0f, 1f)
                            } else 0f
                            val percent = (calProg * 100).toInt()

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = "$percent% of target",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Calorie comparison
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CALORIES LOGGED",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "%,d / %,d kcal".format(
                                    uiState.totalCaloriesForSelectedDate,
                                    uiState.targetCalories
                                ),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        val calProg = if (uiState.targetCalories > 0) {
                            (uiState.totalCaloriesForSelectedDate.toFloat() / uiState.targetCalories).coerceIn(0f, 1f)
                        } else 0f
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { calProg },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3 Macro Capsules Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MacroStatPill(
                                emoji = "🥩",
                                label = "PROTEIN",
                                value = "${uiState.totalProteinForSelectedDate.formatMacroOneDecimal()}g",
                                target = "${uiState.targetProtein}g",
                                color = ProteinBlue,
                                modifier = Modifier.weight(1f)
                            )
                            MacroStatPill(
                                emoji = "🌾",
                                label = "CARBS",
                                value = "${uiState.totalCarbsForSelectedDate.formatMacroOneDecimal()}g",
                                target = "${uiState.targetCarbs}g",
                                color = CarbsGreen,
                                modifier = Modifier.weight(1f)
                            )
                            MacroStatPill(
                                emoji = "🥑",
                                label = "FATS",
                                value = "${uiState.totalFatsForSelectedDate.formatMacroOneDecimal()}g",
                                target = "${uiState.targetFats}g",
                                color = FatsCoral,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Section Header
            item {
                val dayTitle = if (uiState.selectedDate == AppDate.today()) "Today" else uiState.selectedDate.toDisplayTitle()
                Text(
                    text = "Meals Logged on $dayTitle",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Meals List
            if (uiState.mealsForSelectedDate.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nav_history),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No meals recorded for this date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(
                    count = uiState.mealsForSelectedDate.size,
                    key = { index -> "${uiState.mealsForSelectedDate[index].id}_$index" }
                ) { index ->
                    val meal = uiState.mealsForSelectedDate[index]
                    MealItemCard(
                        meal = meal,
                        onDelete = { viewModel.deleteMeal(meal) },
                        onClick = { selectedMealForDetail = meal }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Full view meal detail dialog when tapping a meal card in history
    selectedMealForDetail?.let { meal ->
        MealDetailDialog(
            meal = meal,
            onDismiss = { selectedMealForDetail = null },
            onRegenerate = { updatedPortion, updatedName ->
                viewModel.updateMeal(meal.copy(portion = updatedPortion, foodName = updatedName))
                selectedMealForDetail = null
            },
            onDelete = {
                viewModel.deleteMeal(meal)
                selectedMealForDetail = null
            }
        )
    }
}

@Composable
private fun MacroStatPill(
    emoji: String,
    label: String,
    value: String,
    target: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = emoji,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "of $target",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
