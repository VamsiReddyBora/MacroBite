package com.macrobite.app

import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.ui.chat.ChatFoodPayload
import com.macrobite.app.ui.chat.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatUiTest {

    @Test
    fun testChatMessageCreationAndState() {
        val greeting = ChatMessage(
            id = "initial-greeting",
            text = "Hello! I'm your MacroBite AI Nutritionist.",
            isUser = false
        )

        assertEquals("initial-greeting", greeting.id)
        assertFalse(greeting.isUser)
        assertFalse(greeting.isLogged)
        assertNotNull(greeting.text)

        val userMsg = ChatMessage(
            text = "3 rotis with paneer curry",
            isUser = true
        )
        assertTrue(userMsg.isUser)
        assertFalse(userMsg.isLogged)

        val foodPayload = ChatFoodPayload(
            foodName = "Paneer Curry with Rotis",
            portion = "3 rotis + 1 bowl paneer",
            category = MealCategory.LUNCH,
            calories = 580,
            protein = 22.5f,
            carbs = 68.0f,
            fats = 18.0f
        )

        val aiFoodMsg = ChatMessage(
            text = "Here is the nutrition breakdown:",
            isUser = false,
            foodPayload = foodPayload,
            isLogged = false
        )

        assertNotNull(aiFoodMsg.foodPayload)
        assertEquals(580, aiFoodMsg.foodPayload?.calories)
        assertEquals(22.5f, aiFoodMsg.foodPayload?.protein ?: 0f, 0.01f)
        assertEquals(MealCategory.LUNCH, aiFoodMsg.foodPayload?.category)

        val loggedMsg = aiFoodMsg.copy(isLogged = true)
        assertTrue(loggedMsg.isLogged)
    }
}
