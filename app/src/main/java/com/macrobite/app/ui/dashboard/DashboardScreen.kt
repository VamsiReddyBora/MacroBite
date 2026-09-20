package com.macrobite.app.ui.dashboard

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.health.connect.client.PermissionController
import com.macrobite.app.health.GoogleHealthManager
import com.macrobite.app.ui.scanner.BarcodeNutritionScannerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.macrobite.app.R
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.runtime.Composable
import com.macrobite.app.ui.common.ModernFloatingSnackbarHost
import com.macrobite.app.ui.common.ModernFloatingUndoHud
import com.macrobite.app.ui.common.ThemePickerDialog
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import com.macrobite.app.ui.common.rememberNetworkConnectivity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.macrobite.app.domain.model.MealCategory
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val activeQuery by viewModel.activeQuery.collectAsState()
    val isParsing by viewModel.isParsing.collectAsState()
    val parsingMessage by viewModel.parsingMessage.collectAsState()
    val pendingParseResult by viewModel.pendingParseResult.collectAsState()
    val pendingPhotoUri by viewModel.pendingPhotoUri.collectAsState()
    val snackbarMessage by viewModel.snackbarMessage.collectAsState()
    val customFoods by viewModel.customFoods.collectAsState()
    val selectedMealForDetail by viewModel.selectedMealForDetail.collectAsState()
    val mealPendingDelete by viewModel.mealPendingDelete.collectAsState()
    val undoDeleteEvent by viewModel.undoDeleteEvent.collectAsState()
    val dailyActivity by viewModel.dailyActivity.collectAsState()

    // Health Connect Permission Request Launcher
    val healthConnectLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        val hasRequired = grantedPermissions.containsAll(GoogleHealthManager.REQUIRED_PERMISSIONS)
            || grantedPermissions.any { it.contains("STEPS", ignoreCase = true) }
        viewModel.onHealthConnectPermissionsResult(hasRequired)
    }

    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Clear focus on launch so soft keyboard does not auto popup
    LaunchedEffect(Unit) {
        focusManager.clearFocus()
    }

    var showPresetsSheet by remember { mutableStateOf(false) }
    var showPhotoChoiceDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showBarcodeScannerDialog by remember { mutableStateOf(false) }

    val themeColorPref by viewModel.themeColorPreference.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Camera photo URI holder
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    // Launcher for taking photo via Camera
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempCameraUri != null) {
            viewModel.onPhotoSelected(tempCameraUri!!, context)
        }
    }

    val launchCameraAction = {
        try {
            val photoFile = File(context.cacheDir, "meal_capture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } catch (_: SecurityException) {
            // Handled gracefully
        } catch (_: Throwable) {
            // Handled gracefully
        }
    }

    // Camera Permission Launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraAction()
        }
    }

    val requestCameraAndLaunch = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            launchCameraAction()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Launcher for picking photo from Gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.onPhotoSelected(uri, context)
        }
    }

    // Handle regular snackbar messages
    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.selectPreviousDay() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Previous Day",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .pointerInput(Unit) {
                                    var totalDragX = 0f
                                    detectHorizontalDragGestures(
                                        onDragStart = { totalDragX = 0f },
                                        onDragEnd = {
                                            val swipeThreshold = 20.dp.toPx()
                                            if (totalDragX < -swipeThreshold) {
                                                viewModel.selectNextDay()
                                            } else if (totalDragX > swipeThreshold) {
                                                viewModel.selectPreviousDay()
                                            }
                                            totalDragX = 0f
                                        },
                                        onDragCancel = {
                                            totalDragX = 0f
                                        },
                                        onHorizontalDrag = { change, dragAmount ->
                                            change.consume()
                                            totalDragX += dragAmount
                                        }
                                    )
                                }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            AnimatedContent(
                                targetState = uiState.displayDateTitle,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(150))
                                },
                                label = "date_switch_anim"
                            ) { dateTitle ->
                                Text(
                                    text = dateTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        IconButton(
                            onClick = { viewModel.selectNextDay() },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Next Day",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (!uiState.isToday) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                    .clickable { viewModel.selectToday() }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "TODAY",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp
                                    ),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    val isConnected by rememberNetworkConnectivity()

                    val statusColor = when {
                        !isConnected -> Color(0xFFEF4444) // Red - no network
                        !uiState.isAiMode -> Color(0xFFF59E0B) // Amber - network ok but AI off
                        else -> Color(0xFF22C55E) // Green - all systems go
                    }
                    val statusText = when {
                        !isConnected -> "Offline"
                        !uiState.isAiMode -> "AI Engine Off"
                        else -> "AI Active"
                    }

                    IconButton(
                        onClick = { showThemeDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_palette),
                            contentDescription = "App Theme Color",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                            .clickable { onNavigateToSettings() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_bolt),
                            contentDescription = statusText,
                            tint = Color.Black,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                ModernFloatingUndoHud(
                    meal = undoDeleteEvent,
                    onUndo = {
                        viewModel.undoDelete()
                        viewModel.clearUndoDeleteEvent()
                    },
                    onDismiss = {
                        viewModel.clearUndoDeleteEvent()
                    }
                )
                QuickLogBar(
                    text = inputText,
                    onTextChange = { viewModel.onInputTextChanged(it) },
                    onSubmit = { viewModel.submitText() },
                    onCameraClick = { showPhotoChoiceDialog = true },
                    onPresetsClick = { showPresetsSheet = true },
                    isLoading = isParsing
                )
            }
        },
        snackbarHost = {
            ModernFloatingSnackbarHost(snackbarHostState)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Macro Gauge Widget
            item {
                MacroGaugeWidget(
                    dailyMacros = uiState.dailyMacros,
                    dailyActivity = dailyActivity
                )
            }

            // Google Health Footsteps & Calorie Burn Widget
            item {
                ActivityTrackerCard(
                    activityData = dailyActivity,
                    eatenCalories = uiState.dailyMacros.totalCalories,
                    onSyncClick = { viewModel.syncGoogleHealthActivity() },
                    onRequestPermissions = {
                        healthConnectLauncher.launch(GoogleHealthManager.REQUIRED_PERMISSIONS)
                    },
                    onInstallHealthConnect = { viewModel.openHealthConnectPlayStore() }
                )
            }

            // Meals Feed Grouped by Category
            val allMealsEmpty = uiState.dailyMacros.meals.isEmpty()
            if (allMealsEmpty) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 36.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_nav_restaurant),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No meals logged yet",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Type food below, snap a photo, or 1-tap custom foods.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                MealCategory.entries.forEach { category ->
                    val categoryMeals = uiState.mealsGrouped[category] ?: emptyList()
                    if (categoryMeals.isNotEmpty()) {
                        item(key = "header_${category.name}") {
                            MealCategoryHeader(
                                category = category,
                                meals = categoryMeals
                            )
                        }

                        items(
                            count = categoryMeals.size,
                            key = { index -> "${categoryMeals[index].id}_${category.name}_$index" }
                        ) { index ->
                            val meal = categoryMeals[index]
                            MealItemCard(
                                meal = meal,
                                onClick = { viewModel.selectMealForDetail(meal) },
                                onDelete = { viewModel.requestDeleteMeal(meal) }
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                        }
                    }
                }
            }

            // Extra space at bottom
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Custom Foods (1-Tap Quick Log) Bottom Sheet
    if (showPresetsSheet) {
        val currentMeal = when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> MealCategory.BREAKFAST
            in 12..16 -> MealCategory.LUNCH
            in 17..19 -> MealCategory.SNACKS
            else -> MealCategory.DINNER
        }

        CustomFoodsBottomSheet(
            sheetState = sheetState,
            customFoods = customFoods,
            currentMealCategory = currentMeal,
            onDismiss = { showPresetsSheet = false },
            onLogCustomFood = { food, category ->
                viewModel.logCustomFood(food, category)
            },
            onAddCustomFood = { name, portion, cal, p, c, f ->
                viewModel.addCustomFood(name, portion, cal, p, c, f)
            },
            onDeleteCustomFood = { foodId ->
                viewModel.deleteCustomFood(foodId)
            }
        )
    }

    // Parsed Confirmation / Review Dialog
    pendingParseResult?.let { parseResult ->
        ParsedReviewDialog(
            parseResult = parseResult,
            targetDate = uiState.selectedDate,
            photoUri = pendingPhotoUri,
            onDismiss = { viewModel.dismissParseDialog() },
            onConfirm = { entries -> viewModel.confirmParsedMeals(entries) },
            onRegenerateItem = { index, name, portion ->
                viewModel.regenerateItem(index, name, portion)
            },
            onRegenerateAll = { items ->
                viewModel.regenerateAll(items)
            }
        )
    }

    // Tom & Jerry Nutrition Calculation Pop-up Animation
    if (isParsing) {
        TomAndJerryCalculatingPopup(
            message = parsingMessage,
            foodQuery = activeQuery,
            onCancel = { viewModel.cancelParsing() }
        )
    }

    // Photo Choice Dialog (Camera vs Gallery vs Barcode)
    if (showPhotoChoiceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoChoiceDialog = false },
            title = { Text("Log Meal Photo") },
            text = { Text("Choose a photo from your gallery, take a new picture, or scan a barcode.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPhotoChoiceDialog = false
                        requestCameraAndLaunch()
                    }
                ) {
                    Icon(painter = painterResource(id = R.drawable.ic_camera), contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo")
                }
            },
            dismissButton = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(
                        onClick = {
                            showPhotoChoiceDialog = false
                            showBarcodeScannerDialog = true
                        }
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_barcode_scanner), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Barcode")
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(
                        onClick = {
                            showPhotoChoiceDialog = false
                            galleryLauncher.launch("image/*")
                        }
                    ) {
                        Icon(painter = painterResource(id = R.drawable.ic_photo_library), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery")
                    }
                }
            }
        )
    }

    if (showBarcodeScannerDialog) {
        val currentCustomBarcodes by viewModel.customBarcodes.collectAsState()
        BarcodeNutritionScannerDialog(
            customBarcodes = currentCustomBarcodes,
            onDismiss = { showBarcodeScannerDialog = false },
            onFoodScanned = { entry ->
                showBarcodeScannerDialog = false
                viewModel.logScannedFood(entry)
            },
            onAnalyzeLabelPhoto = { photoPath, barcode ->
                showBarcodeScannerDialog = false
                val file = java.io.File(photoPath)
                val uri = if (file.exists()) {
                    try {
                        FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                    } catch (_: Throwable) {
                        Uri.fromFile(file)
                    }
                } else {
                    Uri.parse(photoPath)
                }
                viewModel.onPhotoSelected(uri, context, barcode)
            },
            onBarcodeUnknown = { barcode ->
                showBarcodeScannerDialog = false
                viewModel.onInputTextChanged("Find nutrition for barcode: $barcode")
                viewModel.submitText()
            }
        )
    }

    // Logged Meal Detail Dialog
    selectedMealForDetail?.let { meal ->
        MealDetailDialog(
            meal = meal,
            onDismiss = { viewModel.dismissMealDetail() },
            onRegenerate = { updatedPortion, updatedName ->
                viewModel.regenerateLoggedMeal(meal, updatedPortion, updatedName)
            },
            onDelete = {
                viewModel.requestDeleteMeal(meal)
            }
        )
    }

    // Delete Confirmation Dialog
    mealPendingDelete?.let { meal ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteMeal() },
            title = {
                Text(
                    text = "Delete Food Log?",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Text("Are you sure you want to delete \"${meal.foodName}\"?")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.confirmDeleteMeal() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDeleteMeal() }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentThemeId = themeColorPref,
            onSelectTheme = { colorId ->
                viewModel.setThemeColor(colorId)
            },
            onDismiss = { showThemeDialog = false }
        )
    }
}
