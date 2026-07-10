package com.erenkang.tvbrowser.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class Bookmark(val title: String, val url: String)

class BookmarkStore(context: Context) {

    private val prefs = context.getSharedPreferences("bookmarks", Context.MODE_PRIVATE)

    fun all(): MutableList<Bookmark> {
        val raw = prefs.getString("items", null) ?: return mutableListOf()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Bookmark(o.optString("t"), o.optString("u"))
            }.toMutableList()
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun contains(url: String): Boolean = all().any { it.url == url }

    fun add(bookmark: Bookmark) {
        val items = all()
        if (items.none { it.url == bookmark.url }) {
            items.add(0, bookmark)
            save(items)
        }
    }

    fun remove(url: String) {
        save(all().filterNot { it.url == url })
    }

    private fun save(items: List<Bookmark>) {
        val arr = JSONArray()
        items.forEach {
            arr.put(JSONObject().put("t", it.title).put("u", it.url))
        }
        prefs.edit().putString("items", arr.toString()).apply()
    }
}
