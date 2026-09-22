package com.macrobite.app.ui.dashboard

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macrobite.app.data.parser.FoodPayloadHelper
import com.macrobite.app.data.parser.ParseResult
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.CustomFood
import com.macrobite.app.domain.model.DailyMacros
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.model.MealItem
import com.macrobite.app.domain.model.PresetFood
import com.macrobite.app.domain.model.UserTargets
import com.macrobite.app.domain.repository.CustomFoodRepository
import com.macrobite.app.domain.repository.MealRepository
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.domain.usecase.HostelTip
import com.macrobite.app.domain.usecase.HostelTipCoach
import com.macrobite.app.domain.usecase.ParseFoodUseCase
import com.macrobite.app.domain.model.DailyActivityData
import com.macrobite.app.domain.repository.WeightRepository
import com.macrobite.app.health.GoogleHealthManager
import com.macrobite.app.notification.NotificationHelper
import com.macrobite.app.domain.repository.ChatRepository
import com.macrobite.app.ui.chat.ChatFoodPayload
import com.macrobite.app.ui.chat.ChatMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val selectedDate: String = AppDate.todayIso(),
    val displayDateTitle: String = "Today",
    val isToday: Boolean = true,
    val dailyMacros: DailyMacros = DailyMacros(date = AppDate.todayIso()),
    val mealsGrouped: Map<MealCategory, List<MealEntry>> = emptyMap(),
    val hostelTip: HostelTip? = null,
    val isAiMode: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mealRepository: MealRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val parseFoodUseCase: ParseFoodUseCase,
    private val hostelTipCoach: HostelTipCoach,
    private val customFoodRepository: CustomFoodRepository,
    private val googleHealthManager: GoogleHealthManager,
    private val weightRepository: WeightRepository,
    private val backupManager: com.macrobite.app.data.backup.BackupManager,
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(AppDate.today())
    val selectedDate: StateFlow<AppDate> = _selectedDate.asStateFlow()

    private val _dailyActivity = MutableStateFlow(DailyActivityData())
    val dailyActivity: StateFlow<DailyActivityData> = _dailyActivity.asStateFlow()

    val themeColorPreference: StateFlow<String> = userPreferencesRepository.getThemeColor()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "amber")

    fun setThemeColor(colorId: String) {
        viewModelScope.launch {
            userPreferencesRepository.setThemeColor(colorId)
        }
    }

    private val _selectedMealForDetail = MutableStateFlow<MealEntry?>(null)
    val selectedMealForDetail: StateFlow<MealEntry?> = _selectedMealForDetail.asStateFlow()

    private val _mealPendingDelete = MutableStateFlow<MealEntry?>(null)
    val mealPendingDelete: StateFlow<MealEntry?> = _mealPendingDelete.asStateFlow()

    private val _undoDeleteEvent = MutableStateFlow<MealEntry?>(null)
    val undoDeleteEvent: StateFlow<MealEntry?> = _undoDeleteEvent.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _activeQuery = MutableStateFlow("")
    val activeQuery: StateFlow<String> = _activeQuery.asStateFlow()

    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _parsingMessage = MutableStateFlow("Calculating nutrition...")
    val parsingMessage: StateFlow<String> = _parsingMessage.asStateFlow()

    private var parseJob: Job? = null

    private val _pendingParseResult = MutableStateFlow<ParseResult?>(null)
    val pendingParseResult: StateFlow<ParseResult?> = _pendingParseResult.asStateFlow()

    private val _pendingPhotoUri = MutableStateFlow<String?>(null)
    val pendingPhotoUri: StateFlow<String?> = _pendingPhotoUri.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var lastDeletedMeal: MealEntry? = null

    val targets: StateFlow<UserTargets> = userPreferencesRepository.getTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserTargets())

    val useGemini: StateFlow<Boolean> = userPreferencesRepository.getUseGemini()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val geminiApiKey: StateFlow<String> = userPreferencesRepository.getGeminiApiKey()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val customFoods: StateFlow<List<CustomFood>> = customFoodRepository.getAllCustomFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val customBarcodes: StateFlow<Map<String, String>> = userPreferencesRepository.getCustomBarcodes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    init {
        viewModelScope.launch {
            customFoodRepository.seedDefaultsIfNeeded()
        }
        viewModelScope.launch {
            _selectedDate.collect { date ->
                syncGoogleHealthActivity(date.toIsoString())
            }
        }
    }

    private val _mealsForSelectedDate = _selectedDate.flatMapLatest { date ->
        mealRepository.getMealsForDate(date.toIsoString())
    }

    private val _tipDismissed = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = combine(
        _selectedDate,
        _mealsForSelectedDate,
        targets,
        combine(useGemini, geminiApiKey, _tipDismissed) { a, b, c -> Triple(a, b, c) }
    ) { date, meals, currentTargets, (geminiActive, apiKey, isTipDismissed) ->
        val dateString = date.toIsoString()
        val today = AppDate.today()
        val isToday = date == today
        val dateTitle = date.toDisplayTitle(today)

        val totalCal = meals.sumOf { it.calories }
        val totalProt = meals.fold(0f) { acc, m -> acc + m.protein }
        val totalCarbs = meals.fold(0f) { acc, m -> acc + m.carbs }
        val totalFats = meals.fold(0f) { acc, m -> acc + m.fats }

        val macros = DailyMacros(
            date = dateString,
            totalCalories = totalCal,
            totalProtein = totalProt,
            totalCarbs = totalCarbs,
            totalFats = totalFats,
            targetCalories = currentTargets.calories,
            targetProtein = currentTargets.protein,
            targetCarbs = currentTargets.carbs,
            targetFats = currentTargets.fats,
            meals = meals
        )

        val grouped = MealCategory.entries.associateWith { cat ->
            meals.filter { it.category == cat }
        }

        val tip = if (isToday && !isTipDismissed) hostelTipCoach.evaluateTip(macros, dateString) else null

        DashboardUiState(
            selectedDate = dateString,
            displayDateTitle = dateTitle,
            isToday = isToday,
            dailyMacros = macros,
            mealsGrouped = grouped,
            hostelTip = tip,
            isAiMode = geminiActive && apiKey.isNotBlank()
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DashboardUiState()
    )

    fun dismissTip() {
        _tipDismissed.value = true
    }

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun selectPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun selectNextDay() {
        _selectedDate.value = _selectedDate.value.plusDays(1)
    }

    fun selectToday() {
        _selectedDate.value = AppDate.today()
    }

    /**
     * Synchronizes footsteps and calories burned from Google Health Connect.
     */
    fun syncGoogleHealthActivity(targetDateIso: String = _selectedDate.value.toIsoString()) {
        viewModelScope.launch {
            _dailyActivity.value = _dailyActivity.value.copy(isSyncing = true)
            val latestWeight = try {
                weightRepository.getAllWeights().firstOrNull()?.firstOrNull()?.weightKg ?: 70f
            } catch (_: Throwable) {
                70f
            }
            val stepGoal = try {
                userPreferencesRepository.getStepGoal().firstOrNull() ?: 10_000
            } catch (_: Throwable) {
                10_000
            }

            val activity = googleHealthManager.readDailyActivity(
                context = context,
                dateIso = targetDateIso,
                userWeightKg = latestWeight,
                stepGoal = stepGoal
            )
            _dailyActivity.value = activity.copy(isSyncing = false)
        }
    }

    /**
     * Callback when the user responds to Health Connect permission dialog.
     */
    fun onHealthConnectPermissionsResult(granted: Boolean) {
        if (granted) {
            syncGoogleHealthActivity()
        } else {
            viewModelScope.launch {
                val hasPerms = googleHealthManager.hasPermissions(context)
                _dailyActivity.value = _dailyActivity.value.copy(hasPermission = hasPerms)
            }
        }
    }

    fun openHealthConnectPlayStore() {
        googleHealthManager.openPlayStoreForHealthConnect(context)
    }

    fun openHealthConnectSettings() {
        googleHealthManager.openHealthConnectSettings(context)
    }

    fun submitText() {
        val query = _inputText.value.trim()
        if (query.isBlank() || _isParsing.value) return

        _activeQuery.value = query
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _parsingMessage.value = "Calculating nutrition..."
            _isParsing.value = true
            val startTime = System.currentTimeMillis()
            try {
                val result = parseFoodUseCase.parseText(query)
                val elapsed = System.currentTimeMillis() - startTime
                val minDuration = 3000L // 3 seconds per user requirement
                if (elapsed < minDuration) {
                    delay(minDuration - elapsed)
                }
                _inputText.value = ""
                _pendingParseResult.value = result
                _pendingPhotoUri.value = null
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                e.printStackTrace()
                _snackbarMessage.value = "Failed to parse meal: ${e.localizedMessage}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    fun onPhotoSelected(uri: Uri, context: Context, barcodeToSave: String? = null) {
        _activeQuery.value = _inputText.value.ifBlank { "Meal Photo" }
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _parsingMessage.value = "Analyzing meal photo..."
            _isParsing.value = true
            val startTime = System.currentTimeMillis()
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (bitmap != null) {
                    val result = parseFoodUseCase.parseImage(bitmap, _inputText.value.ifBlank { null })
                    val elapsed = System.currentTimeMillis() - startTime
                    val minDuration = 3000L // 3 seconds per user requirement
                    if (elapsed < minDuration) {
                        delay(minDuration - elapsed)
                    }
                    _pendingPhotoUri.value = uri.toString()
                    _pendingParseResult.value = result
                    _inputText.value = ""

                    // Save barcode locally if requested and we have valid items
                    if (!barcodeToSave.isNullOrBlank() && result.items.isNotEmpty()) {
                        val firstItem = result.items.first()
                        val mealEntry = MealEntry(
                            date = AppDate.todayIso(),
                            category = MealCategory.SNACKS, // Defaulting to SNACKS
                            foodName = firstItem.name,
                            portion = firstItem.portion,
                            calories = firstItem.calories,
                            protein = firstItem.protein,
                            carbs = firstItem.carbs,
                            fats = firstItem.fats,
                            fiber = firstItem.fiber,
                            sugar = firstItem.sugar,
                            sodium = firstItem.sodium
                        )
                        val resultJson = com.google.gson.Gson().toJson(mealEntry)
                        userPreferencesRepository.saveCustomBarcode(barcodeToSave, resultJson)
                    }
                } else {
                    _snackbarMessage.value = "Could not decode image"
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                e.printStackTrace()
                _snackbarMessage.value = "Failed to load image: ${e.localizedMessage}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    fun regenerateItem(index: Int, name: String, portion: String) {
        val currentResult = _pendingParseResult.value ?: return
        val trimmedName = name.trim()
        val trimmedPortion = portion.trim()
        if (trimmedName.isBlank()) return

        val resolved = FoodPayloadHelper.resolve(trimmedName, trimmedPortion)

        _activeQuery.value = resolved.foodName
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _parsingMessage.value = "Calculating nutrition for ${resolved.foodName}..."
            _isParsing.value = true
            val startTime = System.currentTimeMillis()
            try {
                val singleResult = if (!_pendingPhotoUri.value.isNullOrBlank()) {
                    val bitmap = try {
                        val uri = Uri.parse(_pendingPhotoUri.value)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
                    } catch (e: Exception) {
                        null
                    }
                    if (bitmap != null) {
                        parseFoodUseCase.parseImage(bitmap, promptNote = resolved.fullQuery)
                    } else {
                        parseFoodUseCase.parseText(resolved.fullQuery)
                    }
                } else {
                    parseFoodUseCase.parseText(resolved.fullQuery)
                }

                val newItem = singleResult.items.firstOrNull() ?: MealItem(
                    name = resolved.foodName,
                    portion = resolved.portion,
                    calories = 250,
                    protein = 8f,
                    carbs = 30f,
                    fats = 8f
                )
                val elapsed = System.currentTimeMillis() - startTime
                val minDuration = 3000L // 3 seconds per user requirement
                if (elapsed < minDuration) {
                    delay(minDuration - elapsed)
                }
                val currentItems = currentResult.items.toMutableList()
                if (index in currentItems.indices) {
                    val finalPortion = if (newItem.portion.isNotBlank() && !newItem.portion.equals("1 serving", ignoreCase = true)) {
                        newItem.portion
                    } else {
                        resolved.portion
                    }
                    currentItems[index] = newItem.copy(
                        name = resolved.foodName,
                        portion = finalPortion
                    )
                    _pendingParseResult.value = currentResult.copy(
                        items = currentItems,
                        source = singleResult.source
                    )
                    _snackbarMessage.value = "Updated ${resolved.foodName} nutrition"
                }
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                e.printStackTrace()
                _snackbarMessage.value = "Failed to calculate: ${e.localizedMessage}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    fun regenerateAll(items: List<MealItem>) {
        val currentResult = _pendingParseResult.value ?: return
        if (items.isEmpty()) return

        _activeQuery.value = items.firstOrNull()?.name ?: "All Meals"
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _parsingMessage.value = "Calculating meal nutrition..."
            _isParsing.value = true
            val startTime = System.currentTimeMillis()
            try {
                val inputNote = items.joinToString(", ") { "${it.portion} ${it.name}".trim() }
                val newResult = if (!_pendingPhotoUri.value.isNullOrBlank()) {
                    val bitmap = try {
                        val uri = Uri.parse(_pendingPhotoUri.value)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
                    } catch (e: Exception) {
                        null
                    }
                    if (bitmap != null) {
                        parseFoodUseCase.parseImage(bitmap, promptNote = inputNote)
                    } else {
                        parseFoodUseCase.parseText(inputNote)
                    }
                } else {
                    parseFoodUseCase.parseText(inputNote)
                }
                val elapsed = System.currentTimeMillis() - startTime
                val minDuration = 3000L // 3 seconds per user requirement
                if (elapsed < minDuration) {
                    delay(minDuration - elapsed)
                }
                _pendingParseResult.value = currentResult.copy(
                    items = if (newResult.items.isNotEmpty()) newResult.items else items,
                    source = newResult.source
                )
                _snackbarMessage.value = "Updated all meal macros"
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                e.printStackTrace()
                _snackbarMessage.value = "Failed to recalculate: ${e.localizedMessage}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    fun cancelParsing() {
        parseJob?.cancel()
        parseJob = null
        _isParsing.value = false
    }

    fun logCustomFood(food: CustomFood, category: MealCategory) {
        viewModelScope.launch {
            val entry = MealEntry(
                date = _selectedDate.value.toIsoString(),
                category = category,
                foodName = food.name,
                portion = food.portion,
                calories = food.calories,
                protein = food.protein,
                carbs = food.carbs,
                fats = food.fats
            )
            mealRepository.insertMeal(entry)
            notifyFoodLogged(entry)
            _snackbarMessage.value = "Logged ${food.name} (+${food.calories} kcal)"
            triggerAutoBackup()
        }
    }

    fun addCustomFood(name: String, portion: String, calories: Int, protein: Float, carbs: Float, fats: Float) {
        viewModelScope.launch {
            val customFood = CustomFood(
                name = name,
                portion = portion,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fats = fats
            )
            customFoodRepository.insertCustomFood(customFood)
            _snackbarMessage.value = "Saved custom food: $name"
        }
    }

    fun deleteCustomFood(foodId: Long) {
        viewModelScope.launch {
            customFoodRepository.deleteCustomFood(foodId)
            _snackbarMessage.value = "Removed custom food"
        }
    }

    fun logPreset(preset: PresetFood, category: MealCategory) {
        viewModelScope.launch {
            val entry = MealEntry(
                date = _selectedDate.value.toIsoString(),
                category = category,
                foodName = preset.name,
                portion = preset.portion,
                calories = preset.calories,
                protein = preset.protein,
                carbs = preset.carbs,
                fats = preset.fats
            )
            mealRepository.insertMeal(entry)
            notifyFoodLogged(entry)
            _snackbarMessage.value = "Logged ${preset.name} (+${preset.calories} kcal)"
            triggerAutoBackup()
        }
    }

    fun quickAddTip(items: List<MealEntry>) {
        viewModelScope.launch {
            mealRepository.insertMeals(items)
            notifyMultipleFoodsLogged(items)
            val addedCalories = items.sumOf { it.calories }
            _snackbarMessage.value = "Added top-up: +$addedCalories kcal!"
            triggerAutoBackup()
        }
    }

    fun confirmParsedMeals(entries: List<MealEntry>) {
        viewModelScope.launch {
            mealRepository.insertMeals(entries)
            notifyMultipleFoodsLogged(entries)
            _pendingParseResult.value = null
            _pendingPhotoUri.value = null
            _snackbarMessage.value = "Logged ${entries.size} item(s)"
            triggerAutoBackup()
        }
    }

    fun logScannedFood(entry: MealEntry) {
        viewModelScope.launch {
            val entryWithDate = entry.copy(date = _selectedDate.value.toIsoString())
            mealRepository.insertMeal(entryWithDate)
            notifyFoodLogged(entryWithDate)
            _snackbarMessage.value = "Logged ${entry.foodName} (${entry.calories} kcal)"
            triggerAutoBackup()
        }
    }

    private fun triggerAutoBackup() {
        viewModelScope.launch {
            val isAutoBackup = true
            if (isAutoBackup) {
                backupManager.performAutoBackup()
            }
        }
    }

    private suspend fun notifyFoodLogged(entry: MealEntry) {
        mirrorLoggedMealToChat(entry)
        try {
            val notifPrefs = userPreferencesRepository.getNotificationPreferences().firstOrNull()
            if (notifPrefs?.enabled == true && notifPrefs.foodLogNotificationEnabled) {
                NotificationHelper.showFoodLoggedNotification(context, entry)
            }
        } catch (e: Exception) {
            // Non-critical notification error
        }
    }

    private suspend fun notifyMultipleFoodsLogged(entries: List<MealEntry>) {
        entries.forEach { mirrorLoggedMealToChat(it) }
        try {
            val notifPrefs = userPreferencesRepository.getNotificationPreferences().firstOrNull()
            if (notifPrefs?.enabled == true && notifPrefs.foodLogNotificationEnabled && entries.isNotEmpty()) {
                NotificationHelper.showMultipleFoodsLoggedNotification(context, entries)
            }
        } catch (e: Exception) {
            // Non-critical notification error
        }
    }

    private suspend fun mirrorLoggedMealToChat(entry: MealEntry) {
        try {
            val payload = ChatFoodPayload(
                foodName = entry.foodName,
                portion = entry.portion,
                category = entry.category,
                calories = entry.calories,
                protein = entry.protein,
                carbs = entry.carbs,
                fats = entry.fats,
                fiber = entry.fiber,
                sugar = entry.sugar,
                sodium = entry.sodium,
                vitaminsAndMinerals = entry.vitaminsAndMinerals
            )
            val chatMsg = ChatMessage(
                text = "Logged ${entry.foodName} (${entry.portion}) to ${entry.category.displayName}!",
                isUser = false,
                timestamp = if (entry.timestamp > 0) entry.timestamp else System.currentTimeMillis(),
                foodPayload = payload,
                isLogged = true
            )
            chatRepository.saveMessage(entry.date, chatMsg)
        } catch (e: Exception) {
            android.util.Log.w("DashboardViewModel", "Could not mirror meal to chat", e)
        }
    }

    fun dismissParseDialog() {
        _pendingParseResult.value = null
        _pendingPhotoUri.value = null
    }

    fun selectMealForDetail(meal: MealEntry) {
        _selectedMealForDetail.value = meal
    }

    fun dismissMealDetail() {
        _selectedMealForDetail.value = null
    }

    fun requestDeleteMeal(meal: MealEntry) {
        _mealPendingDelete.value = meal
    }

    fun cancelDeleteMeal() {
        _mealPendingDelete.value = null
    }

    fun confirmDeleteMeal() {
        val meal = _mealPendingDelete.value ?: return
        _mealPendingDelete.value = null
        lastDeletedMeal = meal
        viewModelScope.launch {
            mealRepository.deleteMeal(meal.id)
            triggerAutoBackup()
            if (_selectedMealForDetail.value?.id == meal.id) {
                _selectedMealForDetail.value = null
            }
            _undoDeleteEvent.value = meal
        }
    }

    fun clearUndoDeleteEvent() {
        _undoDeleteEvent.value = null
    }

    fun deleteMeal(meal: MealEntry) {
        requestDeleteMeal(meal)
    }

    fun undoDelete() {
        val mealToRestore = lastDeletedMeal ?: return
        viewModelScope.launch {
            mealRepository.insertMeal(mealToRestore.copy(id = 0))
            lastDeletedMeal = null
            _snackbarMessage.value = "Restored ${mealToRestore.foodName}"
            triggerAutoBackup()
        }
    }

    fun regenerateLoggedMeal(meal: MealEntry, updatedPortion: String, updatedName: String) {
        val trimmedName = updatedName.trim().ifBlank { meal.foodName }
        val trimmedPortion = updatedPortion.trim().ifBlank { meal.portion }
        val resolved = FoodPayloadHelper.resolve(trimmedName, trimmedPortion)

        _activeQuery.value = resolved.foodName
        parseJob?.cancel()
        parseJob = viewModelScope.launch {
            _parsingMessage.value = "Calculating nutrition for ${resolved.foodName}..."
            _isParsing.value = true
            val startTime = System.currentTimeMillis()
            try {
                val singleResult = if (!meal.photoUri.isNullOrBlank()) {
                    val bitmap = try {
                        val uri = Uri.parse(meal.photoUri)
                        val inputStream = context.contentResolver.openInputStream(uri)
                        BitmapFactory.decodeStream(inputStream).also { inputStream?.close() }
                    } catch (e: Exception) {
                        null
                    }
                    if (bitmap != null) {
                        parseFoodUseCase.parseImage(bitmap, promptNote = resolved.fullQuery)
                    } else {
                        parseFoodUseCase.parseText(resolved.fullQuery)
                    }
                } else {
                    parseFoodUseCase.parseText(resolved.fullQuery)
                }

                val newItem = singleResult.items.firstOrNull() ?: MealItem(
                    name = resolved.foodName,
                    portion = resolved.portion,
                    calories = meal.calories,
                    protein = meal.protein,
                    carbs = meal.carbs,
                    fats = meal.fats,
                    fiber = meal.fiber,
                    sugar = meal.sugar,
                    sodium = meal.sodium,
                    saturatedFat = meal.saturatedFat,
                    potassium = meal.potassium,
                    cholesterol = meal.cholesterol,
                    vitaminsAndMinerals = meal.vitaminsAndMinerals
                )

                val elapsed = System.currentTimeMillis() - startTime
                val minDuration = 3000L // 3 seconds per user requirement
                if (elapsed < minDuration) {
                    delay(minDuration - elapsed)
                }

                val finalPortion = if (newItem.portion.isNotBlank() && !newItem.portion.equals("1 serving", ignoreCase = true)) {
                    newItem.portion
                } else {
                    resolved.portion
                }

                val updatedEntry = meal.copy(
                    foodName = resolved.foodName,
                    portion = finalPortion,
                    calories = newItem.calories,
                    protein = newItem.protein,
                    carbs = newItem.carbs,
                    fats = newItem.fats,
                    fiber = newItem.fiber,
                    sugar = newItem.sugar,
                    sodium = newItem.sodium,
                    saturatedFat = newItem.saturatedFat,
                    potassium = newItem.potassium,
                    cholesterol = newItem.cholesterol,
                    vitaminsAndMinerals = newItem.vitaminsAndMinerals
                )

                mealRepository.updateMeal(updatedEntry)
                _selectedMealForDetail.value = updatedEntry
                _snackbarMessage.value = "Updated nutrition for ${resolved.foodName}"
            } catch (e: Exception) {
                if (e is CancellationException) return@launch
                e.printStackTrace()
                _snackbarMessage.value = "Failed to recalculate: ${e.localizedMessage}"
            } finally {
                _isParsing.value = false
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }
}
