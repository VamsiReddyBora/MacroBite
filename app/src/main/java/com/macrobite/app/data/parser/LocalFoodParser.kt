package com.macrobite.app.data.parser

import com.macrobite.app.domain.model.MealCategory
import com.macrobite.app.domain.model.MealItem
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

data class ParseResult(
    val category: MealCategory,
    val items: List<MealItem>,
    val rawText: String,
    val source: String // "local" or "gemini"
)

@Singleton
class LocalFoodParser @Inject constructor() {

    fun parse(input: String): ParseResult {
        val trimmed = input.trim()
        val (category, cleanedText) = extractCategory(trimmed)

        // Split by delimiter: comma, +, &, newline, " and ", " with "
        val splitRegex = Regex("[,+&\\n]|\\band\\b|\\bwith\\b", RegexOption.IGNORE_CASE)
        val rawParts = cleanedText.split(splitRegex)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val items = mutableListOf<MealItem>()

        for (part in rawParts) {
            val item = parseSingleFood(part)
            if (item != null) {
                items.add(item)
            }
        }

        // If no items were parsed but input was not empty, create a generic item
        if (items.isEmpty() && cleanedText.isNotEmpty()) {
            items.add(
                MealItem(
                    name = cleanedText.replaceFirstChar { it.uppercase() },
                    portion = "1 serving",
                    calories = 250,
                    protein = 8f,
                    carbs = 35f,
                    fats = 8f
                )
            )
        }

        return ParseResult(
            category = category,
            items = items,
            rawText = input,
            source = "local"
        )
    }

    private fun extractCategory(input: String): Pair<MealCategory, String> {
        val lower = input.lowercase()
        return when {
            lower.startsWith("breakfast:") || lower.startsWith("breakfast -") -> {
                MealCategory.BREAKFAST to input.substringAfter(":").substringAfter("-").trim()
            }
            lower.startsWith("lunch:") || lower.startsWith("lunch -") || lower.startsWith("mess lunch:") -> {
                MealCategory.LUNCH to input.substringAfter(":").substringAfter("-").trim()
            }
            lower.startsWith("snack:") || lower.startsWith("snacks:") || lower.startsWith("evening:") -> {
                MealCategory.SNACKS to input.substringAfter(":").substringAfter("-").trim()
            }
            lower.startsWith("dinner:") || lower.startsWith("dinner -") || lower.startsWith("mess dinner:") -> {
                MealCategory.DINNER to input.substringAfter(":").substringAfter("-").trim()
            }
            else -> {
                // Infer category from current hour
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val inferred = when (hour) {
                    in 5..11 -> MealCategory.BREAKFAST
                    in 12..16 -> MealCategory.LUNCH
                    in 17..19 -> MealCategory.SNACKS
                    else -> MealCategory.DINNER
                }
                inferred to input
            }
        }
    }

