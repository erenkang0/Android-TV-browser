package com.erenkang.tvbrowser.ui

import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.erenkang.tvbrowser.MainActivity
import com.erenkang.tvbrowser.R
import com.erenkang.tvbrowser.data.SettingsStore

object Dialogs {

    private class ListViews(val root: View) {
        val list: RecyclerView = root.findViewById(R.id.dialogList)
        val empty: TextView = root.findViewById(R.id.dialogEmpty)
        val button: Button = root.findViewById(R.id.dialogButton)
    }

    private fun inflateList(browser: MainActivity): ListViews {
        val views = ListViews(LayoutInflater.from(browser).inflate(R.layout.dialog_list, null))
        views.list.layoutManager = LinearLayoutManager(browser)
        return views
    }

    fun showTabs(browser: MainActivity) {
        val views = inflateList(browser)
        val dialog = AlertDialog.Builder(browser)
            .setTitle(R.string.tabs_title)
            .setView(views.root)
            .create()
        val tabManager = browser.tabManager
        val rows = tabManager.tabs.map {
            RowItem(
                it.title.ifBlank { it.url.ifBlank { browser.getString(R.string.new_tab) } },
                it.url
            )
        }.toMutableList()
        views.list.adapter = RowAdapter(
            rows,
            onClick = { pos ->
                tabManager.selectTab(pos)
                browser.onTabSwitched()
                dialog.dismiss()
            },
            onAction = { pos ->
                tabManager.closeTab(pos)
                browser.onTabSwitched()
                dialog.dismiss()
            }
        )
        views.button.visibility = View.VISIBLE
        views.button.setText(R.string.new_tab)
        views.button.setOnClickListener {
            tabManager.newTab()
            browser.showHome()
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showBookmarks(browser: MainActivity) {
        val views = inflateList(browser)
        val dialog = AlertDialog.Builder(browser)
            .setTitle(R.string.bookmarks_title)
            .setView(views.root)
            .create()
        val store = browser.bookmarkStore
        val bookmarks = store.all()
        val rows = bookmarks.map { RowItem(it.title.ifBlank { it.url }, it.url) }.toMutableList()
        val adapter = RowAdapter(
            rows,
            onClick = { pos ->
                browser.navigate(bookmarks[pos].url)
                dialog.dismiss()
            },
            onAction = { pos ->
                store.remove(bookmarks[pos].url)
                bookmarks.removeAt(pos)
                rows.removeAt(pos)
                views.list.adapter?.notifyItemRemoved(pos)
                if (rows.isEmpty()) showEmpty(views, browser.getString(R.string.no_bookmarks))
                browser.toast(R.string.bookmark_removed)
            }
        )
        views.list.adapter = adapter
        if (rows.isEmpty()) showEmpty(views, browser.getString(R.string.no_bookmarks))
        dialog.show()
    }

    fun showHistory(browser: MainActivity) {
        val views = inflateList(browser)
        val dialog = AlertDialog.Builder(browser)
            .setTitle(R.string.history_title)
            .setView(views.root)
            .create()
        val entries = browser.historyStore.all()
        val rows = entries.map {
            RowItem(it.title.ifBlank { it.url }, it.url, showAction = false)
        }.toMutableList()
        views.list.adapter = RowAdapter(rows, onClick = { pos ->
            browser.navigate(entries[pos].url)
            dialog.dismiss()
        })
        if (rows.isEmpty()) {
            showEmpty(views, browser.getString(R.string.no_history))
        } else {
            views.button.visibility = View.VISIBLE
            views.button.setText(R.string.clear_history)
            views.button.setOnClickListener {
                browser.historyStore.clear()
                browser.toast(R.string.history_cleared)
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    fun showMenu(browser: MainActivity) {
        val labels = arrayOf(
            browser.getString(R.string.bookmarks_title),
            browser.getString(R.string.history_title),
            browser.getString(R.string.settings_title)
        )
        AlertDialog.Builder(browser)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> showBookmarks(browser)
                    1 -> showHistory(browser)
                    2 -> showSettings(browser)
                }
            }
            .show()
    }

    fun showSettings(browser: MainActivity) {
        val settings = browser.settingsStore
        val uaLabels = arrayOf(
            browser.getString(R.string.ua_tv),
            browser.getString(R.string.ua_desktop),
            browser.getString(R.string.ua_mobile)
        )
        val labels = arrayOf(
            browser.getString(R.string.search_engine) + ": " +
                SettingsStore.SEARCH_ENGINES[settings.searchEngine].first,
            browser.getString(R.string.user_agent) + ": " + uaLabels[settings.uaMode],
            browser.getString(R.string.clear_history),
            browser.getString(R.string.clear_cookies),
            browser.getString(R.string.clear_cache)
        )
        AlertDialog.Builder(browser)
            .setTitle(R.string.settings_title)
            .setItems(labels) { _, which ->
                when (which) {
                    0 -> chooseSearchEngine(browser)
                    1 -> chooseUserAgent(browser, uaLabels)
                    2 -> {
                        browser.historyStore.clear()
                        browser.toast(R.string.history_cleared)
                    }
                    3 -> {
                        CookieManager.getInstance().removeAllCookies(null)
                        CookieManager.getInstance().flush()
                        browser.toast(R.string.cookies_cleared)
                    }
                    4 -> {
                        browser.tabManager.clearCaches()
                        browser.toast(R.string.cache_cleared)
                    }
                }
            }
            .show()
    }

    private fun chooseSearchEngine(browser: MainActivity) {
        val names = SettingsStore.SEARCH_ENGINES.map { it.first }.toTypedArray()
        AlertDialog.Builder(browser)
            .setTitle(R.string.search_engine)
            .setSingleChoiceItems(names, browser.settingsStore.searchEngine) { dialog, which ->
                browser.settingsStore.searchEngine = which
                dialog.dismiss()
            }
            .show()
    }

    private fun chooseUserAgent(browser: MainActivity, uaLabels: Array<String>) {
        AlertDialog.Builder(browser)
            .setTitle(R.string.user_agent)
            .setSingleChoiceItems(uaLabels, browser.settingsStore.uaMode) { dialog, which ->
                browser.settingsStore.uaMode = which
                browser.tabManager.applyUserAgent(browser.settingsStore.userAgent())
                dialog.dismiss()
            }
            .show()
    }

    fun confirmExit(browser: MainActivity) {
        AlertDialog.Builder(browser)
            .setTitle(R.string.exit_title)
            .setMessage(R.string.exit_message)
            .setPositiveButton(R.string.exit_yes) { _, _ -> browser.finish() }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showEmpty(views: ListViews, text: String) {
        views.empty.text = text
        views.empty.visibility = View.VISIBLE
        views.list.visibility = View.GONE
    }
}
