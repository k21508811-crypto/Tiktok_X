package ai.neuro.app

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Background = Color(0xFF09070F)
private val SurfaceDark = Color(0xFF191321)
private val Accent = Color(0xFFB65CFF)
private val TextMain = Color(0xFFF7F0FF)
private val TextMuted = Color(0xFFC5B9D1)

private data class AiService(
    val name: String,
    val url: String,
    val description: String,
    val category: String,
    val symbol: String
)

private val serviceList = listOf(
    AiService("ChatGPT", "https://chatgpt.com/", "Универсальный ИИ-помощник", "Чаты", "C"),
    AiService("Gemini", "https://gemini.google.com/app", "ИИ от Google", "Чаты", "G"),
    AiService("Copilot", "https://copilot.microsoft.com/", "ИИ от Microsoft", "Чаты", "C"),
    AiService("DeepSeek", "https://chat.deepseek.com/", "Диалог и анализ", "Чаты", "D"),
    AiService("Perplexity", "https://www.perplexity.ai/", "Поиск с источниками", "Поиск", "P"),
    AiService("Qwen", "https://chat.qwen.ai/", "Модели Qwen", "Чаты", "Q"),
    AiService("Mistral", "https://chat.mistral.ai/", "Le Chat от Mistral", "Чаты", "M"),
    AiService("HuggingChat", "https://huggingface.co/chat/", "Открытые модели", "Чаты", "H"),
    AiService("Claude", "https://claude.ai/", "ИИ от Anthropic", "Чаты", "C"),
    AiService("Grok", "https://grok.com/", "ИИ от xAI", "Чаты", "G"),
    AiService("Алиса AI", "https://alice.yandex.ru/", "ИИ от Яндекса", "Чаты", "А"),
    AiService("Poe", "https://poe.com/", "Каталог ИИ-ботов", "Чаты", "P"),
    AiService("Duck.ai", "https://duck.ai/", "ИИ-чат DuckDuckGo", "Приватность", "D")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val colors = darkColorScheme(
                primary = Accent,
                onPrimary = Color.White,
                background = Background,
                onBackground = TextMain,
                surface = SurfaceDark,
                onSurface = TextMain,
                surfaceVariant = Color(0xFF30213E),
                onSurfaceVariant = TextMuted,
                secondary = Color(0xFFD6A6FF)
            )
            MaterialTheme(colorScheme = colors) {
                Surface(Modifier.fillMaxSize(), color = Background) {
                    NeuroDirectory()
                }
            }
        }
    }
}