    private fun parseSingleFood(phrase: String): MealItem? {
        val lowerPhrase = phrase.lowercase().trim()
        if (lowerPhrase.isEmpty()) return null

        // Check for preset direct match
        val matchedPreset = LocalFoodDatabase.commonPresets.find {
            lowerPhrase.contains(it.name.lowercase())
        }
        if (matchedPreset != null) {
            val matchingDef = LocalFoodDatabase.foodDefinitions.find { def ->
                matchedPreset.name.contains(def.standardName, ignoreCase = true) ||
                    def.aliases.any { matchedPreset.name.contains(it, ignoreCase = true) }
            }
            return MealItem(
                name = matchedPreset.name,
                portion = matchedPreset.portion,
                calories = matchedPreset.calories,
                protein = matchedPreset.protein,
                carbs = matchedPreset.carbs,
                fats = matchedPreset.fats,
                baseMl = matchingDef?.baseMl,
                baseCalories = matchingDef?.baseCalories,
                baseProtein = matchingDef?.baseProtein,
                baseCarbs = matchingDef?.baseCarbs,
                baseFats = matchingDef?.baseFats
            )
        }

        // Sort definitions by alias length descending to match specific phrases first
        val sortedDefs = LocalFoodDatabase.foodDefinitions.flatMap { def ->
            def.aliases.map { alias -> alias to def }
        }.sortedByDescending { it.first.length }

        for ((alias, def) in sortedDefs) {
            val aliasPattern = Regex("\\b${Regex.escape(alias)}\\b")
            if (aliasPattern.containsMatchIn(lowerPhrase)) {
                val mlMatch = Regex("(\\d+)\\s*ml\\b").find(lowerPhrase)
                val gMatch = Regex("(\\d+)\\s*(?:g|grams?)\\b").find(lowerPhrase)

                val (multiplier, portionText) = when {
                    mlMatch != null && def.baseMl != null -> {
                        val specifiedMl = mlMatch.groupValues[1].toFloatOrNull() ?: def.baseMl.toFloat()
                        val mult = (specifiedMl / def.baseMl.toFloat()).coerceAtLeast(0.1f)
                        mult to "${specifiedMl.roundToInt()}ml"
                    }
                    gMatch != null && (def.defaultPortion.contains("g") || def.defaultPortion.contains("raw")) -> {
                        val specifiedG = gMatch.groupValues[1].toFloatOrNull() ?: 100f
                        val baseG = Regex("(\\d+)\\s*g").find(def.defaultPortion)?.groupValues?.get(1)?.toFloatOrNull() ?: 100f
                        val mult = (specifiedG / baseG).coerceAtLeast(0.1f)
                        mult to "${specifiedG.roundToInt()}g"
                    }
                    else -> {
                        val mult = extractQuantityMultiplier(lowerPhrase)
                        val formatted = if (def.baseMl != null) {
                            val totalMl = (mult * def.baseMl).roundToInt()
                            val countStr = if (mult == mult.toInt().toFloat()) "${mult.toInt()}" else "%.1f".format(mult)
                            val unitWord = when {
                                lowerPhrase.contains("bowl") || lowerPhrase.contains("katori") -> if (mult <= 1f) "bowl" else "bowls"
                                lowerPhrase.contains("glass") -> if (mult <= 1f) "glass" else "glasses"
                                else -> if (mult <= 1f) "cup" else "cups"
                            }
                            "$countStr $unitWord (assumed ${totalMl}ml)"
                        } else {
                            formatPortion(def.defaultPortion, mult)
                        }
                        mult to formatted
                    }
                }

                return MealItem(
                    name = def.standardName,
                    portion = portionText,
                    calories = (def.baseCalories * multiplier).roundToInt(),
                    protein = ((def.baseProtein * multiplier) * 10f).roundToInt() / 10f,
                    carbs = ((def.baseCarbs * multiplier) * 10f).roundToInt() / 10f,
                    fats = ((def.baseFats * multiplier) * 10f).roundToInt() / 10f,
                    baseMl = def.baseMl,
                    baseCalories = def.baseCalories,
                    baseProtein = def.baseProtein,
                    baseCarbs = def.baseCarbs,
                    baseFats = def.baseFats
                )
            }
        }

        // Fallback: unrecognized food name in part
        val cleanedName = phrase.replace(Regex("[^a-zA-Z0-9 ]"), " ").trim()
        if (cleanedName.length >= 2) {
            return MealItem(
                name = cleanedName.replaceFirstChar { it.uppercase() },
                portion = "1 serving",
                calories = 200,
                protein = 6f,
                carbs = 26f,
                fats = 7f
            )
        }

        return null
    }

    private fun extractQuantityMultiplier(phrase: String): Float {
        // Fraction check first: 1/2, 1/4, 3/4
        if (phrase.contains("1/2")) return 0.5f
        if (phrase.contains("1/4")) return 0.25f
        if (phrase.contains("3/4")) return 0.75f

        // Look for word numbers or digit numbers
        val numberRegex = Regex("(?:^|\\s)(half|double|triple|single|quarter|one|two|three|four|five|six|\\d+(?:\\.\\d+)?)(?:\\s|$)")
        val numMatch = numberRegex.find(phrase)
        if (numMatch != null) {
            val token = numMatch.groupValues[1].lowercase()
            val parsed = when (token) {
                "half" -> 0.5f
                "quarter" -> 0.25f
                "single", "one" -> 1f
                "double", "two" -> 2f
                "triple", "three" -> 3f
                "four" -> 4f
                "five" -> 5f
                "six" -> 6f
                else -> token.toFloatOrNull() ?: 1f
            }
            if (parsed > 0f) return parsed
        }

        return 1f
    }

    private fun formatPortion(defaultPortion: String, multiplier: Float): String {
        return if (multiplier == 1f) {
            defaultPortion
        } else if (multiplier == multiplier.toInt().toFloat()) {
            val multInt = multiplier.toInt()
            if (defaultPortion.startsWith("1 ")) {
                "$multInt ${defaultPortion.substring(2)}s"
            } else {
                "${multInt}x $defaultPortion"
            }
        } else {
            "%.1fx $defaultPortion".format(multiplier)
        }
    }
}
