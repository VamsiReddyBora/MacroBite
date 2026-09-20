package com.macrobite.app.data.parser

data class ResolvedPayload(
    val foodName: String,
    val portion: String,
    val fullQuery: String
)

object FoodPayloadHelper {
    private val portionRegex = Regex(
        """\b\d+(\.\d+)?\s*(g|gm|gms|gram|grams|ml|l|liter|liters|kg|oz|lbs?)(\s*(pack|packet|packs|packets|cup|bowl|scoop|serving|plate|piece|pcs|roti|slice))?\b|\b\d+(\.\d+)?\s*(pack|packet|packs|packets|scoop|scoops|serving|servings|cup|cups|bowl|bowls|katori|katoris|plate|plates|piece|pieces|pcs?|roti|rotis|slice|slices)\b""",
        RegexOption.IGNORE_CASE
    )

    fun resolve(rawName: String, rawPortion: String): ResolvedPayload {
        val cleanName = rawName.trim()
        val cleanPortion = rawPortion.trim()

        val weightInName = portionRegex.find(cleanName)?.value

        return if (weightInName != null) {
            // The user explicitly embedded the weight or portion in the food name (e.g. "Haldirams matar 20g pack")
            // This is the authoritative, newest portion specification.
            val effectivePortion = if (cleanPortion.isBlank() || cleanPortion.equals("1 serving", ignoreCase = true) || !cleanPortion.contains(weightInName, ignoreCase = true)) {
                weightInName
            } else {
                cleanPortion
            }
            ResolvedPayload(
                foodName = cleanName,
                portion = effectivePortion,
                fullQuery = cleanName
            )
        } else if (cleanPortion.isNotBlank() && !cleanPortion.equals("1 serving", ignoreCase = true)) {
            ResolvedPayload(
                foodName = cleanName,
                portion = cleanPortion,
                fullQuery = "$cleanPortion $cleanName"
            )
        } else {
            ResolvedPayload(
                foodName = cleanName,
                portion = cleanPortion.ifBlank { "1 serving" },
                fullQuery = cleanName
            )
        }
    }
}
