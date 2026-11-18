package ai.neuro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

private val bg = Color(0xFF0B1020)
private val panel = Color(0xFF151D32)
private val accent = Color(0xFF7C6CFF)
private val fg = Color(0xFFF4F5FA)
private val muted = Color(0xFF9AA5BD)
private data class Line(val role: String, val content: String)
private val client = OkHttpClient.Builder().connectTimeout(20, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS).build()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NeuroScreen() }
    }
}

private suspend fun sendChat(lines: List<Line>): String = withContext(Dispatchers.IO) {
    val arr = JSONArray()
    lines.forEach { arr.put(JSONObject().put("role", it.role).put("content", it.content)) }
    val body = JSONObject().put("messages", arr).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
    val url = BuildConfig.API_BASE_URL.trimEnd('/') + "/api/v1/chat"
    client.newCall(Request.Builder().url(url).post(body).build()).execute().use { res ->
        val raw = res.body?.string().orEmpty()
        if (!res.isSuccessful) {
            val detail = runCatching { JSONObject(raw).optString("detail") }.getOrDefault("")
            error(detail.ifBlank { "Server error: ${res.code}" })
        }
        JSONObject(raw).getString("reply")
    }
}

@Composable
private fun NeuroScreen() {
    val lines = remember { mutableStateListOf<Line>() }
    var input by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    MaterialTheme(colorScheme = darkColorScheme(primary = accent, background = bg, surface = panel)) {
        Column(Modifier.fillMaxSize().background(bg).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent, shape = RoundedCornerShape(14.dp)) {
                    Text("N", Modifier.padding(horizontal = 14.dp, vertical = 7.dp), color = Color.White, fontSize = 24.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("NEURO AI", color = fg, fontSize = 21.sp)
                    Text("Ваш интеллектуальный помощник", color = muted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(18.dp))
            if (lines.isEmpty()) {
                Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("С чего начнём?", color = fg, fontSize = 27.sp)
                    Text("Задайте вопрос NEURO AI", color = muted)
                }
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(lines) { line ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.role == "user") Arrangement.End else Arrangement.Start) {
                            Surface(color = if (line.role == "user") accent.copy(alpha = .22f) else panel,
                                shape = RoundedCornerShape(18.dp), modifier = Modifier.widthIn(max = 320.dp)) {
                                Text(line.content, Modifier.padding(14.dp), color = fg)
                            }
                        }
                    }
                    if (busy) item { Text("NEURO AI печатает…", color = muted) }
                }
            }
            errorText?.let { Text(it, color = Color(0xFFFF8585), fontSize = 12.sp) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(input, { input = it }, Modifier.weight(1f), enabled = !busy,
                    placeholder = { Text("Напишите сообщение…", color = muted) }, maxLines = 4,
                    shape = RoundedCornerShape(18.dp))
                Spacer(Modifier.width(8.dp))
                Button(enabled = input.isNotBlank() && !busy, onClick = {
                    val prompt = input.trim()
                    input = ""
                    errorText = null
                    lines.add(Line("user", prompt))
                    busy = true
                    scope.launch {
                        try { lines.add(Line("assistant", sendChat(lines.toList()))) }
                        catch (e: Exception) { errorText = e.message ?: "Не удалось получить ответ." }
                        finally { busy = false }
                    }
                }, contentPadding = PaddingValues(16.dp), shape = RoundedCornerShape(16.dp)) { Text("➤") }
            }
            Text("Проверяйте важную информацию: ответы ИИ могут содержать ошибки.",
                color = muted, fontSize = 10.sp, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp))
        }
    }
}
