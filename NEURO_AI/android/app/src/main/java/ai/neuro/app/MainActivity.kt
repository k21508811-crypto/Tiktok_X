package ai.neuro.app

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val pageBg = Color(0xFF0B1020)
private val pageAccent = Color(0xFF8878FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeuroChatGptScreen() }
    }
}

@Composable
private fun NeuroChatGptScreen() {
    var webView by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var pageError by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = webView?.canGoBack() == true) {
        webView?.goBack()
    }

    MaterialTheme(colorScheme = darkColorScheme(
        primary = pageAccent,
        background = pageBg,
        surface = Color(0xFF151D32)
    )) {
        Column(
            modifier = Modifier.fillMaxSize().background(pageBg)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF11182A))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("NEURO AI", color = Color.White, fontSize = 19.sp)
                    Text("На базе веб-версии ChatGPT", color = Color(0xFFABB4C8), fontSize = 11.sp)
                }
                Button(onClick = {
                    pageError = null
                    webView?.reload()
                }) { Text("Обновить") }
            }

            if (progress in 1..99) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = pageAccent
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            webView = this
                            setBackgroundColor(android.graphics.Color.rgb(11, 16, 32))

                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.databaseEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.javaScriptCanOpenWindowsAutomatically = false
                            settings.setSupportMultipleWindows(false)
                            settings.allowFileAccess = false
                            settings.allowContentAccess = false
                            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW

                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    progress = newProgress
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    pageError = null
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    progress = 100
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val uri = request?.url ?: return false
                                    return when (uri.scheme?.lowercase()) {
                                        "http", "https" -> false
                                        else -> {
                                            runCatching {
                                                context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                            }
                                            true
                                        }
                                    }
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        pageError = "Не удалось загрузить ChatGPT. Проверьте интернет и повторите попытку."
                                    }
                                }

                                override fun onReceivedSslError(
                                    view: WebView?,
                                    handler: SslErrorHandler?,
                                    error: android.net.http.SslError?
                                ) {
                                    handler?.cancel()
                                    pageError = "Не удалось безопасно установить соединение."
                                }
                            }

                            loadUrl("https://chatgpt.com/")
                        }
                    },
                    update = { webView = it }
                )

                if (pageError != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        color = Color(0xFF202A40),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(pageError.orEmpty(), color = Color.White)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = {
                                pageError = null
                                webView?.reload()
                            }) { Text("Повторить") }
                            Spacer(Modifier.height(6.dp))
                            Button(onClick = {
                                runCatching {
                                    webView?.context?.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://chatgpt.com/"))
                                    )
                                }
                            }) { Text("Открыть в браузере") }
                        }
                    }
                }
            }
        }
    }
}
