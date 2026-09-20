package com.macrobite.app.data.scanner

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

data class ScannedFoodResult(
    val barcode: String,
    val name: String,
    val brand: String? = null,
    val servingSize: String = "1 serving",
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fats: Float,
    val fiber: Float? = null,
    val sugar: Float? = null,
    val sodium: Float? = null,
    val imageUrl: String? = null
)

object BarcodeNutritionResolver {

    private val gson = Gson()

    /**
     * Resolves nutritional information for a given barcode using the open-access Open Food Facts API.
     */
    suspend fun resolveBarcode(barcode: String): ScannedFoodResult? = withContext(Dispatchers.IO) {
        val cleanCode = barcode.trim()
        if (cleanCode.isBlank()) return@withContext null

        try {
            val endpoint = "https://world.openfoodfacts.org/api/v2/product/$cleanCode.json"
            val url = URL(endpoint)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.setRequestProperty("User-Agent", "MacroBite - Android Nutrition Tracker")

            if (conn.responseCode == 200) {
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                val root = gson.fromJson(jsonStr, JsonObject::class.java)

                val status = root.get("status")?.asInt ?: 0
                if (status == 1 && root.has("product")) {
                    val product = root.getAsJsonObject("product")
                    val name = product.get("product_name")?.asString
                        ?: product.get("product_name_en")?.asString
                        ?: "Food Product ($cleanCode)"

                    val brand = product.get("brands")?.asString?.split(",")?.firstOrNull()?.trim()

                    val servingSize = product.get("serving_size")?.asString ?: "100g"

                    val nutriments = product.getAsJsonObject("nutriments")
                    var kcal = 0
                    var protein = 0f
                    var carbs = 0f
                    var fat = 0f
                    var fiber: Float? = null
                    var sugar: Float? = null
                    var sodium: Float? = null

                    if (nutriments != null) {
                        // Prefer per-serving if available, otherwise per 100g
                        kcal = nutriments.get("energy-kcal_serving")?.asInt
                            ?: nutriments.get("energy-kcal_100g")?.asInt
                            ?: nutriments.get("energy-kcal")?.asInt
                            ?: 0

                        protein = nutriments.get("proteins_serving")?.asFloat
                            ?: nutriments.get("proteins_100g")?.asFloat
                            ?: nutriments.get("proteins")?.asFloat
                            ?: 0f

                        carbs = nutriments.get("carbohydrates_serving")?.asFloat
                            ?: nutriments.get("carbohydrates_100g")?.asFloat
                            ?: nutriments.get("carbohydrates")?.asFloat
                            ?: 0f

                        fat = nutriments.get("fat_serving")?.asFloat
                            ?: nutriments.get("fat_100g")?.asFloat
                            ?: nutriments.get("fat")?.asFloat
                            ?: 0f

                        fiber = nutriments.get("fiber_100g")?.asFloat
                        sugar = nutriments.get("sugars_100g")?.asFloat
                        sodium = nutriments.get("sodium_100g")?.asFloat
                    }

                    val imageUrl = product.get("image_url")?.asString

                    return@withContext ScannedFoodResult(
                        barcode = cleanCode,
                        name = name,
                        brand = brand,
                        servingSize = servingSize,
                        calories = kcal,
                        protein = protein,
                        carbs = carbs,
                        fats = fat,
                        fiber = fiber,
                        sugar = sugar,
                        sodium = sodium,
                        imageUrl = imageUrl
                    )
                }
            }
        } catch (e: Throwable) {
            Log.e("BarcodeResolver", "Error fetching barcode data from OpenFoodFacts", e)
        }

        null
    }
}
