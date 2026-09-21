package com.macrobite.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.PermissionController
import com.macrobite.app.health.GoogleHealthManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.macrobite.app.ui.common.ModernFloatingSnackbarHost
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import com.macrobite.app.ui.common.ThemePickerDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import com.macrobite.app.domain.model.DailyApiUsage
import java.text.NumberFormat
import java.util.Locale
import kotlinx.coroutines.launch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.macrobite.app.R
import com.macrobite.app.ui.theme.CarbsGreen
import com.macrobite.app.ui.theme.FatsCoral
import com.macrobite.app.ui.theme.ProteinBlue

private data class ParsingEngineOption(
    val id: String,
    val title: String,
    val subtitle: String,
    val isCloud: Boolean,
    val isRecommended: Boolean = false,
    val badge: String = "",
    val quotaText: String = ""
)

private val parsingEngineOptions = listOf(
    ParsingEngineOption(
        id = "gemini-3.5-flash-lite",
        title = "Gemini 3.5 Flash-Lite",
        subtitle = "Fastest speed · High 500 RPD quota · 15 RPM",
        isCloud = true,
        isRecommended = true,
        badge = "Recommended",
        quotaText = "500 RPD · 15 RPM"
    ),
    ParsingEngineOption(
        id = "gemini-3.1-flash-lite",
        title = "Gemini 3.1 Flash-Lite",
        subtitle = "Stable & lightweight · 500 RPD quota · 15 RPM",
        isCloud = true,
        isRecommended = true,
        badge = "Recommended",
        quotaText = "500 RPD · 15 RPM"
    ),
    ParsingEngineOption(
        id = "gemini-3.5-flash",
        title = "Gemini 3.5 Flash",
        subtitle = "Standard Flash · Strict 20 RPD cap · 5 RPM",
        isCloud = true,
        isRecommended = false,
        badge = "20 RPD Cap",
        quotaText = "20 RPD · 5 RPM"
    ),
    ParsingEngineOption(
        id = "gemini-3.6-flash",
        title = "Gemini 3.6 Flash",
        subtitle = "Standard Flash · Strict 20 RPD cap · 5 RPM",
        isCloud = true,
        isRecommended = false,
        badge = "20 RPD Cap",
        quotaText = "20 RPD · 5 RPM"
    ),
    ParsingEngineOption(
        id = "offline-local",
        title = "Offline Local Database",
        subtitle = "Zero internet · Built-in food catalog",
        isCloud = false,
        isRecommended = false,
        badge = "Offline",
        quotaText = "Unlimited"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    isScreenActive: Boolean = true,
    modifier: Modifier = Modifier
) {
    val targets by viewModel.targets.collectAsState()
    val useGemini by viewModel.useGemini.collectAsState()
    val geminiApiKey by viewModel.geminiApiKey.collectAsState()
    val geminiModel by viewModel.geminiModel.collectAsState()
    val darkModePref by viewModel.darkModePreference.collectAsState()
    val themeColorPref by viewModel.themeColorPreference.collectAsState()
    val weightLogs by viewModel.weightLogs.collectAsState()
    val isTestingApi by viewModel.isTestingApi.collectAsState()
    val apiTestMessage by viewModel.apiTestMessage.collectAsState()
    val saveMessage by viewModel.saveMessage.collectAsState()
    val dailyApiUsage by viewModel.dailyApiUsage.collectAsState()
    val notificationPrefs by viewModel.notificationPreferences.collectAsState()
    val stepGoal by viewModel.stepGoal.collectAsState()
    val googleHealthSyncEnabled by viewModel.googleHealthSyncEnabled.collectAsState()
    val googleHealthHasPermissions by viewModel.googleHealthHasPermissions.collectAsState()
    val contactAliases by viewModel.contactAliases.collectAsState()
    val autoBackupEnabled by viewModel.autoBackupEnabled.collectAsState()

    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) {
        viewModel.checkGoogleHealthPermissions()
    }

    var isGoalsExpanded by remember { mutableStateOf(false) }
    val goalsArrowRotation by animateFloatAsState(
        targetValue = if (isGoalsExpanded) 180f else 0f,
        label = "goals_arrow_rotation"
    )

    var isEngineCardExpanded by remember { mutableStateOf(false) }
    var engineMenuExpanded by remember { mutableStateOf(false) }

    val engineArrowRotation by animateFloatAsState(
        targetValue = if (isEngineCardExpanded) 180f else 0f,
        label = "engine_arrow_rotation"
    )

    var isThemeExpanded by remember { mutableStateOf(false) }
    val themeArrowRotation by animateFloatAsState(
        targetValue = if (isThemeExpanded) 180f else 0f,
        label = "theme_arrow_rotation"
    )

    var isAboutExpanded by remember { mutableStateOf(false) }
    val aboutArrowRotation by animateFloatAsState(
        targetValue = if (isAboutExpanded) 180f else 0f,
        label = "about_arrow_rotation"
    )

    val backupStatus by viewModel.backupStatus.collectAsState()
    var isBackupExpanded by remember { mutableStateOf(false) }
    val backupArrowRotation by animateFloatAsState(
        targetValue = if (isBackupExpanded) 180f else 0f,
        label = "backup_arrow_rotation"
    )

    var showThemeDialog by remember { mutableStateOf(false) }
    var showCustomModelDialog by remember { mutableStateOf(false) }
    var customModelInput by remember { mutableStateOf("") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val restoreFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val json = context.contentResolver.openInputStream(uri)?.use { stream ->
                    stream.bufferedReader().readText()
                }
                if (!json.isNullOrBlank()) {
                    viewModel.restoreBackup(json)
                } else {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("Selected backup file is empty.")
                    }
                }
            } catch (e: Exception) {
                coroutineScope.launch {
                    snackbarHostState.showSnackbar("Error reading backup: ${e.message}")
                }
            }
        }
    }

    // Immediately collapse all expanded dropdowns when navigating away from settings
    LaunchedEffect(isScreenActive) {
        if (!isScreenActive) {
            isGoalsExpanded = false
            isEngineCardExpanded = false
            isThemeExpanded = false
            isBackupExpanded = false
            isAboutExpanded = false
        }
    }

    val currentEngineId = if (!useGemini) "offline-local" else if (geminiModel == "gemini-3.7-flash" || geminiModel.isBlank()) "gemini-3.5-flash-lite" else geminiModel
    val selectedEngineOption = remember(currentEngineId) {
        parsingEngineOptions.firstOrNull { it.id == currentEngineId } ?: parsingEngineOptions.first()
    }

    // Local target edit states
    var calInput by remember(targets.calories) { mutableStateOf(targets.calories.toString()) }
    var proteinInput by remember(targets.protein) { mutableStateOf(targets.protein.toString()) }
    var carbsInput by remember(targets.carbs) { mutableStateOf(targets.carbs.toString()) }
    var fatsInput by remember(targets.fats) { mutableStateOf(targets.fats.toString()) }

    // API key edit state
    var keyInput by remember(geminiApiKey) { mutableStateOf(geminiApiKey) }
    var showApiKey by remember { mutableStateOf(false) }

    LaunchedEffect(saveMessage) {
        saveMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings & Targets",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
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
            // Target Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                .clickable { isGoalsExpanded = !isGoalsExpanded }
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
                                        painter = painterResource(id = R.drawable.ic_tune),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Daily Calorie & Macro Goals",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${targets.calories} kcal • ${targets.protein}g P • ${targets.carbs}g C • ${targets.fats}g F",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isGoalsExpanded = !isGoalsExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_keyboard_arrow_down),
                                    contentDescription = if (isGoalsExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.rotate(goalsArrowRotation)
                                )
                            }
                        }

                        // Dropdown Content
                        AnimatedVisibility(
                            visible = isGoalsExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .padding(bottom = 18.dp)
                            ) {
                                Text(
                                    text = "Standard bulking surplus configured for weight gain.",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Target Calories Input
                                OutlinedTextField(
                                    value = calInput,
                                    onValueChange = { calInput = it },
                                    label = { Text("Daily Calories (kcal)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Quick Adjust Chips
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "-100" to -100,
                                        "+100" to 100,
                                        "+250" to 250,
                                        "3000 Std" to 0
                                    ).forEach { (label, delta) ->
                                        SuggestionChip(
                                            onClick = {
                                                val cur = calInput.toIntOrNull() ?: 3000
                                                calInput = if (delta == 0) "3000" else (cur + delta).coerceAtLeast(1200).toString()
                                            },
                                            label = {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp)
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = SuggestionChipDefaults.suggestionChipColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                                labelColor = MaterialTheme.colorScheme.onSurface
                                            ),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Protein, Carbs, Fats row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = proteinInput,
                                        onValueChange = { proteinInput = it },
                                        label = { Text("Protein", fontSize = 11.sp, color = ProteinBlue) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = carbsInput,
                                        onValueChange = { carbsInput = it },
                                        label = { Text("Carbs", fontSize = 11.sp, color = CarbsGreen) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    OutlinedTextField(
                                        value = fatsInput,
                                        onValueChange = { fatsInput = it },
                                        label = { Text("Fats", fontSize = 11.sp, color = FatsCoral) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val c = calInput.toIntOrNull() ?: 3000
                                        val p = proteinInput.toIntOrNull() ?: 110
                                        val cb = carbsInput.toIntOrNull() ?: 360
                                        val f = fatsInput.toIntOrNull() ?: 110
                                        viewModel.saveTargets(c, p, cb, f)
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_save), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Save Target Goals", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // BMI Calculator Dropdown Card
            val latestWeight = weightLogs.firstOrNull()?.weightKg
            item {
                BmiCalculatorCard(
                    initialWeightKg = latestWeight,
                    isScreenActive = isScreenActive
                )
            }

            // Body Weight Logger Section
            item {
                WeightLoggerSection(
                    weightLogs = weightLogs,
                    onAddWeight = { viewModel.logWeight(it) },
                    onDeleteWeight = { viewModel.deleteWeight(it) },
                    isScreenActive = isScreenActive
                )
            }

            // Google Health & Footsteps Integration Card
            item {
                GoogleHealthSettingsCard(
                    sdkStatus = viewModel.getGoogleHealthStatus(),
                    hasPermissions = googleHealthHasPermissions,
                    syncEnabled = googleHealthSyncEnabled,
                    stepGoal = stepGoal,
                    onSyncToggle = { viewModel.setGoogleHealthSyncEnabled(it) },
                    onStepGoalChange = { viewModel.setStepGoal(it) },
                    onRequestPermissions = {
                        healthConnectLauncher.launch(GoogleHealthManager.REQUIRED_PERMISSIONS)
                    },
                    onOpenSettings = { viewModel.openHealthConnectSettings() },
                    onOpenPlayStore = { viewModel.openHealthConnectPlayStore() }
                )
            }

            // Contact Aliases (Nicknames for Voice & Direct Calling)
            item {
                ContactAliasesSettingsCard(
                    aliases = contactAliases,
                    onAddAlias = { alias, target -> viewModel.setContactAlias(alias, target) },
                    onRemoveAlias = { alias -> viewModel.removeContactAlias(alias) }
                )
            }

            // AI & Parsing Engine Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                .clickable { isEngineCardExpanded = !isEngineCardExpanded }
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
                                        painter = painterResource(id = if (useGemini) R.drawable.ic_auto_awesome else R.drawable.ic_speed),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "AI Parsing Engine",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            IconButton(
                                onClick = { isEngineCardExpanded = !isEngineCardExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_keyboard_arrow_down),
                                    contentDescription = if (isEngineCardExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.rotate(engineArrowRotation)
                                )
                            }
                        }

                        // Dropdown Content
                        AnimatedVisibility(
                            visible = isEngineCardExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .padding(bottom = 18.dp)
                            ) {
                                val uriHandler = LocalUriHandler.current
                                val clipboardManager = LocalClipboardManager.current
                                val hasKey = keyInput.isNotBlank()

                                // Enable Cloud AI Toggle Row inside the card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Enable Cloud AI Parsing",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = if (useGemini) "Using Google Gemini cloud engine" else "Using offline local database",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = useGemini,
                                        onCheckedChange = { enabled ->
                                            viewModel.setUseGemini(enabled)
                                            if (enabled && (geminiModel == "offline-local" || geminiModel == "gemini-3.7-flash")) {
                                                viewModel.setGeminiModel("gemini-3.5-flash-lite")
                                            }
                                        },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Modern Status Badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (hasKey && useGemini) CarbsGreen.copy(alpha = 0.12f)
                                            else if (!useGemini) MaterialTheme.colorScheme.surfaceVariant
                                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 5.dp)
                                    ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (hasKey && useGemini) CarbsGreen
                                                    else if (!useGemini) MaterialTheme.colorScheme.onSurfaceVariant
                                                    else MaterialTheme.colorScheme.primary
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (!useGemini) "Offline Mode (Local Database) 📦"
                                            else if (hasKey) "${selectedEngineOption.title} Active ⚡"
                                            else "API Key Required for ${selectedEngineOption.title}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp
                                            ),
                                            color = if (hasKey && useGemini) CarbsGreen
                                            else if (!useGemini) MaterialTheme.colorScheme.onSurfaceVariant
                                            else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // AI Parsing Engine Dropdown Menu Selector
                                ExposedDropdownMenuBox(
                                    expanded = engineMenuExpanded,
                                    onExpandedChange = { engineMenuExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedEngineOption.title,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("AI Parsing Engine / Model") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = engineMenuExpanded) },
                                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    ExposedDropdownMenu(
                                        expanded = engineMenuExpanded,
                                        onDismissRequest = { engineMenuExpanded = false }
                                    ) {
                                        parsingEngineOptions.forEach { option ->
                                            val isSelected = option.id == currentEngineId
                                            DropdownMenuItem(
                                                text = {
                                                    Column {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text(
                                                                text = option.title,
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                                fontSize = 14.sp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            if (option.badge.isNotBlank()) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                val (badgeBg, badgeFg) = when {
                                                                    option.isRecommended -> CarbsGreen.copy(alpha = 0.15f) to CarbsGreen
                                                                    option.badge.contains("Cap") -> FatsCoral.copy(alpha = 0.15f) to FatsCoral
                                                                    else -> ProteinBlue.copy(alpha = 0.15f) to ProteinBlue
                                                                }
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(badgeBg)
                                                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = option.badge,
                                                                        fontSize = 9.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = badgeFg
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Text(
                                                            text = option.subtitle,
                                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                },
                                                onClick = {
                                                    if (option.id == "offline-local") {
                                                        viewModel.setUseGemini(false)
                                                    } else {
                                                        viewModel.setUseGemini(true)
                                                        viewModel.setGeminiModel(option.id)
                                                    }
                                                    engineMenuExpanded = false
                                                },
                                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = if (useGemini) {
                                        "Cloud AI Mode (Online): Uses ${selectedEngineOption.title} for natural Indian food parsing and camera photo estimation."
                                    } else {
                                        "Offline Mode: Uses local food database with zero data usage."
                                    },
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // 1-Tap Get Free API Key Button
                                Button(
                                    onClick = {
                                        uriHandler.openUri("https://aistudio.google.com/app/apikey")
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_auto_awesome),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("1-Tap: Get Free Gemini API Key", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Gemini API Key Input
                                OutlinedTextField(
                                    value = keyInput,
                                    onValueChange = { keyInput = it },
                                    label = { Text("Gemini API Key") },
                                    placeholder = { Text("AIzaSy...") },
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp),
                                    visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showApiKey = !showApiKey }) {
                                            Icon(
                                                painter = painterResource(id = if (showApiKey) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                                contentDescription = "Toggle key visibility",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                // Paste button
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.getText()?.text?.let { clipText ->
                                            val trimmed = clipText.trim()
                                            if (trimmed.isNotBlank()) {
                                                keyInput = trimmed
                                                viewModel.saveGeminiApiKey(trimmed)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_save), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Paste from Clipboard & Save", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.saveGeminiApiKey(keyInput)
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(painter = painterResource(id = R.drawable.ic_key), contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save Key", fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            viewModel.testGeminiConnection(keyInput)
                                        },
                                        enabled = !isTestingApi && keyInput.isNotBlank(),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isTestingApi) {
                                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                        } else {
                                            Text("Test Connection")
                                        }
                                    }
                                }

                                apiTestMessage?.let { msg ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = msg,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                                        color = if (msg.contains("successful")) CarbsGreen else MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Gemini API Quota & Daily Token Usage
            item {
                GeminiApiUsageCard(
                    usage = dailyApiUsage,
                    activeModel = geminiModel,
                    onAddExternalRequests = { viewModel.addExternalRequests(it) },
                    onSetExternalRequests = { viewModel.setExternalRequestCount(it) },
                    onResetUsage = { viewModel.resetTodayApiUsage() },
                    isScreenActive = isScreenActive
                )
            }

            // Theme Preferences
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                .clickable { isThemeExpanded = !isThemeExpanded }
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
                                        painter = painterResource(id = R.drawable.ic_palette),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Theme Appearance",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${darkModePref.replaceFirstChar { it.uppercase() }} mode • ${themeColorPref.replaceFirstChar { it.uppercase() }} accent",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isThemeExpanded = !isThemeExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_keyboard_arrow_down),
                                    contentDescription = if (isThemeExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.rotate(themeArrowRotation)
                                )
                            }
                        }

                        // Dropdown Content
                        AnimatedVisibility(
                            visible = isThemeExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .padding(bottom = 18.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "dark" to "Dark (Default)",
                                        "light" to "Light",
                                        "system" to "System"
                                    ).forEach { (mode, label) ->
                                        FilterChip(
                                            selected = darkModePref == mode,
                                            onClick = { viewModel.setDarkMode(mode) },
                                            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 14.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    thickness = 0.8.dp
                                )

                                Text(
                                    text = "Accent Color Preset",
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Grid of 6 decent preset colors
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    com.macrobite.app.ui.theme.AppThemePreset.entries.forEach { preset ->
                                        val isSelected = preset.id.equals(themeColorPref, ignoreCase = true)
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) preset.primaryColor.copy(alpha = 0.16f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 0.8.dp,
                                                    color = if (isSelected) preset.primaryColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
                                                    shape = RoundedCornerShape(10.dp)
                                                )
                                                .clickable { viewModel.setThemeColor(preset.id) }
                                                .padding(vertical = 8.dp, horizontal = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(CircleShape)
                                                        .background(preset.primaryColor)
                                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = preset.title.split(" ").first(),
                                                    fontSize = 9.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    color = if (isSelected) preset.primaryColor else MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Button to open full interactive color picker
                                OutlinedButton(
                                    onClick = { showThemeDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_palette),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (themeColorPref.startsWith("#")) "Change Custom Color ($themeColorPref)" else "Choose Custom Accent Color...",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Notifications & Meal Reminders Section
            item {
                NotificationSettingsCard(
                    prefs = notificationPrefs,
                    onToggleMaster = { viewModel.setNotificationsEnabled(it) },
                    onToggleMeal = { type, enabled -> viewModel.setMealReminderEnabled(type, enabled) },
                    onSetMealTime = { type, time -> viewModel.setMealReminderTime(type, time) },
                    onToggleInAppNotifications = { viewModel.setInAppNotificationsEnabled(it) },
                    onToggleFoodLogNotification = { viewModel.setFoodLogNotificationEnabled(it) },
                    onToggleChatResponseNotification = { viewModel.setChatResponseNotificationEnabled(it) },
                    onToggleTokenAlerts = { viewModel.setTokenAlertsEnabled(it) },
                    onSetDailyTokenLimit = { viewModel.setDailyTokenAlertLimit(it) },
                    onSendTestNotification = { viewModel.sendTestNotification() },
                    onSendTestFoodLogNotification = { viewModel.sendTestFoodLogNotification() },
                    onSendTestChatNotification = { viewModel.sendTestChatResponseNotification() },
                    onSendTestTokenNotification = { viewModel.sendTestTokenAlertNotification() }
                )
            }

            // Local & Cloud Backup Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                                .clickable { isBackupExpanded = !isBackupExpanded }
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
                                        painter = painterResource(id = R.drawable.ic_save),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Backup & Cloud Sync",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Secure local backup & Google Drive sync",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isBackupExpanded = !isBackupExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_keyboard_arrow_down),
                                    contentDescription = if (isBackupExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.rotate(backupArrowRotation)
                                )
                            }
                        }

                        // Dropdown Content
                        AnimatedVisibility(
                            visible = isBackupExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .padding(bottom = 18.dp)
                            ) {
                                Text(
                                    text = "All your nutrition logs, target settings, and AI preferences can be securely saved locally or uploaded to your personal Google Drive.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Auto Backup Toggle
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Automatic Local Backup",
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.5.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Saves backup automatically after every logged meal",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Switch(
                                        checked = autoBackupEnabled,
                                        onCheckedChange = { viewModel.setAutoBackupEnabled(it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = Color.Black,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Button 1: Save Local Backup (Survives Uninstall)
                                Button(
                                    onClick = {
                                        viewModel.saveLocalBackup()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_save), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Local Backup (Survives Uninstall)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Button 2: Upload into Google Drive
                                OutlinedButton(
                                    onClick = {
                                        coroutineScope.launch {
                                            try {
                                                val intent = viewModel.getGoogleDriveUploadIntent()
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                snackbarHostState.showSnackbar("Unable to open Google Drive: ${e.message}")
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_file_download), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Upload Backup to Google Drive", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Button 3: Restore Backup (File picker)
                                OutlinedButton(
                                    onClick = {
                                        restoreFilePickerLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(painter = painterResource(id = R.drawable.ic_refresh), contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Restore from Backup File...", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                }

                                backupStatus?.let { status ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = status,
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // About Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Clickable Dropdown Header
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isAboutExpanded = !isAboutExpanded }
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
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "About MacroBite",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Version 1.1.0 • Privacy & architecture",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(
                                onClick = { isAboutExpanded = !isAboutExpanded },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_keyboard_arrow_down),
                                    contentDescription = if (isAboutExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.rotate(aboutArrowRotation)
                                )
                            }
                        }

                        // Dropdown Content
                        AnimatedVisibility(
                            visible = isAboutExpanded,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .padding(bottom = 18.dp)
                            ) {
                                Text(
                                    text = "Minimalist, zero-ad food tracking for student bulking & weight gain.",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "• Local-first database: your meals, target goals, and chat history stay on your device.\n• Zero ads, zero tracking, zero subscriptions.\n• Direct Google Gemini integration with your own free API key.",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 18.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentThemeId = themeColorPref,
            onSelectTheme = { selectedTheme ->
                viewModel.setThemeColor(selectedTheme)
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}
