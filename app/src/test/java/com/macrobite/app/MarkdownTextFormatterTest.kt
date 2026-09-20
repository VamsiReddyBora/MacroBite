package com.macrobite.app

import com.macrobite.app.ui.chat.MarkdownTextFormatter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTextFormatterTest {

    @Test
    fun testMarkdownRemovesRawAsterisks() {
        val raw = "Here are your ***calories*** for today:\n* **3 Rotis:** ~312 kcal\n**Total:** 727 kcal"
        val formatted = MarkdownTextFormatter.format(raw)
        val text = formatted.text

        assertFalse("Should not contain raw triple asterisks", text.contains("***"))
        assertFalse("Should not contain raw double asterisks", text.contains("**"))
        assertTrue("Should contain cleaned calories text", text.contains("calories"))
        assertTrue("Should contain bullet point", text.contains("•"))
        assertTrue("Should contain 3 Rotis", text.contains("3 Rotis"))
    }
}
