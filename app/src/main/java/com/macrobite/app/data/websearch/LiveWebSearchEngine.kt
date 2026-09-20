package com.macrobite.app.data.websearch

import android.util.Log
import android.util.Xml
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

data class WebSearchResultItem(
    val title: String,
    val snippet: String = "",
    val source: String = "",
    val date: String = ""
)

object LiveWebSearchEngine {

    suspend fun search(query: String): List<WebSearchResultItem> = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isBlank()) return@withContext emptyList()

        val results = mutableListOf<WebSearchResultItem>()

        // 1. If it's a food, snacks, or brand macro query, query Open Food Facts first
        val isFoodRelated = isFoodQuery(cleanQuery)
        if (isFoodRelated) {
            try {
                val foodItems = fetchOpenFoodFacts(cleanQuery)
                results.addAll(foodItems.take(3))
            } catch (e: Exception) {
                Log.w("LiveWebSearchEngine", "Open Food Facts search failed: ${e.message}")
            }
        }

        // 2. Google News & Web Search RSS Feed (Real-time live Google results without API key restrictions)
        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val rssUrl = "https://news.google.com/rss/search?q=$encodedQuery&hl=en-IN&gl=IN&ceid=IN:en"
            val rssItems = fetchGoogleRss(rssUrl)
            results.addAll(rssItems.take(5))
        } catch (e: Exception) {
            Log.w("LiveWebSearchEngine", "Google RSS search failed: ${e.message}")
        }

        // 3. If fewer than 2 results found, fallback to Wikipedia Search API
        if (results.size < 2) {
            try {
                val wikiItems = fetchWikipedia(cleanQuery)
                results.addAll(wikiItems.take(3))
            } catch (e: Exception) {
                Log.w("LiveWebSearchEngine", "Wikipedia search failed: ${e.message}")
            }
        }

        return@withContext results
    }

    private fun isFoodQuery(q: String): Boolean {
        val lower = q.lowercase(Locale.ROOT)
        val keywords = listOf(
            "calories", "calorie", "protein", "macro", "macros", "carbs", "fats", "nutrition",
            "pack", "packet", "snack", "snacks", "chips", "biscuit", "cookie", "shake",
            "curry", "rice", "roti", "biryani", "paneer", "chicken", "egg", "bar",
            "gram", "grams", "100g", "food", "diet", "eating", "drank", "ate", "lassi",
            "haldiram", "amul", "cadbury", "maggi", "lays", "parle", "britannia", "nestle"
        )
        return keywords.any { lower.contains(it) }
    }

    private fun fetchGoogleRss(urlStr: String): List<WebSearchResultItem> {
        val items = mutableListOf<WebSearchResultItem>()
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
        conn.connectTimeout = 6000
        conn.readTimeout = 8000

        try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val xmlText = conn.inputStream.bufferedReader().use { it.readText() }
                val parser: XmlPullParser = Xml.newPullParser()
                parser.setInput(StringReader(xmlText))

                var eventType = parser.eventType
                var inItem = false
                var currentTitle = ""
                var currentPubDate = ""
                var currentSource = ""

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    val name = parser.name
                    when (eventType) {
                        XmlPullParser.START_TAG -> {
                            if (name.equals("item", ignoreCase = true)) {
                                inItem = true
                                currentTitle = ""
                                currentPubDate = ""
                                currentSource = ""
                            } else if (inItem) {
                                when {
                                    name.equals("title", ignoreCase = true) -> {
                                        currentTitle = parser.nextText() ?: ""
                                    }
                                    name.equals("pubDate", ignoreCase = true) -> {
                                        currentPubDate = parser.nextText() ?: ""
                                    }
                                    name.equals("source", ignoreCase = true) -> {
                                        currentSource = parser.nextText() ?: ""
                                    }
                                }
                            }
                        }
                        XmlPullParser.END_TAG -> {
                            if (name.equals("item", ignoreCase = true)) {
                                if (currentTitle.isNotBlank()) {
                                    items.add(
                                        WebSearchResultItem(
                                            title = currentTitle.trim(),
                                            date = currentPubDate.trim(),
                                            source = if (currentSource.isNotBlank()) currentSource.trim() else "Google News"
                                        )
                                    )
                                }
                                inItem = false
                            }
                        }
                    }
                    eventType = parser.next()
                }
            }
        } finally {
            conn.disconnect()
        }
        return items
    }

    private fun fetchOpenFoodFacts(foodQuery: String): List<WebSearchResultItem> {
        val items = mutableListOf<WebSearchResultItem>()
        val encoded = URLEncoder.encode(foodQuery, "UTF-8")
        val url = URL("https://world.openfoodfacts.org/cgi/search.pl?search_terms=$encoded&search_simple=1&action=process&json=1&page_size=3")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "MacroBiteApp/1.0 (Android Fitness App)")
        conn.connectTimeout = 5000
        conn.readTimeout = 7000

        try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(jsonText).asJsonObject
                val products = root.getAsJsonArray("products")
                if (products != null) {
                    for (i in 0 until products.size()) {
                        val p = products[i].asJsonObject
                        val name = if (p.has("product_name") && !p.get("product_name").isJsonNull) p.get("product_name").asString else ""
                        val brand = if (p.has("brands") && !p.get("brands").isJsonNull) p.get("brands").asString else ""
                        val nutriments = if (p.has("nutriments") && !p.get("nutriments").isJsonNull) p.getAsJsonObject("nutriments") else null

                        if (name.isNotBlank() && nutriments != null) {
                            val calories = nutriments.get("energy-kcal_100g")?.asFloat ?: 0f
                            val protein = nutriments.get("proteins_100g")?.asFloat ?: 0f
                            val carbs = nutriments.get("carbohydrates_100g")?.asFloat ?: 0f
                            val fat = nutriments.get("fat_100g")?.asFloat ?: 0f

                            val snippet = "Brand: $brand | Per 100g: ${calories.toInt()} kcal, ${protein}g protein, ${carbs}g carbs, ${fat}g fat"
                            items.add(
                                WebSearchResultItem(
                                    title = "$name ($brand)",
                                    snippet = snippet,
                                    source = "Open Food Facts"
                                )
                            )
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return items
    }

    private fun fetchWikipedia(wikiQuery: String): List<WebSearchResultItem> {
        val items = mutableListOf<WebSearchResultItem>()
        val encoded = URLEncoder.encode(wikiQuery, "UTF-8")
        val url = URL("https://en.wikipedia.org/w/api.php?action=query&list=search&srsearch=$encoded&format=json&utf8=")
        val conn = url.openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.setRequestProperty("User-Agent", "MacroBiteApp/1.0 (Android Fitness App)")
        conn.connectTimeout = 5000
        conn.readTimeout = 7000

        try {
            if (conn.responseCode == HttpURLConnection.HTTP_OK) {
                val jsonText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JsonParser.parseString(jsonText).asJsonObject
                val queryObj = root.getAsJsonObject("query")
                val searchArr = queryObj?.getAsJsonArray("search")
                if (searchArr != null) {
                    for (i in 0 until searchArr.size()) {
                        val itm = searchArr[i].asJsonObject
                        val title = itm.get("title")?.asString ?: ""
                        val snippetHtml = itm.get("snippet")?.asString ?: ""
                        val cleanSnippet = snippetHtml.replace(Regex("<[^>]*>"), "")
                        if (title.isNotBlank()) {
                            items.add(
                                WebSearchResultItem(
                                    title = title,
                                    snippet = cleanSnippet,
                                    source = "Wikipedia"
                                )
                            )
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
        return items
    }
}
