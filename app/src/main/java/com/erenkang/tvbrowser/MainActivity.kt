package com.erenkang.tvbrowser

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.erenkang.tvbrowser.browser.TabManager
import com.erenkang.tvbrowser.data.Bookmark
import com.erenkang.tvbrowser.data.BookmarkStore
import com.erenkang.tvbrowser.data.HistoryStore
import com.erenkang.tvbrowser.data.SettingsStore
import com.erenkang.tvbrowser.databinding.ActivityMainBinding
import com.erenkang.tvbrowser.home.SpeedDialAdapter
import com.erenkang.tvbrowser.home.SpeedDialItem
import com.erenkang.tvbrowser.ui.Dialogs

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    lateinit var settingsStore: SettingsStore
        private set
    lateinit var bookmarkStore: BookmarkStore
        private set
    lateinit var historyStore: HistoryStore
        private set
    lateinit var tabManager: TabManager
        private set

    private var fullscreenView: View? = null
    private var fullscreenCallback: WebChromeClient.CustomViewCallback? = null

    private val homeVisible: Boolean
        get() = binding.homeContainer.visibility == View.VISIBLE
    private val toolbarVisible: Boolean
        get() = binding.toolbarWrap.visibility == View.VISIBLE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settingsStore = SettingsStore(this)
        bookmarkStore = BookmarkStore(this)
        historyStore = HistoryStore(this)
        tabManager = TabManager(this, binding.webContainer)
        tabManager.onTabsChanged = {
            binding.tabCount.text = tabManager.tabs.size.toString()
            binding.cursorLayout.targetView = tabManager.current?.webView
        }
        binding.cursorLayout.onEdgeBumpTop = { runOnUiThread { showToolbar() } }

        setupToolbar()
        setupHome()
        restoreSession()

        val intentUrl = intent?.dataString
        when {
            intentUrl != null -> navigate(intentUrl)
            tabManager.current?.hasContent == true -> hideHome()
            else -> showHome()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { navigate(it) }
    }

    private fun restoreSession() {
        val session = settingsStore.loadSession()
        if (session.isEmpty()) {
            tabManager.newTab()
        } else {
            session.forEach { tabManager.newTab(url = it, select = false, lazyLoad = true) }
            tabManager.selectTab(0)
        }
    }

    private fun setupToolbar() {
        binding.btnBack.setOnClickListener {
            currentWebView()?.let { if (it.canGoBack()) it.goBack() }
        }
        binding.btnForward.setOnClickListener {
            currentWebView()?.let { if (it.canGoForward()) it.goForward() }
        }
        binding.btnRefresh.setOnClickListener { currentWebView()?.reload() }
        binding.btnHome.setOnClickListener { showHome() }
        binding.btnBookmark.setOnClickListener { toggleBookmark() }
        binding.btnTabs.setOnClickListener { Dialogs.showTabs(this) }
        binding.btnMenu.setOnClickListener { Dialogs.showMenu(this) }
        binding.urlInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN)
            ) {
                navigate(binding.urlInput.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun setupHome() {
        binding.speedDialGrid.layoutManager = GridLayoutManager(this, GRID_COLUMNS)
        refreshSpeedDial()
        binding.homeSearchInput.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER &&
                    event.action == KeyEvent.ACTION_DOWN)
            ) {
                navigate(binding.homeSearchInput.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun refreshSpeedDial() {
        val defaults = DEFAULT_SITES.map { SpeedDialItem(it.first, it.second) }
        val fromBookmarks = bookmarkStore.all()
            .filter { bookmark -> defaults.none { it.url == bookmark.url } }
            .map { SpeedDialItem(it.title.ifBlank { it.url }, it.url) }
        binding.speedDialGrid.adapter = SpeedDialAdapter(defaults + fromBookmarks) {
            navigate(it.url)
        }
    }

    // ---- Navigation ----

    fun navigate(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return
        val url = normalizeUrl(trimmed)
        hideIme()
        hideToolbar()
        hideHome()
        val tab = tabManager.current ?: tabManager.newTab()
        tab.hasContent = true
        tab.pendingUrl = null
        tab.webView.loadUrl(url)
        binding.cursorLayout.centerCursor()
    }

    private fun normalizeUrl(input: String): String {
        val hasScheme = input.startsWith("http://") || input.startsWith("https://") ||
            input.startsWith("about:")
        return when {
            hasScheme -> input
            !input.contains(" ") && input.contains(".") -> "https://$input"
            else -> settingsStore.searchUrl(input)
        }
    }

    // ---- Home / toolbar visibility ----

    fun showHome() {
        hideToolbar()
        refreshSpeedDial()
        binding.homeSearchInput.setText("")
        binding.homeContainer.visibility = View.VISIBLE
        binding.homeSearchInput.requestFocus()
    }

    private fun hideHome() {
        binding.homeContainer.visibility = View.GONE
    }

    private fun showToolbar() {
        if (homeVisible || fullscreenView != null) return
        binding.cursorLayout.releaseAllKeys()
        binding.toolbarWrap.visibility = View.VISIBLE
        binding.urlInput.setText(tabManager.current?.url.orEmpty())
        updateBookmarkStar()
        binding.urlInput.requestFocus()
    }

    private fun hideToolbar() {
        binding.toolbarWrap.visibility = View.GONE
        hideIme()
    }

    fun onTabSwitched() {
        hideToolbar()
        if (tabManager.current?.hasContent == true) hideHome() else showHome()
    }

    private fun toggleBookmark() {
        val tab = tabManager.current ?: return
        if (tab.url.isBlank()) return
        if (bookmarkStore.contains(tab.url)) {
            bookmarkStore.remove(tab.url)
            toast(R.string.bookmark_removed)
        } else {
            bookmarkStore.add(Bookmark(tab.title.ifBlank { tab.url }, tab.url))
            toast(R.string.bookmark_added)
        }
        updateBookmarkStar()
    }

    private fun updateBookmarkStar() {
        val bookmarked = tabManager.current?.url?.let { bookmarkStore.contains(it) } == true
        val color = if (bookmarked) {
            ContextCompat.getColor(this, R.color.star_active)
        } else {
            ContextCompat.getColor(this, R.color.text_primary)
        }
        binding.btnBookmark.setColorFilter(color)
    }

    private fun hideIme() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { imm.hideSoftInputFromWindow(it.windowToken, 0) }
    }

    private fun currentWebView(): WebView? = tabManager.current?.webView

    // ---- Callbacks from WebView clients ----

    fun onTabPageStarted(view: WebView, url: String) {
        tabManager.tabOf(view)?.let {
            it.url = url
            it.hasContent = true
            it.title = ""
        }
        if (view === currentWebView()) {
            if (toolbarVisible) binding.urlInput.setText(url)
            updateBookmarkStar()
        }
    }

    fun onTabPageFinished(view: WebView, url: String) {
        val tab = tabManager.tabOf(view) ?: return
        tab.url = url
        if (tab.title.isBlank()) tab.title = view.title.orEmpty()
        historyStore.record(tab.title, url)
        if (view === currentWebView()) updateBookmarkStar()
    }

    fun onTabProgress(view: WebView, progress: Int) {
        if (view !== currentWebView()) return
        binding.progressBar.progress = progress
        binding.progressBar.visibility = if (progress in 1..99) View.VISIBLE else View.GONE
    }

    fun onTabTitle(view: WebView, title: String) {
        tabManager.tabOf(view)?.title = title
    }

    fun onTabPageError(view: WebView, failingUrl: String) {
        val title = getString(R.string.page_error_title)
        val safeUrl = failingUrl.replace("<", "&lt;").replace(">", "&gt;")
        val html = """
            <html><head><meta name="viewport" content="width=device-width, initial-scale=1"></head>
            <body style="background:#0E1116;color:#F2F5FA;font-family:sans-serif;
                display:flex;align-items:center;justify-content:center;height:100vh;margin:0">
            <div style="text-align:center;max-width:80%">
            <div style="font-size:56px">&#9888;</div>
            <h2 style="font-weight:600">$title</h2>
            <p style="color:#9AA7B8;word-break:break-all">$safeUrl</p>
            </div></body></html>
        """.trimIndent()
        view.loadDataWithBaseURL(failingUrl, html, "text/html", "utf-8", failingUrl)
    }

    fun confirmSslError(handler: SslErrorHandler) {
        AlertDialog.Builder(this)
            .setTitle(R.string.ssl_error_title)
            .setMessage(R.string.ssl_error_message)
            .setPositiveButton(R.string.ssl_continue) { _, _ -> handler.proceed() }
            .setNegativeButton(R.string.cancel) { _, _ -> handler.cancel() }
            .setOnCancelListener { handler.cancel() }
            .show()
    }

    fun createPopupWebView(): WebView? {
        val tab = tabManager.newTab()
        tab.hasContent = true
        hideHome()
        return tab.webView
    }

    fun toast(resId: Int) {
        Toast.makeText(this, resId, Toast.LENGTH_SHORT).show()
    }

    // ---- Fullscreen video ----

    fun showFullscreen(view: View, callback: WebChromeClient.CustomViewCallback) {
        if (fullscreenView != null) {
            callback.onCustomViewHidden()
            return
        }
        binding.cursorLayout.releaseAllKeys()
        fullscreenView = view
        fullscreenCallback = callback
        binding.fullscreenContainer.addView(
            view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        binding.fullscreenContainer.visibility = View.VISIBLE
        hideToolbar()
    }

    fun hideFullscreen() {
        val view = fullscreenView ?: return
        binding.fullscreenContainer.removeView(view)
        binding.fullscreenContainer.visibility = View.GONE
        fullscreenView = null
        fullscreenCallback?.onCustomViewHidden()
        fullscreenCallback = null
    }

    // ---- Key handling ----

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode

        if (fullscreenView != null) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) hideFullscreen()
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        if (homeVisible) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action == KeyEvent.ACTION_UP) {
                    if (tabManager.current?.hasContent == true) {
                        hideHome()
                    } else {
                        Dialogs.confirmExit(this)
                    }
                }
                return true
            }
            return super.dispatchKeyEvent(event)
        }

        if (toolbarVisible) {
            when (keyCode) {
                KeyEvent.KEYCODE_BACK -> {
                    if (event.action == KeyEvent.ACTION_UP) hideToolbar()
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (event.action == KeyEvent.ACTION_DOWN) hideToolbar()
                    return true
                }
            }
            return super.dispatchKeyEvent(event)
        }

        // Cursor (web page) mode.
        when (event.action) {
            KeyEvent.ACTION_DOWN ->
                if (binding.cursorLayout.handleKeyDown(keyCode, event)) return true
            KeyEvent.ACTION_UP ->
                if (binding.cursorLayout.handleKeyUp(keyCode)) return true
        }
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (event.action == KeyEvent.ACTION_UP) {
                    val webView = currentWebView()
                    if (webView?.canGoBack() == true) {
                        webView.goBack()
                    } else {
                        Dialogs.confirmExit(this)
                    }
                }
                return true
            }
            KeyEvent.KEYCODE_MENU -> {
                if (event.action == KeyEvent.ACTION_UP) showToolbar()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    // ---- Lifecycle ----

    override fun onPause() {
        super.onPause()
        settingsStore.saveSession(
            tabManager.tabs.filter { it.hasContent && it.url.isNotBlank() }.map { it.url }
        )
        CookieManager.getInstance().flush()
    }

    override fun onDestroy() {
        if (isFinishing) tabManager.destroyAll()
        super.onDestroy()
    }

    companion object {
        private const val GRID_COLUMNS = 5

        private val DEFAULT_SITES = listOf(
            "Google" to "https://www.google.com",
            "YouTube" to "https://www.youtube.com",
            "Wikipedia" to "https://www.wikipedia.org",
            "X" to "https://x.com",
            "Reddit" to "https://www.reddit.com",
            "Twitch" to "https://www.twitch.tv",
            "IMDb" to "https://www.imdb.com",
            "BBC" to "https://www.bbc.com"
        )
    }
}
