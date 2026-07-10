package com.erenkang.tvbrowser.browser

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.FrameLayout
import com.erenkang.tvbrowser.MainActivity
import com.erenkang.tvbrowser.R
import kotlin.math.min

class Tab(val webView: WebView) {
    var title: String = ""
    var url: String = ""
    var hasContent: Boolean = false

    /** URL restored from the previous session, loaded lazily when the tab is first selected. */
    var pendingUrl: String? = null
}

class TabManager(
    private val browser: MainActivity,
    private val container: FrameLayout
) {
    val tabs = mutableListOf<Tab>()

    var currentIndex = -1
        private set

    val current: Tab?
        get() = tabs.getOrNull(currentIndex)

    /** Notified whenever the tab list or the selected tab changes. */
    var onTabsChanged: (() -> Unit)? = null

    fun newTab(url: String? = null, select: Boolean = true, lazyLoad: Boolean = false): Tab {
        val tab = Tab(createWebView())
        tabs.add(tab)
        if (url != null) {
            tab.url = url
            tab.hasContent = true
            if (lazyLoad) {
                tab.pendingUrl = url
            } else {
                tab.webView.loadUrl(url)
            }
        }
        if (select) {
            selectTab(tabs.lastIndex)
        } else {
            onTabsChanged?.invoke()
        }
        return tab
    }

    fun selectTab(index: Int) {
        if (index !in tabs.indices) return
        currentIndex = index
        container.removeAllViews()
        val tab = tabs[index]
        container.addView(
            tab.webView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        tab.pendingUrl?.let {
            tab.pendingUrl = null
            tab.webView.loadUrl(it)
        }
        onTabsChanged?.invoke()
    }

    fun closeTab(index: Int) {
        if (index !in tabs.indices) return
        val tab = tabs.removeAt(index)
        container.removeView(tab.webView)
        tab.webView.destroy()
        if (tabs.isEmpty()) {
            currentIndex = -1
            newTab()
        } else {
            selectTab(min(index, tabs.lastIndex))
        }
    }

    fun tabOf(view: WebView): Tab? = tabs.find { it.webView === view }

    fun applyUserAgent(userAgent: String?) {
        tabs.forEach { it.webView.settings.userAgentString = userAgent }
    }

    fun clearCaches() {
        tabs.forEach { it.webView.clearCache(true) }
    }

    fun destroyAll() {
        container.removeAllViews()
        tabs.forEach { it.webView.destroy() }
        tabs.clear()
        currentIndex = -1
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun createWebView(): WebView {
        val webView = WebView(browser)
        with(webView.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = false
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            browser.settingsStore.userAgent()?.let { userAgentString = it }
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        webView.webViewClient = BrowserWebViewClient(browser)
        webView.webChromeClient = BrowserChromeClient(browser)
        webView.setDownloadListener { _, _, _, _, _ ->
            browser.toast(R.string.downloads_unsupported)
        }
        webView.isFocusable = true
        webView.isFocusableInTouchMode = true
        webView.overScrollMode = View.OVER_SCROLL_NEVER
        return webView
    }
}
