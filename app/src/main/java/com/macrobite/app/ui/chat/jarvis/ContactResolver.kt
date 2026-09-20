package com.macrobite.app.ui.chat.jarvis

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

sealed class ContactSearchResult {
    data class Single(val contact: ContactResolver.ContactMatch) : ContactSearchResult()
    data class Multiple(val query: String, val contacts: List<ContactResolver.ContactMatch>) : ContactSearchResult()
    data class None(val query: String) : ContactSearchResult()
}

object ContactResolver {

    private const val TAG = "ContactResolver"

    data class ContactMatch(
        val displayName: String,
        val phoneNumber: String
    )

    // Common built-in relationship synonyms so "dad", "mom", "brother" etc. work automatically
    val BUILT_IN_RELATIONSHIPS: Map<String, List<String>> = mapOf(
        "dad" to listOf("dad", "daddy", "father", "papa", "appa", "nanna", "pitaji", "baba", "pappa"),
        "daddy" to listOf("dad", "daddy", "father", "papa", "appa", "nanna", "pitaji", "baba", "pappa"),
        "father" to listOf("dad", "daddy", "father", "papa", "appa", "nanna", "pitaji", "baba", "pappa"),
        "papa" to listOf("dad", "daddy", "father", "papa", "appa", "nanna", "pitaji", "baba", "pappa"),
        "appa" to listOf("dad", "daddy", "father", "papa", "appa", "nanna", "pitaji", "baba", "pappa"),
        "nanna" to listOf("dad", "daddy", "father", "papa", "appa", "nanna", "pitaji", "baba", "pappa"),
        "mom" to listOf("mom", "mummy", "mother", "amma", "ammi", "maa", "mataji", "mommy", "aai"),
        "mummy" to listOf("mom", "mummy", "mother", "amma", "ammi", "maa", "mataji", "mommy", "aai"),
        "mother" to listOf("mom", "mummy", "mother", "amma", "ammi", "maa", "mataji", "mommy", "aai"),
        "amma" to listOf("mom", "mummy", "mother", "amma", "ammi", "maa", "mataji", "mommy", "aai"),
        "maa" to listOf("mom", "mummy", "mother", "amma", "ammi", "maa", "mataji", "mommy", "aai"),
        "mommy" to listOf("mom", "mummy", "mother", "amma", "ammi", "maa", "mataji", "mommy", "aai"),
        "bro" to listOf("brother", "bro", "bhai", "anna", "thambi", "bhaiya"),
        "brother" to listOf("brother", "bro", "bhai", "anna", "thambi", "bhaiya"),
        "bhai" to listOf("brother", "bro", "bhai", "anna", "thambi", "bhaiya"),
        "sis" to listOf("sister", "sis", "didi", "akka", "chelli", "behan"),
        "sister" to listOf("sister", "sis", "didi", "akka", "chelli", "behan"),
        "wife" to listOf("wife", "wifey", "biwi", "patni"),
        "wifey" to listOf("wife", "wifey", "biwi", "patni"),
        "husband" to listOf("husband", "hubby", "pati"),
        "hubby" to listOf("husband", "hubby", "pati")
    )

