package com.macrobite.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.macrobite.app.R

sealed class Screen(
    val route: String,
    val title: String,
    val iconRes: Int? = null,
    val iconVector: ImageVector? = null
) {
    data object Dashboard : Screen(
        route = "dashboard",
        title = "Log",
        iconRes = R.drawable.ic_nav_restaurant
    )

    data object Chat : Screen(
        route = "chat",
        title = "Chat",
        iconRes = R.drawable.ic_nav_chat
    )

    data object History : Screen(
        route = "history",
        title = "History",
        iconRes = R.drawable.ic_nav_history
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        iconVector = Icons.Default.Settings
    )

    companion object {
        // Use getter property so items are evaluated dynamically after subclasses are initialized
        val items: List<Screen>
            get() = listOf(Dashboard, Chat, History, Settings)
    }
}
