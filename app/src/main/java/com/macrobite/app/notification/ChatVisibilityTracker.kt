package com.macrobite.app.notification

/**
 * Thread-safe global visibility tracker for the Chat screen.
 * Tracks whether the user is actively viewing the Chat screen in the foreground.
 *
 * Used to suppress chat response notifications when the user is actively looking at
 * the chat, while reliably firing notifications when the user navigates away or backgrounds the app.
 */
object ChatVisibilityTracker {
    @Volatile
    private var isAppForeground: Boolean = true

    @Volatile
    private var isChatSelected: Boolean = false

    val isChatScreenVisible: Boolean
        get() = isAppForeground && isChatSelected

    fun setAppForeground(foreground: Boolean) {
        isAppForeground = foreground
    }

    fun setChatSelected(selected: Boolean) {
        isChatSelected = selected
    }
}
