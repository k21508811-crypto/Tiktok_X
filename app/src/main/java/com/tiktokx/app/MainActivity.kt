package com.tiktokx.app

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity

private const val START_URL = "https://proverka29.ru/tiktok/"
private const val FILE_CHOOSER_REQUEST = 4102

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(WebView(this), true)

        webView = WebView(this).apply {
            setBackgroundColor(android.graphics.Color.rgb(8, 8, 11))
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = false
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = false
                allowContentAccess = true
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                userAgentString = "$userAgentString TikTokXAndroid/1.0"
            }
            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView,
                    request: WebResourceRequest,
                    error: WebResourceError
                ) {
                    super.onReceivedError(view, request, error)
                    if (request.isForMainFrame) showLoadError(error.description?.toString() ?: "Не удалось подключиться к сайту")
                }
            }
            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    view: WebView?,
                    callback: ValueCallback<Array<Uri>>?,
                    params: FileChooserParams?
                ): Boolean {
                    filePathCallback?.onReceiveValue(null)
                    filePathCallback = callback
                    val intent = try {
                        params?.createIntent()?.apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        } ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                    } catch (_: Exception) {
                        Intent(Intent.ACTION_GET_CONTENT).apply {
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "*/*"
                        }
                    }
                    return try {
                        startActivityForResult(intent, FILE_CHOOSER_REQUEST)
                        true
                    } catch (_: ActivityNotFoundException) {
                        filePathCallback = null
                        Toast.makeText(this@MainActivity, "Не найден файловый менеджер", Toast.LENGTH_LONG).show()
                        false
                    }
                }
            }
        }

        setContentView(webView)
        if (savedInstanceState == null) {
            webView.loadUrl(START_URL)
        } else {
            val restored = webView.restoreState(savedInstanceState)
            if (restored == null) webView.loadUrl(START_URL)
        }
    }

    private fun showLoadError(message: String) {
        val safeMessage = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
        val html = """
            <!doctype html><html lang="ru"><meta charset="utf-8">
            <meta name="viewport" content="width=device-width,initial-scale=1">
            <body style="margin:0;background:#08080b;color:white;font:16px system-ui;display:grid;min-height:100vh;place-items:center">
            <main style="padding:28px;text-align:center;max-width:420px">
            <h2 style="color:#fe2c55">TikTok X</h2>
            <p>Не удалось загрузить сайт.</p><p style="color:#aaa;font-size:13px">$safeMessage</p>
            <button onclick="location.href='$START_URL'" style="border:0;border-radius:24px;background:#fe2c55;color:white;padding:14px 28px;font-weight:bold">Повторить</button>
            </main></body></html>
        """.trimIndent()
        webView.loadDataWithBaseURL(START_URL, html, "text/html", "UTF-8", null)
    }

    @Deprecated("Deprecated by Android; retained for file chooser compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val result = if (resultCode == RESULT_OK) {
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            } else null
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        webView.saveState(outState)
        super.onSaveInstanceState(outState)
    }

    @Deprecated("Deprecated in Android 13; retained for older Android versions")
    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onDestroy() {
        filePathCallback?.onReceiveValue(null)
        filePathCallback = null
        if (::webView.isInitialized) {
            webView.stopLoading()
            webView.destroy()
        }
        super.onDestroy()
    }
}
