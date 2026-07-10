package com.erenkang.tvbrowser.data

import android.content.Context
import org.json.JSONArray
import java.net.URLEncoder

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var searchEngine: Int
        get() = prefs.getInt("search_engine", 0).coerceIn(0, SEARCH_ENGINES.lastIndex)
        set(value) = prefs.edit().putInt("search_engine", value).apply()

    var uaMode: Int
        get() = prefs.getInt("ua_mode", UA_TV)
        set(value) = prefs.edit().putInt("ua_mode", value).apply()

    fun searchUrl(query: String): String =
        SEARCH_ENGINES[searchEngine].second + URLEncoder.encode(query, "UTF-8")

    /** null means: keep the WebView's default (TV) user agent. */
    fun userAgent(): String? = when (uaMode) {
        UA_DESKTOP -> UA_STRING_DESKTOP
        UA_MOBILE -> UA_STRING_MOBILE
        else -> null
    }

    fun saveSession(urls: List<String>) {
        val arr = JSONArray()
        urls.forEach { arr.put(it) }
        prefs.edit().putString("session", arr.toString()).apply()
    }

    fun loadSession(): List<String> {
        val raw = prefs.getString("session", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { arr.getString(it) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    companion object {
        const val UA_TV = 0
        const val UA_DESKTOP = 1
        const val UA_MOBILE = 2

        val SEARCH_ENGINES = listOf(
            "Google" to "https://www.google.com/search?q=",
            "Yandex" to "https://yandex.com.tr/search/?text=",
            "DuckDuckGo" to "https://duckduckgo.com/?q=",
            "Bing" to "https://www.bing.com/search?q="
        )

        private const val UA_STRING_DESKTOP =
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Safari/537.36"
        private const val UA_STRING_MOBILE =
            "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0.0.0 Mobile Safari/537.36"
    }
}
