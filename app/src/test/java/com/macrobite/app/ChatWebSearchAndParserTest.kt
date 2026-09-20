package com.macrobite.app

import com.google.gson.Gson
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.ui.chat.ChatResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatWebSearchAndParserTest {

    private val gson = Gson()

    @Test
    fun testNonFoodWebSearchResponse() {
        val rawResponse = """
            Here are the latest Tollywood movies released recently in theaters:
            1. **Game Changer** - Starring Ram Charan [1].
            2. **Devara: Part 1** - Action drama starring Jr NTR 【2†source】.
            3. **Pushpa 2: The Rule** - Highly anticipated sequel [cite: 3].
            
            Enjoy the movies!
        """.trimIndent()

        val parsed = ChatResponseParser.parse(rawResponse, gson, isWebSearch = true)

        assertTrue(parsed.isWebSearch)
        assertFalse(parsed.isUser)
        assertNull(parsed.foodPayload)
        assertFalse(parsed.text.contains("【2†source】"))
        assertFalse(parsed.text.contains("[cite: 3]"))
        assertTrue(parsed.text.contains("Game Changer"))
        assertTrue(parsed.text.contains("Devara: Part 1"))
    }

    @Test
    fun testWebSearchCitationSanitizationInFoodJson() {
        val rawResponse = """
            I searched the live web for Haldiram's Bhujia 50g pack nutrition! [1]
            Here is your exact verified breakdown:

            ```json
            {
              "has_food": true,
              "food_name": "Haldiram's Bhujia [1]",
              "portion": "50g pack [2]",
              "category": "SNACKS",
              "calories": 285 [1],
              "protein": 6.5 [1, 2],
              "carbs": 21.0,
              "fats": 19.5,
              "fiber": 2.5,
              "sugar": 1.0,
              "sodium": 390.0,
              "vitamins": "Iron [1]"
            }
            ```
        """.trimIndent()

        val parsed = ChatResponseParser.parse(rawResponse, gson, isWebSearch = true)

        assertTrue(parsed.isWebSearch)
        assertNotNull(parsed.foodPayload)

        val payload = parsed.foodPayload!!
        assertEquals("Haldiram's Bhujia", payload.foodName)
        assertEquals("50g pack", payload.portion)
        assertEquals(MealCategory.SNACKS, payload.category)
        assertEquals(285, payload.calories)
        assertEquals(6.5f, payload.protein, 0.01f)
        assertEquals(21.0f, payload.carbs, 0.01f)
        assertEquals(19.5f, payload.fats, 0.01f)
        assertEquals(2.5f, payload.fiber, 0.01f)
        assertEquals("Iron", payload.vitaminsAndMinerals)

        // Ensure conversational text is clean and doesn't leak raw JSON
        assertFalse(parsed.text.contains("```json"))
        assertTrue(parsed.text.contains("Haldiram's Bhujia"))
    }

    @Test
    fun testFallbackRegexExtractorWhenJsonMalformedByCitations() {
        val malformedJson = """
            {
              "has_food": true,
              "food_name": "Subway Paneer Tikka Sub [1]",
              "portion": "6-inch sub [2]",
              "category": "LUNCH",
              "calories": 480 [1],
              "protein": 19.0 [cite: 1],
              "carbs": 54.0,
              "fats": 16.0,
              "fiber": 5.0,
              "sugar": 6.0,
              "sodium": 650.0
            }
        """.trimIndent()

        val fallbackPayload = ChatResponseParser.extractFoodPayloadFallback(ChatResponseParser.sanitizeJsonBlock(malformedJson))

        assertNotNull(fallbackPayload)
        assertEquals("Subway Paneer Tikka Sub", fallbackPayload?.foodName)
        assertEquals("6-inch sub", fallbackPayload?.portion)
        assertEquals(MealCategory.LUNCH, fallbackPayload?.category)
        assertEquals(480, fallbackPayload?.calories)
        assertEquals(19.0f, fallbackPayload?.protein ?: 0f, 0.01f)
        assertEquals(54.0f, fallbackPayload?.carbs ?: 0f, 0.01f)
        assertEquals(16.0f, fallbackPayload?.fats ?: 0f, 0.01f)
    }

    @Test
    fun testStandardPretrainedModelWithoutWebSearchFlag() {
        val rawResponse = """
            Here is your protein shake calculation:
            ```json
            {
              "has_food": true,
              "food_name": "Whey Protein Shake",
              "portion": "1 scoop in water",
              "category": "SNACKS",
              "calories": 125,
              "protein": 24.5,
              "carbs": 2.0,
              "fats": 1.5
            }
            ```
        """.trimIndent()

        val parsed = ChatResponseParser.parse(rawResponse, gson, isWebSearch = false)

        assertFalse(parsed.isWebSearch)
        assertNotNull(parsed.foodPayload)
        assertEquals(125, parsed.foodPayload?.calories)
        assertEquals(24.5f, parsed.foodPayload?.protein ?: 0f, 0.01f)
    }

    @Test
    fun testPunctuationSpacingsCleanedInConversationalText() {
        val dirtyText = "This information is verified [1] . Another source is here [2, 3] !"
        val cleaned = ChatResponseParser.sanitizeConversationalText(dirtyText)
        assertEquals("This information is verified. Another source is here!", cleaned)
    }
}
