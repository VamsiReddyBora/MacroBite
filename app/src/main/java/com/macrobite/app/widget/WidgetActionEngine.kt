package com.macrobite.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews
import com.macrobite.app.R
import com.macrobite.app.domain.model.AppDate
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealEntry
import com.macrobite.app.domain.repository.ChatRepository
import com.macrobite.app.domain.repository.MealRepository
import com.macrobite.app.domain.repository.UserPreferencesRepository
import com.macrobite.app.domain.usecase.ParseFoodUseCase
import com.macrobite.app.ui.chat.ChatFoodPayload
import com.macrobite.app.ui.chat.ChatMessage
import com.macrobite.app.ui.chat.jarvis.ContactResolver
import com.macrobite.app.ui.chat.jarvis.JarvisActionDispatcher
import com.macrobite.app.ui.chat.jarvis.JarvisDispatchResult
import com.macrobite.app.ui.chat.jarvis.LocalJarvisActionParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull

/**
 * Supercharged unified action engine for MacroBite Home Screen Widget.
 * Handles assistant device tasks (Calling, Timers, Alarms, Apps, Flashlight, etc.)
 * AND instant food logging AND AI chat mirroring simultaneously.
 */
object WidgetActionEngine {

    suspend fun processInput(
        context: Context,
        rawInput: String,
        parseFoodUseCase: ParseFoodUseCase,
        mealRepository: MealRepository,
        chatRepository: ChatRepository,
        userPreferencesRepository: UserPreferencesRepository
    ) {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val componentName = ComponentName(context, MacroBiteChatWidgetProvider::class.java)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)

        fun updateWidget(text: String) {
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                val rv = RemoteViews(context.packageName, R.layout.widget_chat_bar)
                rv.setTextViewText(R.id.widget_placeholder_text, text)
                appWidgetManager.partiallyUpdateAppWidget(appWidgetIds, rv)
            }
        }

        // 1. Check for Alias Command (e.g. "set alias dad = john doe")
        val aliasCmd = LocalJarvisActionParser.parseAliasCommand(trimmed)
        if (aliasCmd != null) {
            userPreferencesRepository.setContactAlias(aliasCmd.alias, aliasCmd.target)
            val msg = "Saved alias: ${aliasCmd.alias} -> ${aliasCmd.target} ✓"
            updateWidget(msg)
            chatRepository.saveMessage(
                AppDate.todayIso(),
                ChatMessage(
                    text = "Saved alias: \"${aliasCmd.alias}\" mapped to \"${aliasCmd.target}\".",
                    isUser = false,
                    timestamp = System.currentTimeMillis()
                )
            )
            delay(4000)
            MacroBiteChatWidgetProvider.updateAllWidgets(context)
            return
        }

        // 2. Check for Device / Assistant Actions (Call, Timer, Alarm, Torch, YouTube, Apps, Maps, etc.)
        val localAction = LocalJarvisActionParser.parse(trimmed)
        if (localAction != null) {
            var actionToDispatch = localAction
            // Resolve contact name with custom aliases if it's a CALL
            if (localAction.actionType == com.macrobite.app.domain.model.JarvisActionType.CALL && !localAction.contactName.isNullOrBlank()) {
                val customAliases = userPreferencesRepository.getContactAliases().firstOrNull() ?: emptyMap()
                val targetName = localAction.contactName.lowercase().trim()
                val resolvedName = customAliases[targetName] ?: localAction.contactName
                val matched = ContactResolver.findContactPhoneNumber(context, resolvedName)
                if (matched != null) {
                    actionToDispatch = localAction.copy(
                        contactName = matched.displayName,
                        phoneNumber = matched.phoneNumber
                    )
                }
            }

            val dispatchResult = JarvisActionDispatcher.dispatch(context, actionToDispatch)
            val resultMessage = when (dispatchResult) {
                is JarvisDispatchResult.Success -> dispatchResult.message
                is JarvisDispatchResult.Info -> dispatchResult.message
                is JarvisDispatchResult.Error -> dispatchResult.message
            }

            updateWidget(resultMessage)

            // Mirror into AI Chat
            chatRepository.saveMessage(
                AppDate.todayIso(),
                ChatMessage(
                    text = resultMessage,
                    isUser = false,
                    timestamp = System.currentTimeMillis(),
                    actionPayload = actionToDispatch
                )
            )

            delay(4500)
            MacroBiteChatWidgetProvider.updateAllWidgets(context)
            return
        }

        // 3. Not an action: Parse as Food Log
        try {
            val parsedResult = parseFoodUseCase.parseText(trimmed)
            if (parsedResult.items.isNotEmpty()) {
                val firstItem = parsedResult.items.first()
                val mealEntry = MealEntry(
                    date = AppDate.todayIso(),
                    category = MealCategory.SNACKS,
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
                mealRepository.insertMeal(mealEntry)

                // Mirror into AI Chat
                val payload = ChatFoodPayload(
                    foodName = mealEntry.foodName,
                    portion = mealEntry.portion,
                    category = mealEntry.category,
                    calories = mealEntry.calories,
                    protein = mealEntry.protein,
                    carbs = mealEntry.carbs,
                    fats = mealEntry.fats,
                    fiber = mealEntry.fiber,
                    sugar = mealEntry.sugar,
                    sodium = mealEntry.sodium
                )
                chatRepository.saveMessage(
                    mealEntry.date,
                    ChatMessage(
                        text = "Logged ${mealEntry.foodName} (${mealEntry.portion}) to ${mealEntry.category.displayName}!",
                        isUser = false,
                        timestamp = System.currentTimeMillis(),
                        foodPayload = payload,
                        isLogged = true
                    )
                )

                updateWidget("Logged: ${firstItem.name} (${firstItem.calories} kcal) ✓")
                delay(4000)
                MacroBiteChatWidgetProvider.updateAllWidgets(context)
                return
            }
        } catch (_: Throwable) {}

        // 4. If neither action nor food item: Show acknowledgment & mirror query to Chat
        updateWidget("Received: $trimmed ✓")
        chatRepository.saveMessage(
            AppDate.todayIso(),
            ChatMessage(
                text = trimmed,
                isUser = true,
                timestamp = System.currentTimeMillis()
            )
        )
        delay(3500)
        MacroBiteChatWidgetProvider.updateAllWidgets(context)
    }
}
