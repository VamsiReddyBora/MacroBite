package com.macrobite.app.ui.theme

import androidx.compose.ui.graphics.Color

data class ResolvedThemeColor(
    val id: String,
    val title: String,
    val primaryColor: Color,
    val onPrimaryColor: Color = Color.White,
    val isCustom: Boolean = false
)

enum class AppThemePreset(
    val id: String,
    val title: String,
    val primaryColor: Color,
    val onPrimaryColor: Color = Color.White,
    val isDefault: Boolean = false
) {
    AMBER(
        id = "amber",
        title = "Amber Gold",
        primaryColor = Color(0xFFF59E0B),
        onPrimaryColor = Color.Black,
        isDefault = true
    ),
    EMERALD(
        id = "emerald",
        title = "Emerald Forest",
        primaryColor = Color(0xFF10B981),
        onPrimaryColor = Color.White
    ),
    COBALT(
        id = "cobalt",
        title = "Ocean Cobalt",
        primaryColor = Color(0xFF3B82F6),
        onPrimaryColor = Color.White
    ),
    CRIMSON(
        id = "crimson",
        title = "Crimson Rose",
        primaryColor = Color(0xFFE11D48),
        onPrimaryColor = Color.White
    ),
    AMETHYST(
        id = "amethyst",
        title = "Royal Amethyst",
        primaryColor = Color(0xFF8B5CF6),
        onPrimaryColor = Color.White
    ),
    SLATE(
        id = "slate",
        title = "Titanium Slate",
        primaryColor = Color(0xFF64748B),
        onPrimaryColor = Color.White
    );

    companion object {
        fun fromId(id: String): AppThemePreset {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: AMBER
        }

        fun resolveTheme(themeId: String): ResolvedThemeColor {
            val preset = entries.firstOrNull { it.id.equals(themeId, ignoreCase = true) }
            if (preset != null) {
                return ResolvedThemeColor(
                    id = preset.id,
                    title = preset.title,
                    primaryColor = preset.primaryColor,
                    onPrimaryColor = preset.onPrimaryColor,
                    isCustom = false
                )
            }

            // Try parsing as hex custom color string (e.g. "#FF5722" or "FF5722")
            try {
                val clean = themeId.trim().removePrefix("#").removePrefix("0x")
                if (clean.length == 6 || clean.length == 8) {
                    val parsedInt = android.graphics.Color.parseColor("#$clean")
                    val composeColor = Color(parsedInt)
                    val isLight = (0.299 * composeColor.red + 0.587 * composeColor.green + 0.114 * composeColor.blue) > 0.55
                    val onPrimary = if (isLight) Color.Black else Color.White
                    return ResolvedThemeColor(
                        id = "#$clean",
                        title = "Custom (#${clean.uppercase()})",
                        primaryColor = composeColor,
                        onPrimaryColor = onPrimary,
                        isCustom = true
                    )
                }
            } catch (_: Throwable) {}

            return ResolvedThemeColor(
                id = AMBER.id,
                title = AMBER.title,
                primaryColor = AMBER.primaryColor,
                onPrimaryColor = AMBER.onPrimaryColor,
                isCustom = false
            )
        }
    }
}
