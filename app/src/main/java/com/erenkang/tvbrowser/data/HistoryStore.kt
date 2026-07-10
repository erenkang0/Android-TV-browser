package com.erenkang.tvbrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class HistoryEntry(val title: String, val url: String, val time: Long)

class HistoryStore(context: Context) {

    private val prefs = context.getSharedPreferences("history", Context.MODE_PRIVATE)

    fun all(): MutableList<HistoryEntry> {
        val raw = prefs.getString("items", null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                HistoryEntry(o.optString("t"), o.optString("u"), o.optLong("ts"))
            }.toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun record(title: String, url: String) {
        if (url.isBlank() || url.startsWith("about:")) return
        val items = all()
        if (items.firstOrNull()?.url == url) {
            // Refresh the newest entry's title instead of duplicating it.
            items[0] = HistoryEntry(title, url, System.currentTimeMillis())
        } else {
            items.add(0, HistoryEntry(title, url, System.currentTimeMillis()))
        }
        save(items.take(MAX_ENTRIES))
    }

    fun clear() {
        prefs.edit().remove("items").apply()
    }

    private fun save(items: List<HistoryEntry>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("t", it.title).put("u", it.url).put("ts", it.time))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }

    companion object {
        private const val MAX_ENTRIES = 200
    }
}