    fun getAllContacts(context: Context): List<ContactMatch> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }

        val results = mutableListOf<ContactMatch>()
        val seen = mutableSetOf<String>()

        try {
            val contentResolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val sortOrder = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"

            contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext()) {
                    val name = if (nameIndex != -1) cursor.getString(nameIndex)?.trim().orEmpty() else ""
                    val number = if (numberIndex != -1) cursor.getString(numberIndex)?.trim().orEmpty() else ""
                    if (name.isNotBlank() && number.isNotBlank()) {
                        val key = "${name.lowercase()}___${number.replace(Regex("[^0-9+]"), "")}"
                        if (seen.add(key)) {
                            results.add(ContactMatch(displayName = name, phoneNumber = number))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying contacts", e)
        }
        return results
    }

    /**
     * Pure matcher function for contacts search, aliases, and fuzzy/phonetic similarity.
     * Evaluates exact, alias, substring, and close matches.
     */
    fun findMatches(
        allContacts: List<ContactMatch>,
        rawQuery: String,
        customAliases: Map<String, String> = emptyMap()
    ): ContactSearchResult {
        val trimmed = rawQuery.trim()
        if (trimmed.isBlank() || allContacts.isEmpty()) {
            return ContactSearchResult.None(rawQuery)
        }

        val lowerQuery = trimmed.lowercase()

        // 1. User Custom Alias match (highest priority)
        val customTarget = customAliases[lowerQuery]
        if (!customTarget.isNullOrBlank()) {
            val aliasMatches = allContacts.filter { it.displayName.equals(customTarget, ignoreCase = true) }
            if (aliasMatches.size == 1) {
                return ContactSearchResult.Single(aliasMatches.first())
            }
            if (aliasMatches.size > 1) {
                return ContactSearchResult.Multiple(rawQuery, aliasMatches)
            }
            // Substring match on alias target
            val aliasSub = allContacts.filter { it.displayName.contains(customTarget, ignoreCase = true) }
            if (aliasSub.size == 1) {
                return ContactSearchResult.Single(aliasSub.first())
            }
            if (aliasSub.size > 1) {
                return ContactSearchResult.Multiple(rawQuery, aliasSub)
            }
        }

        // 2. Built-in Relationship Synonyms match (e.g. "dad" -> "Daddy", "Mom" -> "Mummy")
        val synonyms = BUILT_IN_RELATIONSHIPS[lowerQuery]
        if (synonyms != null) {
            val synExact = allContacts.filter { c ->
                synonyms.any { s -> c.displayName.equals(s, ignoreCase = true) }
            }
            if (synExact.size == 1) {
                return ContactSearchResult.Single(synExact.first())
            }
            if (synExact.size > 1) {
                return ContactSearchResult.Multiple(rawQuery, synExact)
            }

            val synSub = allContacts.filter { c ->
                synonyms.any { s -> c.displayName.contains(s, ignoreCase = true) }
            }
            if (synSub.size == 1) {
                return ContactSearchResult.Single(synSub.first())
            }
            if (synSub.size > 1) {
                return ContactSearchResult.Multiple(rawQuery, synSub)
            }
        }

        // 3. Exact Display Name match
        val exactMatches = allContacts.filter { it.displayName.equals(trimmed, ignoreCase = true) }
        if (exactMatches.size == 1) {
            return ContactSearchResult.Single(exactMatches.first())
        }
        if (exactMatches.size > 1) {
            return ContactSearchResult.Multiple(rawQuery, exactMatches)
        }

        // 4. If exact match not found, gather all close, prefix, substring, and phonetic candidates
        val candidates = mutableListOf<ContactMatch>()
        val seenKeys = mutableSetOf<String>()

        fun addCandidate(c: ContactMatch) {
            val key = "${c.displayName.lowercase()}___${c.phoneNumber.replace(Regex("[^0-9+]"), "")}"
            if (seenKeys.add(key)) {
                candidates.add(c)
            }
        }

        // Prefix & Word Start matches (e.g. "Arthi Sharma" or "Dr Arthi" for "Arthi")
        allContacts.filter { c ->
            val name = c.displayName.lowercase()
            name.startsWith(lowerQuery) ||
            name.split(" ", "_", "-", ".").any { word -> word.startsWith(lowerQuery) }
        }.forEach { addCandidate(it) }

        // Substring matches
        allContacts.filter { it.displayName.contains(lowerQuery, ignoreCase = true) }
            .forEach { addCandidate(it) }

        // Close / Fuzzy / Relevant / Phonetic matches (e.g. "arthi" matches "Aarthi", "Arathi", "Aarti", "Arti")
        val phoneticQuery = normalizePhonetic(trimmed)
        val closeMatches = mutableListOf<Pair<ContactMatch, Int>>()

        for (contact in allContacts) {
            val contactName = contact.displayName.trim()
            val phoneticContact = normalizePhonetic(contactName)

            // Exact phonetic match (e.g. "arthi" and "aarthi" both normalize to "arti")
            if (phoneticQuery.length >= 3 && (phoneticContact == phoneticQuery || phoneticContact.contains(phoneticQuery))) {
                closeMatches.add(contact to 0)
                continue
            }

            // Word-level Levenshtein and phonetic distance
            val words = contactName.split(" ", "_", "-", ".")
            var minWordDist = Int.MAX_VALUE

            for (word in words) {
                val cleanWord = word.trim().lowercase()
                if (cleanWord.isBlank()) continue

                val wordDist = levenshteinDistance(cleanWord, lowerQuery)
                val wordPhoneticDist = levenshteinDistance(normalizePhonetic(cleanWord), phoneticQuery)

                val effectiveDist = minOf(wordDist, wordPhoneticDist)
                if (effectiveDist < minWordDist) {
                    minWordDist = effectiveDist
                }
            }

            val maxAllowedDist = if (trimmed.length >= 5) 2 else if (trimmed.length >= 3) 1 else 0
            if (minWordDist <= maxAllowedDist) {
                closeMatches.add(contact to minWordDist)
            }
        }

        closeMatches.sortedBy { it.second }.forEach { addCandidate(it.first) }

        if (candidates.isNotEmpty()) {
            // Per user requirement: If exact match not found, show candidate options to user to pick who to call
            return ContactSearchResult.Multiple(rawQuery, candidates.take(6))
        }

        return ContactSearchResult.None(rawQuery)
    }

    fun searchContacts(
        context: Context,
        rawQuery: String,
        customAliases: Map<String, String> = emptyMap()
    ): ContactSearchResult {
        val contacts = getAllContacts(context)
        return findMatches(contacts, rawQuery, customAliases)
    }

    fun findContactPhoneNumber(context: Context, contactName: String): ContactMatch? {
        val result = searchContacts(context, contactName)
        return when (result) {
            is ContactSearchResult.Single -> result.contact
            is ContactSearchResult.Multiple -> result.contacts.firstOrNull()
            is ContactSearchResult.None -> null
        }
    }

    fun levenshteinDistance(s1: String, s2: String): Int {
        val a = s1.lowercase().trim()
        val b = s2.lowercase().trim()
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val costs = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            costs[0] = i
            var nw = i - 1
            for (j in 1..b.length) {
                val cj = minOf(
                    1 + minOf(costs[j], costs[j - 1]),
                    if (a[i - 1] == b[j - 1]) nw else nw + 1
                )
                nw = costs[j]
                costs[j] = cj
            }
        }
        return costs[b.length]
    }

    fun normalizePhonetic(s: String): String {
        return s.lowercase().trim()
            .replace("aa", "a")
            .replace("ee", "i")
            .replace("oo", "u")
            .replace("th", "t")
            .replace("dh", "d")
            .replace("ph", "f")
            .replace("sh", "s")
            .replace("ch", "c")
            .replace("ck", "k")
            .replace("c", "k")
            .replace(Regex("[^a-z0-9]"), "")
    }
}
