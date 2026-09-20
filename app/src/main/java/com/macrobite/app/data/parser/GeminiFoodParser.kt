package com.macrobite.app.data.parser

import android.graphics.Bitmap
import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealItem
import com.macrobite.app.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class GeminiResponseDto(
    @SerializedName("category") val category: String? = null,
    @SerializedName("food_items") val foodItems: List<GeminiFoodItemDto>? = null
)

data class GeminiFoodItemDto(
    @SerializedName("name") val name: String,
    @SerializedName("portion") val portion: String? = "1 serving",
    @SerializedName("calories") val calories: Int,
    @SerializedName("protein") val protein: Float,
    @SerializedName("carbs") val carbs: Float,
    @SerializedName("fats") val fats: Float,
    @SerializedName("fiber") val fiber: Float? = 0f,
    @SerializedName("sugar") val sugar: Float? = 0f,
    @SerializedName("sodium") val sodium: Float? = 0f,
    @SerializedName("saturated_fat") val saturatedFat: Float? = 0f,
    @SerializedName("potassium") val potassium: Float? = 0f,
    @SerializedName("cholesterol") val cholesterol: Float? = 0f,
    @SerializedName("vitamins_and_minerals") val vitaminsAndMinerals: String? = ""
)

@Singleton
class GeminiFoodParser @Inject constructor(
    private val gson: Gson,
    private val preferencesRepository: UserPreferencesRepository
) {

    private val systemInstruction = """
        You are an expert nutrition and macro tracking assistant focused on high precision, scientific accuracy, and comprehensive nutritional analysis.
        Given a meal text description or an image of food, parse and calculate the exact macronutrients, calories, and extended nutritional values (fiber, sugar, sodium, saturated fat, potassium, cholesterol, and vitamins/minerals).

        CRITICAL ACCURACY RULES:
        - USER QUANTITY & WEIGHT OVERRIDE (HIGHEST PRIORITY): If the user specifies any text, weight, portion, or quantity (e.g. "20g packet", "70 grams", "2 rotis", "150ml", "1 scoop"), you MUST STRICTLY FOLLOW the user's stated quantity, weight, and portion. NEVER guess a different weight (like 13g) if the user specifies "20g packet" or "20g". Calculate all calories, macronutrients, and micronutrients proportionally scaled for that exact stated portion/weight.
        - ZERO CALORIE LIQUIDS: Plain water, ice, black coffee without sugar, plain black/green tea without sugar, and zero-calorie mixing liquids have ZERO (0) calories and ZERO (0) macros. NEVER create a separate food item or add calories for water or plain mixing water.
        - SHAKES & SUPPLEMENT POWDERS: When a user enters powder mixed with water (e.g. "GNC pro weight gainer powder of 70 grams with water" or "1 scoop whey with water"), water is purely the carrier with 0 calories. The single food item is the powder itself (e.g. "GNC Pro Weight Gainer Powder", portion: "70g"), and calculate its exact real-world nutritional profile (e.g. 70g GNC weight gainer = ~265 kcal, 19.0g protein, 44.0g carbs, 2.0g fats).
        - STANDARD VOLUMES:
          * 1 cup is strictly 250ml.
          * 1 bowl / katori is strictly 150ml.
          * If a user mentions cups, bowls, or katoris without specifying exact ml, label portion as "1 cup (assumed 250ml)" or "1 bowl (assumed 150ml)" so the user can verify.
          * If the user specifies exact grams or ml (e.g. "70g", "200ml", "20g"), calculate proportionally with high accuracy based on USDA/IFCT nutritional tables.
        - BRAND & FOOD KNOWLEDGE: Accurately recognize real brand supplements (GNC, Optimum Nutrition, MuscleBlaze, MyProtein, etc.), packaged snacks (e.g. Lay's, Doritos, Kurkure, Haldiram's, protein bars), hostel foods, and regional dishes without guessing random or fabricated numbers.
        - FLOATING POINT PRECISION: Return protein, carbs, fats, fiber, sugar, and saturated_fat as precise decimal numbers with 1 decimal place (e.g., 1.4, 12.5, 0.5, 2.1). Do not round down or truncate to integers. If a food item has 1.4g protein, return 1.4, NOT 1. Calories should remain whole integers (kcal).
        - EXTENDED NUTRITION & MICRONUTRIENTS:
          * "fiber": Dietary fiber in grams (decimal, e.g. 1.5).
          * "sugar": Sugars in grams (decimal, e.g. 2.0).
          * "sodium": Sodium in milligrams (decimal, e.g. 140.0).
          * "saturated_fat": Saturated fats in grams (decimal, e.g. 1.2).
          * "potassium": Potassium in milligrams (decimal, e.g. 210.0).
          * "cholesterol": Cholesterol in milligrams (decimal, e.g. 0.0).
          * "vitamins_and_minerals": Highlight key vitamins and minerals with amounts/units (e.g. "Vitamin C: 12mg, Iron: 1.2mg, Calcium: 40mg, Vitamin A: 80mcg"). If negligible, return "None significant".

        Respond ONLY with a valid JSON object strictly matching this schema with no markdown explanations outside:
        {
          "category": "BREAKFAST" | "LUNCH" | "SNACKS" | "DINNER",
          "food_items": [
            {
              "name": "Food Item Name",
              "portion": "e.g. 20g packet / 70 grams / 1 cup (assumed 250ml)",
              "calories": 105,
              "protein": 1.4,
              "carbs": 12.8,
              "fats": 5.2,
              "fiber": 1.1,
              "sugar": 0.6,
              "sodium": 120.0,
              "saturated_fat": 2.1,
              "potassium": 110.0,
              "cholesterol": 0.0,
              "vitamins_and_minerals": "Vitamin C: 5mg, Iron: 0.6mg, Calcium: 12mg"
            }
          ]
        }
    """.trimIndent()

    private val candidateModels = listOf(
        "gemini-3.5-flash-lite",
        "gemini-3.1-flash-lite",
        "gemini-flash-lite-latest",
        "gemini-3.5-flash",
        "gemini-3.6-flash"
    )

    private suspend fun getOrderedModels(): List<String> {
        val preferred = try {
            preferencesRepository.getGeminiModel().first()
        } catch (e: Exception) {
            "gemini-3.5-flash-lite"
        }
        val safePreferred = if (preferred == "gemini-3.7-flash" || preferred.isBlank()) "gemini-3.5-flash-lite" else preferred
        return if (candidateModels.contains(safePreferred)) {
            listOf(safePreferred) + candidateModels.filter { it != safePreferred }
        } else {
            listOf(safePreferred) + candidateModels
        }
    }

    suspend fun parseText(input: String, apiKey: String): ParseResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is required for online logging. Please configure your API key in Settings.")
        }

        val prompt = """
            $systemInstruction

            USER MEAL INPUT:
            "$input"

            CRITICAL PORTION SCALING INSTRUCTION:
            - The user specified: "$input".
            - If the input specifies an exact weight or portion (such as "20g pack", "20g", "70 grams", "150ml", "2 eggs"), you MUST calculate all calories, macros (protein, carbs, fats), and micronutrients specifically for that EXACT specified portion/weight.
            - DO NOT return generic or old estimates (such as 13g or 1 serving) when the user specifies a quantity like 20g! Scale all nutrition proportionally for "$input".
        """.trimIndent()
        var lastError: Exception? = null
        var responseText: String? = null
        val modelsToTry = getOrderedModels()

        for (modelName in modelsToTry) {
            try {
                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey
                )
                val response = model.generateContent(prompt)
                val text = response.text ?: ""
                if (text.isNotBlank()) {
                    responseText = text
                    recordUsage(promptText = prompt, responseText = text, imageTokens = 0)
                    break
                }
            } catch (e: Exception) {
                lastError = e
                e.printStackTrace()
            }
        }

        if (responseText == null) {
            throw IllegalStateException(
                "Online Gemini analysis failed: ${lastError?.localizedMessage ?: "Unable to connect to Gemini API. Please check your internet connection."}"
            )
        }

        return@withContext parseGeminiJson(responseText, input, "gemini_online")
    }

    private fun scaleDownBitmap(bitmap: Bitmap, maxDim: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDim && height <= maxDim) return bitmap
        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDim
            newHeight = (maxDim / ratio).toInt().coerceAtLeast(1)
        } else {
            newHeight = maxDim
            newWidth = (maxDim * ratio).toInt().coerceAtLeast(1)
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    suspend fun parseImage(bitmap: Bitmap, promptNote: String?, apiKey: String): ParseResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalStateException("Gemini API key is required for photo nutrition estimation. Please enter your API key in Settings.")
        }

        val userPrompt = if (!promptNote.isNullOrBlank()) {
            """
            $systemInstruction

            USER PROVIDED PHOTO WITH EXPLICIT USER INSTRUCTION / SPECIFICATION:
            "$promptNote"

            CRITICAL OVERRIDE INSTRUCTION:
            - The user has explicitly specified: "$promptNote".
            - You MUST calculate the nutritional values for this EXACT user-specified portion/weight ("$promptNote").
            - DO NOT use the default printed net weight on the packaging in the image (e.g. if the packaging in the photo says 13g or 5 rupees, but the user specifies "20g pack" or "20g", you MUST scale all nutritional values to exactly 20g).
            - The user's text specification takes 100% precedence over any visual packaging estimate.
            """.trimIndent()
        } else {
            """
            $systemInstruction

            Analyze this food image and accurately estimate the food items, exact portion sizes, and full macronutrient/micronutrient values.
            """.trimIndent()
        }

        val uploadBitmap = scaleDownBitmap(bitmap)
        var lastError: Exception? = null
        var responseText: String? = null
        val modelsToTry = getOrderedModels()

        for (modelName in modelsToTry) {
            try {
                val model = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey
                )
                val inputContent = content {
                    image(uploadBitmap)
                    text(userPrompt)
                }
                val response = model.generateContent(inputContent)
                val text = response.text ?: ""
                if (text.isNotBlank()) {
                    responseText = text
                    recordUsage(promptText = userPrompt, responseText = text, imageTokens = 258)
                    break
                }
            } catch (e: Exception) {
                lastError = e
                e.printStackTrace()
            }
        }

        if (responseText == null) {
            throw IllegalStateException(
                "Photo analysis failed: ${lastError?.localizedMessage ?: "Unable to analyze photo via Gemini Vision."}"
            )
        }

        return@withContext parseGeminiJson(responseText, promptNote ?: "Food Photo", "gemini_vision")
    }

    private fun parseGeminiJson(rawJson: String, originalInput: String, sourceTag: String): ParseResult {
        var cleaned = rawJson.trim()
        val startIdx = cleaned.indexOf('{')
        val endIdx = cleaned.lastIndexOf('}')
        if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
            cleaned = cleaned.substring(startIdx, endIdx + 1).trim()
        } else if (cleaned.startsWith("```json")) {
            cleaned = cleaned.removePrefix("```json").substringBeforeLast("```").trim()
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.removePrefix("```").substringBeforeLast("```").trim()
        }

        val parsedDto = gson.fromJson(cleaned, GeminiResponseDto::class.java)
        val category = parsedDto.category?.let { MealCategory.fromString(it) } ?: MealCategory.LUNCH
        val items = parsedDto.foodItems?.map { dto ->
            MealItem(
                name = dto.name,
                portion = dto.portion ?: "1 serving",
                calories = dto.calories,
                protein = dto.protein,
                carbs = dto.carbs,
                fats = dto.fats,
                fiber = dto.fiber ?: 0f,
                sugar = dto.sugar ?: 0f,
                sodium = dto.sodium ?: 0f,
                saturatedFat = dto.saturatedFat ?: 0f,
                potassium = dto.potassium ?: 0f,
                cholesterol = dto.cholesterol ?: 0f,
                vitaminsAndMinerals = dto.vitaminsAndMinerals.orEmpty()
            )
        } ?: emptyList()

        if (items.isEmpty()) {
            throw IllegalStateException("Gemini could not identify any food items in: \"$originalInput\". Please check your input.")
        }

        return ParseResult(
            category = category,
            items = items,
            rawText = originalInput,
            source = sourceTag
        )
    }

    private suspend fun recordUsage(promptText: String, responseText: String, imageTokens: Int) {
        try {
            val promptTokens = imageTokens + (promptText.length / 3.8).toInt().coerceAtLeast(1)
            val candidateTokens = (responseText.length / 3.8).toInt().coerceAtLeast(1)
            val totalTokens = promptTokens + candidateTokens
            preferencesRepository.recordApiUsage(promptTokens, candidateTokens, totalTokens)
        } catch (e: Exception) {
            Log.e("GeminiFoodParser", "Failed to record API token usage: ${e.message}")
        }
    }
}
