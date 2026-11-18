package ai.neuro.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

private val backgroundTop = Color(0xFF090B14)
private val backgroundBottom = Color(0xFF171126)
private val violet = Color(0xFF9B72FF)
private val lavender = Color(0xFFC7A8FF)
private val muted = Color(0xFFA9A4B8)

private enum class AppSection(val title: String) {
    HOME("Главная"),
    CLOUD("CLOUD"),
    ALICE("АЛИСА AI"),
    CHATGPT("CHATGPT")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = violet,
                    background = backgroundTop,
                    surface = Color(0xFF1B1728),
                    onPrimary = Color.White,
                    onBackground = Color.White,
                    onSurface = Color.White
                )
            ) {
                NeuroApp()
            }
        }
    }
}

@Composable
private fun NeuroApp() {
    var section by remember { mutableStateOf(AppSection.HOME) }
    var showProInfo by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundTop, Color(0xFF11101D), backgroundBottom)
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Brush.linearGradient(listOf(violet, Color(0xFF5435A6))), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("N", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(11.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("AI NEURO", fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                Text("ИНТЕЛЛЕКТ НОВОГО ПОКОЛЕНИЯ", fontSize = 9.sp, color = muted, letterSpacing = 1.sp)
            }
            Surface(color = Color(0xFF302342), shape = RoundedCornerShape(50)) {
                Text(
                    "BETA",
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 7.dp),
                    color = lavender,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }
        }

        when (section) {
            AppSection.HOME -> HomeScreen(onProClick = { showProInfo = true })
            AppSection.CLOUD -> WebServiceScreen(
                title = "CLOUD",
                subtitle = "Официальный сайт Cloud.com",
                url = "https://www.cloud.com"
            )
            AppSection.ALICE -> WebServiceScreen(
                title = "АЛИСА AI",
                subtitle = "Нейросеть Яндекса",
                url = "https://alice.yandex.ru"
            )
            AppSection.CHATGPT -> LockedPremiumScreen(onUnlockClick = { showProInfo = true })
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF100E1B))
                .padding(horizontal = 5.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppSection.values().forEach { item ->
                val selected = section == item
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            if (item == AppSection.CHATGPT) showProInfo = true else section = item
                        }
                        .padding(vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        when (item) {
                            AppSection.HOME -> "⌂"
                            AppSection.CLOUD -> "☁"
                            AppSection.ALICE -> "✦"
                            AppSection.CHATGPT -> "▣"
                        },
                        color = if (selected) lavender else muted,
                        fontSize = 19.sp
                    )
                    Text(
                        if (item == AppSection.CHATGPT) "PREMIUM" else item.title,
                        color = if (selected) lavender else muted,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                    )
                    if (item == AppSection.CHATGPT) {
                        Text("🔒", fontSize = 9.sp)
                    }
                }
            }
        }
    }

    if (showProInfo) {
        AlertDialog(
            onDismissRequest = { showProInfo = false },
            containerColor = Color(0xFF211B31),
            title = { Text("PREMIUM CHATGPT", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Раздел ChatGPT закрыт для бесплатного доступа. Он будет открываться только после подтверждения активной AI Neuro PRO подписки. Оплата и серверная проверка подписки пока не подключены.",
                    color = Color(0xFFD8D2E5)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showProInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = violet)
                ) { Text("ПОНЯТНО") }
            }
        )
    }
}

@Composable
private fun HomeScreen(onProClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(104.dp)
                .shadow(24.dp, CircleShape)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF5B3BA0), Color(0xFF241C3D), Color(0xFF171426))),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("✦", color = lavender, fontSize = 55.sp)
        }
        Spacer(Modifier.height(24.dp))
        Text("AI Neuro", color = Color.White, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Ваш интеллектуальный помощник", color = muted, fontSize = 14.sp)
        Spacer(Modifier.height(25.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xCC201B30),
            shape = RoundedCornerShape(22.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Здравствуйте!", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    Text("БЕТА-ТЕСТ", color = Color(0xFFFFC66D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Это бета-версия AI Neuro. Основной ИИ-чат временно недоступен. Используйте разделы CLOUD и Алиса AI в нижнем меню.",
                    color = Color(0xFFE0DCEB),
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF171426),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(Modifier.padding(17.dp)) {
                Text("AI NEURO PRO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(Modifier.height(7.dp))
                Text(
                    "Премиум-доступ и закрытый раздел ChatGPT.",
                    color = muted,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = onProClick,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = violet)
                ) {
                    Text("ОФОРМИТЬ PRO", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            "AI NEURO • ВАШ ИНТЕЛЛЕКТ БЕЗ ГРАНИЦ",
            color = Color(0xFF777184),
            fontSize = 9.sp,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 8.dp)
        )
    }
}

@Composable
private fun LockedPremiumScreen(onUnlockClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", fontSize = 54.sp)
        Spacer(Modifier.height(18.dp))
        Text("PREMIUM CHATGPT", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text(
            "Доступ к ChatGPT доступен только подписчикам AI Neuro PRO.",
            color = muted,
            textAlign = TextAlign.Center,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(22.dp))
        Button(onClick = onUnlockClick, colors = ButtonDefaults.buttonColors(containerColor = violet)) {
            Text("ОТКРЫТЬ PRO")
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebServiceScreen(title: String, subtitle: String, url: String) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = muted, fontSize = 12.sp)
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.loadsImagesAutomatically = true
                    settings.javaScriptCanOpenWindowsAutomatically = false
                    webChromeClient = WebChromeClient()
                    webViewClient = WebViewClient()
                    loadUrl(url)
                }
            },
            update = { view ->
                if (view.tag != url) {
                    view.tag = url
                    view.loadUrl(url)
                }
            }
        )
    }
}
