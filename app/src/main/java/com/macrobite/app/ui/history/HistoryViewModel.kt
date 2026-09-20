package com.macrobite.app.ui.history

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.model.UserTargets
import com.macrobite.app.domain.repository.MealRepository
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.domain.usecase.ExportCsvUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HistoryUiState(
    val selectedDate: AppDate = AppDate.today(),
    val weekCalorieData: List<DayCalorieData> = emptyList(),
    val mealsForSelectedDate: List<MealEntry> = emptyList(),
    val totalCaloriesForSelectedDate: Int = 0,
    val totalProteinForSelectedDate: Float = 0f,
    val totalCarbsForSelectedDate: Float = 0f,
    val totalFatsForSelectedDate: Float = 0f,
    val targetCalories: Int = 3000,
    val targetProtein: Int = 110,
    val targetCarbs: Int = 360,
    val targetFats: Int = 110,
    val exportShareIntent: Intent? = null,
    val message: String? = null
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val mealRepository: MealRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val exportCsvUseCase: ExportCsvUseCase
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(AppDate.today())
    val selectedDate: StateFlow<AppDate> = _selectedDate.asStateFlow()

    private val _exportShareIntent = MutableStateFlow<Intent?>(null)
    val exportShareIntent: StateFlow<Intent?> = _exportShareIntent.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val targets: StateFlow<UserTargets> = userPreferencesRepository.getTargets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserTargets())

    // 7 days window ending today
    private val weekDates: List<AppDate> = (6 downTo 0).map { AppDate.today().minusDays(it) }

    val uiState: StateFlow<HistoryUiState> = combine(
        _selectedDate,
        mealRepository.getAllMeals(),
        targets,
        _exportShareIntent,
        _message
    ) { selDate, allMeals, currentTargets, shareIntent, msg ->
        val selectedDateStr = selDate.toIsoString()
        val mealsOnSelectedDate = allMeals.filter { it.date == selectedDateStr }

        val weekData = weekDates.map { date ->
            val dateStr = date.toIsoString()
            val cal = allMeals.filter { it.date == dateStr }.sumOf { it.calories }
            DayCalorieData(
                date = date,
                calories = cal,
                targetCalories = currentTargets.calories
            )
        }

        HistoryUiState(
            selectedDate = selDate,
            weekCalorieData = weekData,
            mealsForSelectedDate = mealsOnSelectedDate,
            totalCaloriesForSelectedDate = mealsOnSelectedDate.sumOf { it.calories },
            totalProteinForSelectedDate = mealsOnSelectedDate.fold(0f) { acc, m -> acc + m.protein },
            totalCarbsForSelectedDate = mealsOnSelectedDate.fold(0f) { acc, m -> acc + m.carbs },
            totalFatsForSelectedDate = mealsOnSelectedDate.fold(0f) { acc, m -> acc + m.fats },
            targetCalories = currentTargets.calories,
            targetProtein = currentTargets.protein,
            targetCarbs = currentTargets.carbs,
            targetFats = currentTargets.fats,
            exportShareIntent = shareIntent,
            message = msg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        HistoryUiState()
    )

    fun selectDate(date: AppDate) {
        _selectedDate.value = date
    }

    fun exportToCsv() {
        viewModelScope.launch {
            try {
                val exportedMeals = mealRepository.getAllMeals().first()
                if (exportedMeals.isEmpty()) {
                    _message.value = "No meals to export yet!"
                    return@launch
                }
                val intent = exportCsvUseCase.generateCsvFile(exportedMeals)
                _exportShareIntent.value = intent
            } catch (e: Exception) {
                e.printStackTrace()
                _message.value = "Export failed: ${e.localizedMessage}"
            }
        }
    }

    fun clearExportIntent() {
        _exportShareIntent.value = null
    }

    fun deleteMeal(meal: MealEntry) {
        viewModelScope.launch {
            mealRepository.deleteMeal(meal.id)
        }
    }

    fun updateMeal(meal: MealEntry) {
        viewModelScope.launch {
            mealRepository.updateMeal(meal)
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
