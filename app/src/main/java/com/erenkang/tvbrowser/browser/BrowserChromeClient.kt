package com.erenkang.tvbrowser.browser

import android.os.Message
import android.view.View
import android.webkit.WebChromeClient
import android.webkit.WebView
import com.erenkang.tvbrowser.MainActivity

class BrowserChromeClient(private val browser: MainActivity) : WebChromeClient() {

    override fun onProgressChanged(view: WebView, newProgress: Int) {
        browser.onTabProgress(view, newProgress)
    }

    override fun onReceivedTitle(view: WebView, title: String?) {
        browser.onTabTitle(view, title ?: "")
    }

    override fun onShowCustomView(view: View, callback: CustomViewCallback) {
        browser.showFullscreen(view, callback)
    }

    override fun onHideCustomView() {
        browser.hideFullscreen()
    }

    override fun onCreateWindow(
        view: WebView,
        isDialog: Boolean,
        isUserGesture: Boolean,
        resultMsg: Message
    ): Boolean {
        if (!isUserGesture) return false
        val newWebView = browser.createPopupWebView() ?: return false
        (resultMsg.obj as WebView.WebViewTransport).webView = newWebView
        resultMsg.sendToTarget()
        return true
    }
}
