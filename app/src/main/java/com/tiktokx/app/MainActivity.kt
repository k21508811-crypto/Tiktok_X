package com.tiktokx.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

private val AppBg = Color(0xFF08080B)
private val AppAccent = Color(0xFFFE2C55)
private val AppSurface = Color(0xFF17171D)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.BLACK
        window.navigationBarColor = android.graphics.Color.BLACK
        setContent { TikTokXRoot() }
    }
}

private data class AppUser(val id: Long, val username: String, val email: String, val displayName: String = "", val bio: String = "", val verified: Boolean = false)
private data class AppVideo(val id: Long, val username: String, val caption: String, val url: String, val likes: Int, val comments: Int, val views: Long)

@Composable
private fun TikTokXRoot() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tiktokx_settings", 0) }
    var apiBase by remember { mutableStateOf(prefs.getString("api_base", "") ?: "") }
    var token by remember { mutableStateOf(prefs.getString("auth_token", null)) }
    var user by remember { mutableStateOf<AppUser?>(null) }

    MaterialTheme(colorScheme = darkColorScheme(primary = AppAccent, background = AppBg, surface = AppSurface)) {
        if (token.isNullOrBlank()) {
            AuthScreen(
                initialBase = apiBase,
                onServerChange = { apiBase = it; prefs.edit().putString("api_base", it).apply() },
                onAuthenticated = { newToken, newUser ->
                    token = newToken
                    user = newUser
                    prefs.edit().putString("auth_token", newToken).apply()
                }
            )
        } else {
            MainScreen(
                base = apiBase,
                token = token!!,
                initialUser = user,
                onLogout = {
                    token = null
                    user = null
                    prefs.edit().remove("auth_token").apply()
                },
                onUserLoaded = { user = it }
            )
        }
    }
}

