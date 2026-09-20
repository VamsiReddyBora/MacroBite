package com.macrobite.app

import com.macrobite.app.ui.chat.availableAvatars
import com.macrobite.app.ui.chat.availableChatModels
import com.macrobite.app.ui.chat.getAvatarDrawable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RaayaProfileTest {

    @Test
    fun testAvailableChatModelsListHasCleanNames() {
        // Verify models list includes flash-lite 3.5, 3.1, flash 3.5, 3.6, flash 2.5, flash-lite latest
        val modelIds = availableChatModels.map { it.id }
        assertTrue(modelIds.contains("gemini-3.5-flash-lite"))
        assertTrue(modelIds.contains("gemini-3.1-flash-lite"))
        assertTrue(modelIds.contains("gemini-3.5-flash"))
        assertTrue(modelIds.contains("gemini-3.6-flash"))
        assertTrue(modelIds.contains("gemini-2.5-flash"))
        assertTrue(modelIds.contains("gemini-flash-lite-latest"))

        // Verify clean model names without preview tags or badges
        availableChatModels.forEach { model ->
            assertFalse(model.name.contains("Preview", ignoreCase = true))
            assertFalse(model.name.contains("Recommended", ignoreCase = true))
            assertTrue(model.name.isNotBlank())
        }
    }

    @Test
    fun testAvailableAvatarsAndDrawables() {
        val avatarIds = availableAvatars.map { it.id }
        assertTrue(avatarIds.contains("default"))
        assertTrue(avatarIds.contains("chef"))
        assertTrue(avatarIds.contains("eureka"))
        assertTrue(avatarIds.contains("think"))
        assertTrue(avatarIds.contains("calc"))
        assertTrue(avatarIds.contains("work"))

        // Verify getAvatarDrawable maps correctly
        assertEquals(R.drawable.ic_default_profile, getAvatarDrawable("default"))
        assertEquals(R.drawable.img_raaya_chef, getAvatarDrawable("chef"))
        assertEquals(R.drawable.img_tom_jerry_eureka, getAvatarDrawable("eureka"))
        assertEquals(R.drawable.img_tom_jerry_think, getAvatarDrawable("think"))
        assertEquals(R.drawable.img_tom_jerry_calc, getAvatarDrawable("calc"))
        assertEquals(R.drawable.img_tom_jerry_work, getAvatarDrawable("work"))
        // Fallback
        assertEquals(R.drawable.ic_default_profile, getAvatarDrawable("unknown_id"))
    }
}