@Composable
private fun NeuroDirectory() {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("neuro_ai", Context.MODE_PRIVATE) }
    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Все") }
    var favoritesOnly by remember { mutableStateOf(false) }
    var showPrivacy by remember { mutableStateOf(false) }
    var favorites by remember {
        mutableStateOf(preferences.getStringSet("favorites", emptySet())?.toSet() ?: emptySet())
    }

    val filtered = serviceList.filter { service ->
        val categoryMatches = category == "Все" || service.category == category
        val searchMatches = service.name.contains(search, true) ||
            service.description.contains(search, true)
        categoryMatches && searchMatches && (!favoritesOnly || favorites.contains(service.name))
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF090711), Background, Color(0xFF12091B))))
            .statusBarsPadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(50.dp).background(
                    Brush.linearGradient(listOf(Accent, Color(0xFF35105D))),
                    RoundedCornerShape(16.dp)
                ),
                contentAlignment = Alignment.Center
            ) {
                Text("N", fontSize = 29.sp, fontWeight = FontWeight.Black, color = Color.White)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("NEURO AI", fontSize = 22.sp, fontWeight = FontWeight.Black, color = TextMain, letterSpacing = 1.sp)
                Text("ТВОЯ ПАНЕЛЬ ИИ-СЕРВИСОВ", fontSize = 9.sp, color = TextMuted, letterSpacing = 1.sp)
            }
            TextButton(onClick = { showPrivacy = true }) {
                Text("ПРИВАТНОСТЬ", color = Color(0xFFD8B6FF), fontSize = 10.sp)
            }
        }

        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(Color(0xFF35134E), Color(0xFF17101F), Color(0xFF241039))),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(21.dp)
                ) {
                    Text("NEON AI DIRECTORY  /  BETA", color = Color(0xFFE2C9FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Spacer(Modifier.height(12.dp))
                    Text("Найди своего\nИИ-помощника", fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.ExtraBold, color = TextMain)
                    Spacer(Modifier.height(9.dp))
                    Text("Каталог ИИ-сервисов с поиском, категориями и избранным.", fontSize = 13.sp, lineHeight = 19.sp, color = TextMuted)
                }
            }

            item {
                Text("ПОИСК ПО КАТАЛОГУ", fontSize = 10.sp, color = TextMuted, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth().background(SurfaceDark, RoundedCornerShape(16.dp)).padding(15.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("⌕", fontSize = 23.sp, color = Accent)
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        if (search.isEmpty()) Text("Название сервиса или задача…", color = Color(0xFF94889F), fontSize = 13.sp)
                        BasicTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            textStyle = TextStyle(color = TextMain, fontSize = 14.sp)
                        )
                    }
                    if (search.isNotEmpty()) Text("×", Modifier.clickable { search = "" }, color = TextMuted, fontSize = 20.sp)
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("Все", "Чаты", "Поиск", "Приватность").forEach { item ->
                        val selected = category == item
                        Surface(
                            color = if (selected) Color(0xFF48216A) else SurfaceDark,
                            shape = RoundedCornerShape(30.dp),
                            modifier = Modifier.clickable { category = item }
                        ) {
                            Text(
                                item,
                                Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                                color = if (selected) Color.White else TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("ИИ-СЕРВИСЫ", fontSize = 18.sp, color = TextMain, fontWeight = FontWeight.ExtraBold)
                        Text("Открытие происходит в браузере", fontSize = 11.sp, color = TextMuted)
                    }
                    TextButton(onClick = { favoritesOnly = !favoritesOnly }) {
                        Text(if (favoritesOnly) "ВСЕ СЕРВИСЫ" else "★ ИЗБРАННОЕ", color = Color(0xFFD7B4FF), fontSize = 10.sp)
                    }
                }
            }

            items(filtered, key = { it.name }) { service ->
                val isFavorite = favorites.contains(service.name)
                Row(
                    Modifier.fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF251832), Color(0xFF191321))),
                            RoundedCornerShape(18.dp)
                        )
                        .clickable {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(service.url)))
                            } catch (_: ActivityNotFoundException) {
                                Toast.makeText(context, "Не найден браузер для открытия ссылки", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(45.dp).background(Color(0xFF352344), RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(service.symbol, color = Color(0xFFE7CCFF), fontSize = 20.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(service.name, color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(service.description, color = TextMuted, fontSize = 11.sp)
                    }
                    Text(
                        if (isFavorite) "★" else "☆",
                        Modifier.clickable {
                            val updated = if (isFavorite) favorites - service.name else favorites + service.name
                            favorites = updated
                            preferences.edit().putStringSet("favorites", updated).apply()
                        }.padding(horizontal = 10.dp, vertical = 7.dp),
                        color = Color(0xFFD5B0FF),
                        fontSize = 23.sp
                    )
                    Text("↗", color = Color(0xFFD5B0FF), fontSize = 17.sp)
                }
            }

            if (filtered.isEmpty()) {
                item { Text("Ничего не найдено. Измени запрос или категорию.", color = TextMuted, modifier = Modifier.padding(12.dp)) }
            }

            item {
                Column(Modifier.fillMaxWidth().background(Color(0xFF130F1B), RoundedCornerShape(17.dp)).padding(15.dp)) {
                    Text("ВАЖНО О ПРИВАТНОСТИ", fontSize = 11.sp, color = Color(0xFFE2C9FF), fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "NEURO AI — независимый каталог, а не официальный клиент перечисленных компаний. При открытии сервиса вы переходите на его сайт в выбранном браузере. Cookie и входом управляет сам сайт и браузер.",
                        fontSize = 11.sp, color = TextMuted, lineHeight = 16.sp
                    )
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(Color(0xFF0C0911)).navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("NEURO AI  •  BETA", fontSize = 10.sp, color = Color(0xFF9B90A6))
            TextButton(onClick = { showPrivacy = true }) {
                Text("COOKIE И ДАННЫЕ", fontSize = 10.sp, color = Color(0xFFCDA5FF))
            }
        }
    }

    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            containerColor = Color(0xFF21162D),
            title = { Text("Приватность и данные", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Приложение хранит только список избранных сервисов на устройстве. Оно не встраивает сторонние сайты, не читает их cookie и не запрашивает доступ к контактам, файлам, микрофону, камере или геолокации. При переходе по ссылке данные обрабатываются выбранным браузером и владельцем сайта.",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacy = false }) {
                    Text("ПОНЯТНО", color = Color(0xFFD9B4FF))
                }
            }
        )
    }
}