@Composable
private fun AuthScreen(
    initialBase: String,
    onServerChange: (String) -> Unit,
    onAuthenticated: (String, AppUser) -> Unit
) {
    val scope = rememberCoroutineScope()
    var base by remember { mutableStateOf(initialBase) }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var registering by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("Укажи адрес API своего сайта на REG.RU, например https://example.ru/api") }

    Surface(Modifier.fillMaxSize(), color = AppBg) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("TikTok X", color = Color.White, style = MaterialTheme.typography.headlineLarge)
            Text(if (registering) "Создание аккаунта" else "Вход в аккаунт", color = Color.LightGray)
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = base, onValueChange = { base = it; onServerChange(it) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Адрес API") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = username, onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text(if (registering) "Username (латиница)" else "Username или Email") }
            )
            if (registering) {
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = email, onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Email") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Пароль (от 10 символов при регистрации)") },
                visualTransformation = PasswordVisualTransformation()
            )
            Spacer(Modifier.height(16.dp))
            Button(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        message = "Подключение к серверу..."
                        try {
                            val result = withContext(Dispatchers.IO) {
                                if (registering) Api.register(base, username.trim(), email.trim(), password)
                                else Api.login(base, username.trim(), password)
                            }
                            onAuthenticated(result.first, result.second)
                            message = ""
                        } catch (e: Exception) {
                            message = e.message ?: "Не удалось подключиться"
                        } finally { busy = false }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AppAccent)
            ) { Text(if (busy) "Подождите..." else if (registering) "Зарегистрироваться" else "Войти") }
            TextButton(onClick = { registering = !registering; message = "" }) {
                Text(if (registering) "Уже есть аккаунт? Войти" else "Создать аккаунт", color = Color.White)
            }
            if (message.isNotBlank()) Text(message, color = Color.LightGray, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    base: String, token: String, initialUser: AppUser?,
    onLogout: () -> Unit, onUserLoaded: (AppUser) -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tab by remember { mutableIntStateOf(0) }
    var videos by remember { mutableStateOf<List<AppVideo>>(emptyList()) }
    var searchText by remember { mutableStateOf("") }
    var users by remember { mutableStateOf<List<AppUser>>(emptyList()) }
    var currentUser by remember { mutableStateOf(initialUser) }
    var status by remember { mutableStateOf("Загружаем данные...") }
    var busy by remember { mutableStateOf(false) }
    var selectedVideo by remember { mutableStateOf<Uri?>(null) }
    var captionDraft by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selectedVideo = it }

    fun loadFeed() {
        scope.launch {
            busy = true
            try {
                videos = withContext(Dispatchers.IO) { Api.feed(base, token) }
                status = if (videos.isEmpty()) "Пока нет опубликованных видео. Загрузи первый ролик." else ""
            } catch (e: Exception) { status = e.message ?: "Ошибка загрузки ленты" }
            finally { busy = false }
        }
    }

    LaunchedEffect(base, token) {
        try {
            val me = withContext(Dispatchers.IO) { Api.me(base, token) }
            currentUser = me
            onUserLoaded(me)
        } catch (_: Exception) { }
        loadFeed()
    }

    Scaffold(
        containerColor = AppBg,
        topBar = {
            TopAppBar(
                title = { Text("TikTok X", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppBg),
                actions = {
                    IconButton(onClick = { picker.launch(arrayOf("video/*")) }) { Icon(Icons.Default.Add, "Загрузить видео", tint = Color.White) }
                    IconButton(onClick = { loadFeed() }) { Icon(Icons.Default.Refresh, "Обновить", tint = Color.White) }
                    IconButton(onClick = onLogout) { Icon(Icons.Default.Logout, "Выйти", tint = Color.White) }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = AppSurface) {
                listOf("Лента", "Поиск", "Профиль").forEachIndexed { i, label ->
                    val icon = when (i) { 0 -> Icons.Default.Home; 1 -> Icons.Default.Search; else -> Icons.Default.Person }
                    NavigationBarItem(selected = tab == i, onClick = { tab = i }, icon = { Icon(icon, null) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when (tab) {
                0 -> {
                    if (status.isNotBlank()) Text(status, color = Color.LightGray, modifier = Modifier.padding(vertical = 14.dp))
                    LazyColumn {
                        items(videos, key = { it.id }) { video ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = AppSurface)) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("@${video.username}", color = Color.White, style = MaterialTheme.typography.titleMedium)
                                    Text(video.caption, color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                                    if (video.url.isNotBlank()) {
                                        Text("Открыть видео", color = AppAccent, modifier = Modifier.padding(top = 10.dp).clickable {
                                            runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.url))) }
                                        })
                                    }
                                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("♥ ${video.likes}", color = Color.White, modifier = Modifier.clickable {
                                            scope.launch { runCatching { withContext(Dispatchers.IO) { Api.like(base, token, video.id) }; loadFeed() }.onFailure { status = it.message ?: "Ошибка лайка" } }
                                        })
                                        Text("Комментарии: ${video.comments}", color = Color.White)
                                        Text("Просмотры: ${video.views}", color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    OutlinedTextField(searchText, { searchText = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Имя пользователя") })
                    Button(onClick = {
                        scope.launch {
                            try { users = withContext(Dispatchers.IO) { Api.search(base, token, searchText) }; status = "" }
                            catch (e: Exception) { status = e.message ?: "Ошибка поиска" }
                        }
                    }, colors = ButtonDefaults.buttonColors(containerColor = AppAccent), modifier = Modifier.padding(top = 8.dp)) { Text("Найти") }
                    LazyColumn {
                        items(users) { found -> Text("@${found.username}  ${found.displayName}", color = Color.White, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) }
                    }
                    if (status.isNotBlank()) Text(status, color = Color.LightGray)
                }
                else -> {
                    Text(currentUser?.displayName?.ifBlank { currentUser?.username ?: "Профиль" } ?: "Профиль", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text("@${currentUser?.username ?: ""}", color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                    Text(currentUser?.email ?: "", color = Color.Gray)
                    Text(currentUser?.bio ?: "", color = Color.LightGray, modifier = Modifier.padding(top = 12.dp))
                    if (currentUser?.verified == true) Text("Подтверждённый аккаунт", color = Color(0xFF25F4EE))
                    Text("Аккаунт загружается с сервера. Профиль не подменяется тестовыми данными.", color = Color.Gray, modifier = Modifier.padding(top = 18.dp))
                }
            }
        }
    }
    selectedVideo?.let { uri ->
        AlertDialog(
            onDismissRequest = { selectedVideo = null },
            title = { Text("Публикация видео") },
            text = { OutlinedTextField(captionDraft, { captionDraft = it }, label = { Text("Описание") }, maxLines = 4) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            withContext(Dispatchers.IO) { Api.uploadVideo(context, base, token, uri, captionDraft) }
                            selectedVideo = null
                            captionDraft = ""
                            loadFeed()
                            status = "Видео опубликовано"
                        } catch (e: Exception) {
                            status = e.message ?: "Ошибка загрузки"
                        } finally { busy = false }
                    }
                }) { Text(if (busy) "Загрузка..." else "Опубликовать", color = AppAccent) }
            },
            dismissButton = { TextButton(onClick = { selectedVideo = null }) { Text("Отмена") } }
        )
    }
}

private object Api {
    private fun endpoint(base: String, path: String): String {
        val root = base.trim().trimEnd('/')
        require(root.startsWith("https://")) { "Адрес API должен начинаться с https://" }
        return root + path
    }

    private fun request(base: String, path: String, method: String, body: JSONObject? = null, token: String? = null): JSONObject {
        val c = URL(endpoint(base, path)).openConnection() as HttpURLConnection
        c.requestMethod = method
        c.connectTimeout = 15000
        c.readTimeout = 20000
        c.setRequestProperty("Accept", "application/json")
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        if (!token.isNullOrBlank()) c.setRequestProperty("Authorization", "Bearer $token")
        if (body != null) {
            c.doOutput = true
            c.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
        }
        val code = c.responseCode
        val stream = if (code in 200..299) c.inputStream else c.errorStream
        val raw = stream?.bufferedReader()?.use { it.readText() } ?: ""
        c.disconnect()
        val json = try { JSONObject(raw) } catch (_: Exception) { throw IllegalStateException("Сервер вернул HTTP $code без JSON-ответа") }
        if (code !in 200..299 || !json.optBoolean("ok", false)) throw IllegalStateException(json.optString("error", "HTTP $code"))
        return json
    }

    fun login(base: String, login: String, password: String): Pair<String, AppUser> {
        val r = request(base, "/login", "POST", JSONObject().put("login", login).put("password", password))
        return parseAuth(r)
    }
    fun register(base: String, username: String, email: String, password: String): Pair<String, AppUser> {
        val r = request(base, "/register", "POST", JSONObject().put("username", username).put("email", email).put("password", password))
        return parseAuth(r)
    }
    private fun parseAuth(r: JSONObject): Pair<String, AppUser> {
        val u = r.getJSONObject("user")
        return r.getString("token") to AppUser(u.getLong("id"), u.getString("username"), u.optString("email"))
    }
    fun me(base: String, token: String): AppUser {
        val u = request(base, "/me", "GET", token = token).getJSONObject("user")
        return AppUser(u.getLong("id"), u.getString("username"), u.optString("email"), u.optString("display_name"), u.optString("bio"), u.optInt("is_verified") == 1)
    }
    fun feed(base: String, token: String): List<AppVideo> {
        val a = request(base, "/feed", "GET", token = token).optJSONArray("videos") ?: return emptyList()
        return buildList { for (i in 0 until a.length()) { val v = a.getJSONObject(i); add(AppVideo(v.getLong("id"), v.optString("username"), v.optString("caption"), v.optString("video_path"), v.optInt("likes_count"), v.optInt("comments_count"), v.optLong("views_count"))) } }
    }
    fun search(base: String, token: String, query: String): List<AppUser> {
        val a = request(base, "/search?q=" + URLEncoder.encode(query, "UTF-8"), "GET", token = token).optJSONArray("users") ?: return emptyList()
        return buildList { for (i in 0 until a.length()) { val u = a.getJSONObject(i); add(AppUser(u.getLong("id"), u.optString("username"), "", u.optString("display_name"), verified = u.optInt("is_verified") == 1)) } }
    }
    fun uploadVideo(context: Context, base: String, token: String, uri: Uri, caption: String) {
        val boundary = "TikTokX-" + System.currentTimeMillis()
        val c = URL(endpoint(base, "/upload-video")).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 20000
        c.readTimeout = 120000
        c.doOutput = true
        c.setRequestProperty("Authorization", "Bearer $token")
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        c.setChunkedStreamingMode(65536)
        c.outputStream.use { out ->
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n$caption\r\n".toByteArray(Charsets.UTF_8))
            val mime = context.contentResolver.getType(uri) ?: "video/mp4"
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"video\"; filename=\"clip\"\r\nContent-Type: $mime\r\n\r\n".toByteArray(Charsets.UTF_8))
            context.contentResolver.openInputStream(uri)?.use { it.copyTo(out, 65536) } ?: throw IllegalStateException("Не удалось прочитать видео")
            out.write("\r\n--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        }
        val code = c.responseCode
        val raw = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        c.disconnect()
        val json = try { JSONObject(raw) } catch (_: Exception) { throw IllegalStateException("Сервер вернул HTTP $code") }
        if (code !in 200..299 || !json.optBoolean("ok", false)) throw IllegalStateException(json.optString("error", "Ошибка загрузки"))
    }

    fun like(base: String, token: String, id: Long) { request(base, "/videos/$id/like", "POST", JSONObject(), token) }
}
