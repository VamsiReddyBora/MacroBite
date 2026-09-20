package com.macrobite.app

import com.google.gson.Gson
import com.macrobite.app.domain.model.JarvisActionType
import com.macrobite.app.ui.chat.ChatResponseParser
import com.macrobite.app.ui.chat.jarvis.ContactResolver
import com.macrobite.app.ui.chat.jarvis.ContactSearchResult
import com.macrobite.app.ui.chat.jarvis.LocalJarvisActionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JarvisAssistantTest {

    private val gson = Gson()

    @Test
    fun testParseCallActionJson() {
        val rawResponse = """
            Right away! Calling Mom now.
            ```json
            {
              "has_action": true,
              "action_type": "CALL",
              "contact_name": "Mom",
              "title": "Call Mom"
            }
            ```
        """.trimIndent()

        val parsed = ChatResponseParser.parse(rawResponse, gson, isWebSearch = false)

        assertNull(parsed.foodPayload)
        assertNotNull(parsed.actionPayload)
        assertEquals(JarvisActionType.CALL, parsed.actionPayload?.actionType)
        assertEquals("Mom", parsed.actionPayload?.contactName)
        assertEquals("Call Mom", parsed.actionPayload?.displayTitle)
        assertTrue(parsed.text.contains("Calling Mom now."))
    }

    @Test
    fun testParseWhatsAppActionJson() {
        val rawResponse = """
            Preparing your WhatsApp message for Rahul!
            ```json
            {
              "has_action": true,
              "action_type": "WHATSAPP",
              "contact_name": "Rahul",
              "message": "I will be 10 minutes late"
            }
            ```
        """.trimIndent()

        val parsed = ChatResponseParser.parse(rawResponse, gson, isWebSearch = false)

        assertNotNull(parsed.actionPayload)
        assertEquals(JarvisActionType.WHATSAPP, parsed.actionPayload?.actionType)
        assertEquals("Rahul", parsed.actionPayload?.contactName)
        assertEquals("I will be 10 minutes late", parsed.actionPayload?.message)
        assertEquals("WhatsApp Rahul", parsed.actionPayload?.displayTitle)
    }

    @Test
    fun testDualPayloadBothFoodAndTimerAction() {
        val rawResponse = """
            Here is your hard-boiled egg breakdown, and I also set a 10-minute timer for your eggs!
            ```json
            {
              "has_food": true,
              "food_name": "2 Hard Boiled Eggs",
              "portion": "2 large eggs",
              "category": "BREAKFAST",
              "calories": 140,
              "protein": 12.0,
              "carbs": 1.0,
              "fats": 10.0
            }
            ```
            ```json
            {
              "has_action": true,
              "action_type": "TIMER",
              "timer_seconds": 600,
              "title": "Egg Boiling Timer"
            }
            ```
        """.trimIndent()

        val parsed = ChatResponseParser.parse(rawResponse, gson, isWebSearch = false)

        assertNotNull(parsed.foodPayload)
        assertEquals("2 Hard Boiled Eggs", parsed.foodPayload?.foodName)
        assertEquals(140, parsed.foodPayload?.calories)

        assertNotNull(parsed.actionPayload)
        assertEquals(JarvisActionType.TIMER, parsed.actionPayload?.actionType)
        assertEquals(600, parsed.actionPayload?.timerSeconds)
        assertEquals("Timer for 10 min", parsed.actionPayload?.displayTitle)
    }

    @Test
    fun testLocalJarvisActionParserDirectMatching() {
        val callAction = LocalJarvisActionParser.parse("Call Mom")
        assertNotNull(callAction)
        assertEquals(JarvisActionType.CALL, callAction?.actionType)
        assertEquals("Mom", callAction?.contactName)

        val timerAction = LocalJarvisActionParser.parse("Set timer for 15 minutes")
        assertNotNull(timerAction)
        assertEquals(JarvisActionType.TIMER, timerAction?.actionType)
        assertEquals(900, timerAction?.timerSeconds)

        val alarmAction = LocalJarvisActionParser.parse("Set alarm for 6:30 AM")
        assertNotNull(alarmAction)
        assertEquals(JarvisActionType.ALARM, alarmAction?.actionType)
        assertEquals(6, alarmAction?.timeHour)
        assertEquals(30, alarmAction?.timeMinute)

        val torchAction = LocalJarvisActionParser.parse("Turn on flashlight")
        assertNotNull(torchAction)
        assertEquals(JarvisActionType.FLASHLIGHT, torchAction?.actionType)
        assertEquals(true, torchAction?.turnOn)

        val batteryAction = LocalJarvisActionParser.parse("Check battery status")
        assertNotNull(batteryAction)
        assertEquals(JarvisActionType.BATTERY, batteryAction?.actionType)

        val mapsAction = LocalJarvisActionParser.parse("Directions to nearest gym")
        assertNotNull(mapsAction)
        assertEquals(JarvisActionType.MAPS, mapsAction?.actionType)
        assertEquals("nearest gym", mapsAction?.query)

        val ytAction = LocalJarvisActionParser.parse("Search chest workout on YouTube")
        assertNotNull(ytAction)
        assertEquals(JarvisActionType.YOUTUBE, ytAction?.actionType)
        assertEquals("chest workout", ytAction?.query)

        val waAction = LocalJarvisActionParser.parse("WhatsApp Mom: Coming home now")
        assertNotNull(waAction)
        assertEquals(JarvisActionType.WHATSAPP, waAction?.actionType)
        assertEquals("Mom", waAction?.contactName)
        assertEquals("Coming home now", waAction?.message)

        val openYtAction = LocalJarvisActionParser.parse("open youtube")
        assertNotNull(openYtAction)
        assertEquals(JarvisActionType.YOUTUBE, openYtAction?.actionType)

        val launchYtAction = LocalJarvisActionParser.parse("launch youtube")
        assertNotNull(launchYtAction)
        assertEquals(JarvisActionType.YOUTUBE, launchYtAction?.actionType)

        val openCameraAction = LocalJarvisActionParser.parse("open camera")
        assertNotNull(openCameraAction)
        assertEquals(JarvisActionType.OPEN_APP, openCameraAction?.actionType)
        assertEquals("camera", openCameraAction?.appName?.lowercase())

        val openSpotifyAction = LocalJarvisActionParser.parse("open spotify")
        assertNotNull(openSpotifyAction)
        assertEquals(JarvisActionType.OPEN_APP, openSpotifyAction?.actionType)
        assertEquals("spotify", openSpotifyAction?.appName?.lowercase())
    }

    @Test
    fun testExactContactMatchReturnsSingle() {
        val contacts = listOf(
            ContactResolver.ContactMatch(displayName = "Arthi", phoneNumber = "+919876543210"),
            ContactResolver.ContactMatch(displayName = "Rahul", phoneNumber = "+919876543211")
        )

        val result = ContactResolver.findMatches(contacts, "arthi")
        assertTrue(result is ContactSearchResult.Single)
        val single = result as ContactSearchResult.Single
        assertEquals("Arthi", single.contact.displayName)
        assertEquals("+919876543210", single.contact.phoneNumber)
    }

    @Test
    fun testMultipleContactsDisambiguation() {
        val contacts = listOf(
            ContactResolver.ContactMatch(displayName = "Arthi Work", phoneNumber = "+919876543210"),
            ContactResolver.ContactMatch(displayName = "Arthi Home", phoneNumber = "+919876543211")
        )

        val result = ContactResolver.findMatches(contacts, "arthi")
        assertTrue(result is ContactSearchResult.Multiple)
        val multiple = result as ContactSearchResult.Multiple
        assertEquals(2, multiple.contacts.size)
    }

    @Test
    fun testClosePhoneticFuzzyContactMatchForArthi() {
        // User asks for "arthi", contacts have "Aarthi" or "Arathi"
        val contacts = listOf(
            ContactResolver.ContactMatch(displayName = "Aarthi", phoneNumber = "+919876543210"),
            ContactResolver.ContactMatch(displayName = "Suresh", phoneNumber = "+919876543212")
        )

        val result = ContactResolver.findMatches(contacts, "arthi")
        assertTrue("Expected close match result", result is ContactSearchResult.Multiple)
        val multiple = result as ContactSearchResult.Multiple
        assertEquals("Aarthi", multiple.contacts.first().displayName)
    }

    @Test
    fun testBuiltInFamilySynonymsDadToDaddy() {
        val contacts = listOf(
            ContactResolver.ContactMatch(displayName = "Daddy", phoneNumber = "+919988776655")
        )

        // User says "call dad", contacts have "Daddy"
        val result = ContactResolver.findMatches(contacts, "dad")
        assertTrue(result is ContactSearchResult.Single)
        val single = result as ContactSearchResult.Single
        assertEquals("Daddy", single.contact.displayName)
        assertEquals("+919988776655", single.contact.phoneNumber)
    }

    @Test
    fun testCustomUserAliasOverride() {
        val contacts = listOf(
            ContactResolver.ContactMatch(displayName = "Sundar", phoneNumber = "+919111222333")
        )
        val userAliases = mapOf("boss" to "Sundar")

        val result = ContactResolver.findMatches(contacts, "boss", customAliases = userAliases)
        assertTrue(result is ContactSearchResult.Single)
        val single = result as ContactSearchResult.Single
        assertEquals("Sundar", single.contact.displayName)
    }

    @Test
    fun testParseAliasCommands() {
        val cmd1 = LocalJarvisActionParser.parseAliasCommand("set alias dad = daddy")
        assertNotNull(cmd1)
        assertEquals("dad", cmd1?.alias)
        assertEquals("daddy", cmd1?.target)

        val cmd2 = LocalJarvisActionParser.parseAliasCommand("my dad is Daddy")
        assertNotNull(cmd2)
        assertEquals("dad", cmd2?.alias)
        assertEquals("Daddy", cmd2?.target)

        val cmd3 = LocalJarvisActionParser.parseAliasCommand("alias mom as Mummy")
        assertNotNull(cmd3)
        assertEquals("mom", cmd3?.alias)
        assertEquals("Mummy", cmd3?.target)

        val cmd4 = LocalJarvisActionParser.parseAliasCommand("alias sis Chotu")
        assertNotNull(cmd4)
        assertEquals("sis", cmd4?.alias)
        assertEquals("Chotu", cmd4?.target)
    }

    @Test
    fun testCandidatePayloadDataModel() {
        val candidates = listOf(
            com.macrobite.app.domain.model.ContactCandidate("Arthi Mobile", "+919876543210"),
            com.macrobite.app.domain.model.ContactCandidate("Arthi Work", "+919876543211")
        )
        val payload = com.macrobite.app.domain.model.JarvisActionPayload(
            actionType = JarvisActionType.CALL,
            contactName = "arthi",
            candidates = candidates
        )

        assertTrue(payload.hasMultipleCandidates)
        assertEquals("Multiple Contacts Found", payload.displayTitle)
        assertTrue(payload.displaySubtitle.contains("2 contacts match"))
    }
}
