package com.macrobite.app

import com.macrobite.app.widget.MacroBiteChatWidgetProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class MacroBiteChatWidgetTest {

    @Test
    fun testThemeColorResolutions() {
        assertEquals(0xFFF59E0B.toInt(), MacroBiteChatWidgetProvider.getThemeColorInt("amber"))
        assertEquals(0xFF10B981.toInt(), MacroBiteChatWidgetProvider.getThemeColorInt("emerald"))
        assertEquals(0xFF3B82F6.toInt(), MacroBiteChatWidgetProvider.getThemeColorInt("cobalt"))
        assertEquals(0xFFE11D48.toInt(), MacroBiteChatWidgetProvider.getThemeColorInt("crimson"))
        assertEquals(0xFF8B5CF6.toInt(), MacroBiteChatWidgetProvider.getThemeColorInt("amethyst"))
        assertEquals(0xFF64748B.toInt(), MacroBiteChatWidgetProvider.getThemeColorInt("slate"))
        // Case insensitivity and default fallback
        assertEquals(0xFF10B981.toInt(), MacroBiteChatWidgetProvider.getThemeColorInt("EMERALD"))
        assertEquals(0xFFF59E0B.toInt(), MacroBiteChatWidgetProvider.getThemeColorInt("unknown_theme"))
    }
}
