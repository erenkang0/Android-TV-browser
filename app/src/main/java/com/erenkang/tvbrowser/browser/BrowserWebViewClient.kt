package com.erenkang.tvbrowser.browser

import android.graphics.Bitmap
import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.erenkang.tvbrowser.MainActivity

class BrowserWebViewClient(private val browser: MainActivity) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
        shouldBlock(request.url.toString())

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
        shouldBlock(url)

    /** Loads http/https in the WebView; silently drops other schemes (no external apps on TV). */
    private fun shouldBlock(url: String): Boolean =
        !(url.startsWith("http://") || url.startsWith("https://") || url.startsWith("about:"))

    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
        browser.onTabPageStarted(view, url)
    }

    override fun onPageFinished(view: WebView, url: String) {
        browser.onTabPageFinished(view, url)
    }

    override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
        browser.confirmSslError(handler)
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        if (request.isForMainFrame) {
            browser.onTabPageError(view, request.url.toString())
        }
    }
}
