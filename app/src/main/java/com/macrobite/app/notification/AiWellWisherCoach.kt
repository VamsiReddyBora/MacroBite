package com.macrobite.app.notification

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.model.NotificationPreferences
import com.macrobite.app.domain.model.UserTargets
import com.macrobite.app.domain.repository.MealRepository
import com.macrobite.app.domain.repository.UserPreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class WellWisherAdvice(
    val title: String,
    val body: String,
    val recommendedActionFood: String? = null,
    val remainingCalories: Int = 0,
    val isCongratulatory: Boolean = false
)

/**
 * Intelligent AI Nutrition Well-Wisher Coach
 *
 * Dynamically analyzes:
 * 1. Real-time food logs in database for today
 * 2. Exact calorie & protein remaining deficit against user personal targets
 * 3. Whether the user forgot to log or is experiencing eating difficulty / fatigue
 * 4. User dietary preferences (hostel / student / vegetarian / non-veg)
 * 5. Time-of-day availability and recommends practical high-calorie replacement foods
 *
 * Employs Gemini Generative AI for creative, warm, empathetic advice when online,
 * with a bulletproof smart local rule engine fallback ensuring 100% on-time delivery.
 */
@Singleton
class AiWellWisherCoach @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mealRepository: MealRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "AiWellWisherCoach"
    }

    suspend fun generateWellWisherAdvice(
        mealType: String,
        aiName: String = "AI Coach"
    ): WellWisherAdvice = withContext(Dispatchers.IO) {
        val today = AppDate.todayIso()
        val mealsToday = try {
            mealRepository.getMealsForDate(today).firstOrNull() ?: emptyList()
        } catch (e: Throwable) {
            emptyList()
        }

        val targets = try {
            userPreferencesRepository.getTargets().firstOrNull() ?: UserTargets()
        } catch (e: Throwable) {
            UserTargets()
        }

        val dietaryNotes = try {
            userPreferencesRepository.getRaayaDietaryNotes().firstOrNull() ?: ""
        } catch (e: Throwable) {
            ""
        }

        val apiKey = try {
            userPreferencesRepository.getGeminiApiKey().firstOrNull() ?: ""
        } catch (e: Throwable) {
            ""
        }

        val useGemini = try {
            userPreferencesRepository.getUseGemini().firstOrNull() ?: true
        } catch (e: Throwable) {
            true
        }

        val consumedCalories = mealsToday.sumOf { it.calories }
        val consumedProtein = mealsToday.sumOf { it.protein.toDouble() }.toFloat()
        val remainingCalories = maxOf(0, targets.calories - consumedCalories)
        val remainingProtein = maxOf(0f, targets.protein - consumedProtein)

        val hasBreakfast = mealsToday.any { it.category == MealCategory.BREAKFAST }
        val hasLunch = mealsToday.any { it.category == MealCategory.LUNCH }
        val hasSnack = mealsToday.any { it.category == MealCategory.SNACKS }
        val hasDinner = mealsToday.any { it.category == MealCategory.DINNER }

        // 1. Try Online Gemini AI Generation (2.5s timeout for fast notification responsiveness)
        if (useGemini && apiKey.isNotBlank() && isNetworkAvailable()) {
            val aiAdvice = withTimeoutOrNull(2500L) {
                tryGenerateGeminiAdvice(
                    aiName = aiName,
                    mealType = mealType,
                    targets = targets,
                    consumedCalories = consumedCalories,
                    remainingCalories = remainingCalories,
                    remainingProtein = remainingProtein,
                    mealsToday = mealsToday,
                    dietaryNotes = dietaryNotes,
                    apiKey = apiKey
                )
            }
            if (aiAdvice != null) {
                Log.d(TAG, "Generated advice using Gemini AI: ${aiAdvice.title}")
                return@withContext aiAdvice
            }
        }

        // 2. Fallback to Local Smart Well-Wisher Engine (Instant, robust, and calibrated)
        Log.d(TAG, "Falling back to local smart well-wisher engine for slot $mealType")
        return@withContext generateLocalWellWisherAdvice(
            aiName = aiName,
            mealType = mealType,
            targets = targets,
            consumedCalories = consumedCalories,
            consumedProtein = consumedProtein,
            remainingCalories = remainingCalories,
            remainingProtein = remainingProtein,
            hasBreakfast = hasBreakfast,
            hasLunch = hasLunch,
            hasSnack = hasSnack,
            hasDinner = hasDinner,
            mealsToday = mealsToday,
            dietaryNotes = dietaryNotes
        )
    }

    private suspend fun tryGenerateGeminiAdvice(
        aiName: String,
        mealType: String,
        targets: UserTargets,
        consumedCalories: Int,
        remainingCalories: Int,
        remainingProtein: Float,
        mealsToday: List<MealEntry>,
        dietaryNotes: String,
        apiKey: String
    ): WellWisherAdvice? {
        return try {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            val prompt = """
                You are $aiName, a warm, caring, intelligent nutritionist and personal well-wisher friend for MacroBite app.
                USER CONTEXT:
                - Current Time: $hour:00 (Slot: $mealType)
                - Target Goal: ${targets.calories} kcal, ${targets.protein}g protein
                - Consumed Today: $consumedCalories kcal (${remainingCalories} kcal deficit left, ${remainingProtein.roundToInt()}g protein left)
                - Total meals logged today: ${mealsToday.size}
                - Recent meals today: ${if (mealsToday.isEmpty()) "None logged yet" else mealsToday.joinToString { it.foodName }}
                - Dietary / Availability Notes: ${dietaryNotes.ifBlank { "Hostel / student / budget-conscious, everyday foods" }}

                GOAL:
                Create a supportive, compassionate status bar notification.
                - If the user hasn't logged this meal or has a deficit:
                  Act like a true well-wisher. If they forgot or are facing trouble eating / busy, reassure them warmly and recommend 1-2 realistic, easily accessible replacement foods (e.g. whole milk & peanut butter shake, boiled eggs, paneer roll, curd rice, banana) to hit calories effortlessly without stress.
                - If they already logged or are on track:
                  Give warm praise and positive encouragement.
                - CONSTRAINTS:
                  Title: max 5-6 words with 1 emoji.
                  Body: max 25-28 words. Highly practical and motivating.
                Format EXACTLY as:
                TITLE: <short title>
                BODY: <caring body text>
                REPLACEMENT: <short food recommendation with kcal or none>
            """.trimIndent()

            val generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey
            )
            val response = generativeModel.generateContent(prompt)
            val text = response.text ?: return null

            var title: String? = null
            var body: String? = null
            var replacement: String? = null

            text.lines().forEach { line ->
                val trimmed = line.trim()
                when {
                    trimmed.startsWith("TITLE:", ignoreCase = true) ->
                        title = trimmed.substringAfter(":").trim()
                    trimmed.startsWith("BODY:", ignoreCase = true) ->
                        body = trimmed.substringAfter(":").trim()
                    trimmed.startsWith("REPLACEMENT:", ignoreCase = true) ->
                        replacement = trimmed.substringAfter(":").trim().takeIf { !it.equals("none", true) }
                }
            }

            if (!title.isNullOrBlank() && !body.isNullOrBlank()) {
                WellWisherAdvice(
                    title = title!!,
                    body = body!!,
                    recommendedActionFood = replacement,
                    remainingCalories = remainingCalories,
                    isCongratulatory = remainingCalories <= 150
                )
            } else {
                null
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Gemini notification generation failed", e)
            null
        }
    }

    private fun generateLocalWellWisherAdvice(
        aiName: String,
        mealType: String,
        targets: UserTargets,
        consumedCalories: Int,
        consumedProtein: Float,
        remainingCalories: Int,
        remainingProtein: Float,
        hasBreakfast: Boolean,
        hasLunch: Boolean,
        hasSnack: Boolean,
        hasDinner: Boolean,
        mealsToday: List<MealEntry>,
        dietaryNotes: String
    ): WellWisherAdvice {
        val lowerNotes = dietaryNotes.lowercase()
        val isVeg = lowerNotes.contains("veg") && !lowerNotes.contains("non-veg")

        val breakfastReplacement = if (isVeg) "2 PB toasts + banana (+380 kcal)" else "3 boiled eggs + banana (+280 kcal, 18g P)"
        val lunchReplacement = if (isVeg) "paneer roll or curd rice (+450 kcal)" else "double egg/chicken roll or sandwich (+450 kcal)"
        val snackReplacement = "1 glass milk + 2 tbsp peanut butter + banana (+600 kcal)"
        val dinnerReplacement = if (isVeg) "2 rotis with paneer/dal (+450 kcal)" else "rotis with chicken/paneer (+450 kcal)"
        val bedtimeReplacement = "warm milk, honey & almonds (+280 kcal)"

        return when (mealType) {
            NotificationPreferences.MEAL_BREAKFAST -> {
                if (hasBreakfast) {
                    WellWisherAdvice(
                        title = "$aiName: Morning Fuel Logged! ☀️",
                        body = "Breakfast is tracked ($consumedCalories / ${targets.calories} kcal). Great energy momentum today!",
                        remainingCalories = remainingCalories,
                        isCongratulatory = true
                    )
                } else {
                    WellWisherAdvice(
                        title = "$aiName: Missed Breakfast? 🍳",
                        body = "Don't skip morning fuel! If running late or rushed, grab $breakfastReplacement to power up without cooking.",
                        recommendedActionFood = breakfastReplacement,
                        remainingCalories = remainingCalories
                    )
                }
            }

            NotificationPreferences.MEAL_LUNCH -> {
                if (hasLunch) {
                    WellWisherAdvice(
                        title = "$aiName: Midday Target On Track 🍛",
                        body = "Lunch recorded ($consumedCalories / ${targets.calories} kcal, $remainingCalories kcal left). Stay hydrated this afternoon!",
                        remainingCalories = remainingCalories,
                        isCongratulatory = true
                    )
                } else if (mealsToday.isEmpty()) {
                    WellWisherAdvice(
                        title = "$aiName: Midday Check-In 💛",
                        body = "Haven't logged any meals yet today (${targets.calories} kcal goal)? If busy or mess food is tough, grab $lunchReplacement to stay fueled.",
                        recommendedActionFood = lunchReplacement,
                        remainingCalories = remainingCalories
                    )
                } else {
                    WellWisherAdvice(
                        title = "$aiName: Time For Lunch 🍛",
                        body = "You have $remainingCalories kcal remaining today. Grab a solid meal or $lunchReplacement to maintain your energy surge.",
                        recommendedActionFood = lunchReplacement,
                        remainingCalories = remainingCalories
                    )
                }
            }

            NotificationPreferences.MEAL_SNACK -> {
                if (hasSnack) {
                    WellWisherAdvice(
                        title = "$aiName: Snack Tracked! ⚡",
                        body = "Snack recorded ($consumedCalories / ${targets.calories} kcal). Energy levels on track!",
                        remainingCalories = remainingCalories,
                        isCongratulatory = true
                    )
                } else if (remainingCalories >= 900) {
                    WellWisherAdvice(
                        title = "$aiName: Calorie Boost Needed ⚡",
                        body = "Big calorie gap ($remainingCalories kcal left)! Easy replacement: $snackReplacement will easily close the deficit.",
                        recommendedActionFood = snackReplacement,
                        remainingCalories = remainingCalories
                    )
                } else if (remainingCalories in 450..899) {
                    WellWisherAdvice(
                        title = "$aiName: Afternoon Fuel Break 🥜",
                        body = "You have $remainingCalories kcal remaining. A handful of nuts with fruit or a protein shake bridges the gap effortlessly.",
                        recommendedActionFood = "Nuts & Fruit (+300 kcal)",
                        remainingCalories = remainingCalories
                    )
                } else {
                    WellWisherAdvice(
                        title = "$aiName: Almost at Daily Goal! 🎯",
                        body = "Just $remainingCalories kcal away from your daily target. A light snack or tea with roasted chana will finish it off!",
                        recommendedActionFood = "Light Snack (+150 kcal)",
                        remainingCalories = remainingCalories,
                        isCongratulatory = true
                    )
                }
            }

            NotificationPreferences.MEAL_DINNER -> {
                if (hasDinner) {
                    WellWisherAdvice(
                        title = "$aiName: Dinner Tracked! 🍽️",
                        body = "Dinner recorded ($consumedCalories / ${targets.calories} kcal). You've put in great discipline today!",
                        remainingCalories = remainingCalories,
                        isCongratulatory = true
                    )
                } else if (remainingCalories >= 600) {
                    val proteinNote = if (remainingProtein > 30f) " and ${remainingProtein.roundToInt()}g protein" else ""
                    WellWisherAdvice(
                        title = "$aiName: Evening Calorie Deficit 🌙",
                        body = "You're $remainingCalories kcal$proteinNote away from your goal. If eating feels heavy tonight, have $dinnerReplacement so you don't sleep under-fueled.",
                        recommendedActionFood = dinnerReplacement,
                        remainingCalories = remainingCalories
                    )
                } else {
                    WellWisherAdvice(
                        title = "$aiName: Finish Strong Tonight! 🍽️",
                        body = "Only $remainingCalories kcal left to reach today's target. Log your dinner or a simple home-style meal to wrap up the day.",
                        recommendedActionFood = dinnerReplacement,
                        remainingCalories = remainingCalories
                    )
                }
            }

            NotificationPreferences.SUMMARY_DAILY -> {
                if (remainingCalories >= 600) {
                    WellWisherAdvice(
                        title = "$aiName: Bedtime Well-Wisher 💛",
                        body = "Ending the day with $remainingCalories kcal deficit. Facing trouble eating? Even $bedtimeReplacement aids overnight recovery.",
                        recommendedActionFood = bedtimeReplacement,
                        remainingCalories = remainingCalories
                    )
                } else if (remainingCalories in 1..599) {
                    val percent = ((consumedCalories.toDouble() / targets.calories.toDouble()) * 100).roundToInt()
                    WellWisherAdvice(
                        title = "$aiName: Daily Review: Solid Effort! 🌟",
                        body = "You reached $consumedCalories / ${targets.calories} kcal today ($percent% of target). Great consistency tracking your food. Rest up!",
                        remainingCalories = remainingCalories,
                        isCongratulatory = true
                    )
                } else {
                    WellWisherAdvice(
                        title = "$aiName: 100% Target Crushed! 🏆",
                        body = "Magnificent discipline! You fully reached your ${targets.calories} kcal goal today. Sleep well and recover strong!",
                        remainingCalories = remainingCalories,
                        isCongratulatory = true
                    )
                }
            }

            else -> {
                WellWisherAdvice(
                    title = "$aiName: Nutrition Reminder",
                    body = "Don't forget to track your nutrition today ($remainingCalories kcal left).",
                    remainingCalories = remainingCalories
                )
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            false
        }
    }
}
