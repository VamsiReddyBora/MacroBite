package com.macrobite.app

import com.macrobite.app.ui.navigation.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ScreenNavigationTest {

    @Test
    fun testScreenObjectsAndItemsAreNotNull() {
        assertNotNull("Dashboard should not be null", Screen.Dashboard)
        assertNotNull("Chat should not be null", Screen.Chat)
        assertNotNull("History should not be null", Screen.History)
        assertNotNull("Settings should not be null", Screen.Settings)

        assertEquals("dashboard", Screen.Dashboard.route)
        assertEquals("chat", Screen.Chat.route)
        assertEquals("history", Screen.History.route)
        assertEquals("settings", Screen.Settings.route)

        val items = Screen.items
        assertNotNull("Screen.items must not be null", items)
        assertEquals(4, items.size)
        for (item in items) {
            assertNotNull("Navigation item in list must not be null", item)
            assertNotNull("Navigation item route must not be null", item.route)
            assertNotNull("Navigation item title must not be null", item.title)
        }
    }
}
