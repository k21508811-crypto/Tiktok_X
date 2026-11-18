package ai.neuro.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

private val bg = Color(0xFF08070E)
private val panel = Color(0xFF191426)
private val purple = Color(0xFF9B55FF)
private val muted = Color(0xFFB7AEC9)
private data class Service(val title: String, val url: String, val subtitle: String)
private val services = listOf(
    Service("ChatGPT", "https://chatgpt.com/", "Встроенный веб-чат ChatGPT"),
    Service("Cloud", "https://www.cloud.com/", "Официальный сайт Cloud"),
    Service("Алиса AI", "https://alice.yandex.ru/", "ИИ-помощник Яндекса")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = purple, background = bg, surface = panel,
                onPrimary = Color.White, onBackground = Color.White, onSurface = Color.White
            )) { NeuroApp() }
        }
    }
}

@Composable
private fun NeuroApp() {
    var intro by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf("ChatGPT") }
    var premiumInfo by remember { mutableStateOf(false) }
    var currentWebView by remember { mutableStateOf<WebView?>(null) }

    BackHandler(enabled = currentWebView?.canGoBack() == true && !intro && selected != "PREMIUM") {
        currentWebView?.goBack()
    }

    if (intro) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF05040A), Color(0xFF170D2B), Color(0xFF05040A)))
            ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(28.dp)) {
                Image(painterResource(R.drawable.neuro_icon), "NEURO AI", Modifier.size(250.dp), contentScale = ContentScale.Fit)
                Spacer(Modifier.height(22.dp))
                Text("ИНТЕЛЛЕКТ. ВОЗМОЖНОСТИ. БУДУЩЕЕ.", color = Color(0xFFD8C6FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(44.dp))
                Button(
                    onClick = { intro = false },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = purple)
                ) { Text("ПРОДОЛЖИТЬ", fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp) }
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(bg).padding(top = 12.dp, start = 14.dp, end = 14.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                Image(painterResource(R.drawable.neuro_icon), "NEURO AI", Modifier.size(48.dp), contentScale = ContentScale.Crop)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text("NEURO AI", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Интеллект. Возможности. Будущее.", fontSize = 10.sp, color = muted)
                }
                Text("PRO", color = Color(0xFFD5B3FF), fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text("ИИ-СЕРВИСЫ", color = muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        services.forEach { service ->
                            ServiceTile(service.title, selected == service.title, Modifier.weight(1f)) {
                                currentWebView = null
                                selected = service.title
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            currentWebView = null
                            selected = "PREMIUM"
                            premiumInfo = true
                        },
                        shape = RoundedCornerShape(17.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF241632))
                    ) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🔒", fontSize = 23.sp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("PREMIUM CHAT GPT", fontWeight = FontWeight.ExtraBold, color = Color.White, fontSize = 14.sp)
                                Text("Расширенный доступ • только PRO", color = muted, fontSize = 11.sp)
                            }
                            Text("ЗАМОК", color = Color(0xFFD5B3FF), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }
                item {
                    Text(
                        when (selected) { "ChatGPT" -> "CHATGPT"; "Cloud" -> "CLOUD"; "Алиса AI" -> "АЛИСА AI"; else -> "PREMIUM CHAT GPT" },
                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                    )
                    Text(
                        services.firstOrNull { it.title == selected }?.subtitle ?: "Раздел закрыт до подтверждения PRO-подписки",
                        color = muted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp, bottom = 8.dp)
                    )
                }
                item {
                    if (selected == "PREMIUM") LockedPremium { premiumInfo = true }
                    else {
                        val url = services.firstOrNull { it.title == selected }?.url ?: services[0].url
                        WebPanel(url) { currentWebView = it }
                    }
                }
            }
            Text(
                "NEURO AI  •  СТОРОННИЕ СЕРВИСЫ ОТКРЫВАЮТСЯ В WEBVIEW",
                color = Color(0xFF756D83), fontSize = 8.sp,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }

    if (premiumInfo) {
        AlertDialog(
            onDismissRequest = { premiumInfo = false },
            containerColor = panel,
            title = { Text("PREMIUM CHAT GPT", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Этот раздел закрыт. Он откроется только после подтверждения активной PRO-подписки. Оплата RuStore и серверная проверка подписки пока не подключены, поэтому доступ не будет разблокирован фиктивно.",
                    color = muted
                )
            },
            confirmButton = { TextButton(onClick = { premiumInfo = false }) { Text("ПОНЯТНО", color = purple) } }
        )
    }
}

@Composable
private fun ServiceTile(title: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Card(
        modifier = modifier.height(58.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFF382052) else panel)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(title, color = if (active) Color.White else muted, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebPanel(url: String, onCreated: (WebView) -> Unit) {
    val context = LocalContext.current
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(540.dp),
        factory = {
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.loadsImagesAutomatically = true
                settings.javaScriptCanOpenWindowsAutomatically = true
                settings.setSupportMultipleWindows(false)
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                onCreated(this)
                loadUrl(url)
            }
        },
        update = { onCreated(it) }
    )
}

@Composable
private fun LockedPremium(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = panel)
    ) {
        Box(Modifier.fillMaxSize().padding(22.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔒", fontSize = 42.sp)
                Spacer(Modifier.height(12.dp))
                Text("PREMIUM CHAT GPT", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                Text("Доступ только для подписчиков PRO", color = muted, fontSize = 12.sp)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = purple)) {
                    Text("ПОДРОБНЕЕ О PRO")
                }
            }
        }
    }
}
