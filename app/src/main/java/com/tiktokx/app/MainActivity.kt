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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.runtime.DisposableEffect
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
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
private data class AppComment(val id: Long, val username: String, val body: String)
private data class AppMusic(val id: Long, val username: String, val title: String, val url: String)

@Composable
private fun VideoPlayback(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playWhenReady = true
            prepare()
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth().aspectRatio(9f / 16f).padding(top = 10.dp),
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                this.player = player
                useController = true
                setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
            }
        },
        update = { it.player = player }
    )
}

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
    var selectedAudio by remember { mutableStateOf<Uri?>(null) }
    var audioTitle by remember { mutableStateOf("") }
    var musicItems by remember { mutableStateOf<List<AppMusic>>(emptyList()) }
    var captionDraft by remember { mutableStateOf("") }
    var commentVideo by remember { mutableStateOf<AppVideo?>(null) }
    var commentItems by remember { mutableStateOf<List<AppComment>>(emptyList()) }
    var commentDraft by remember { mutableStateOf("") }
    var displayNameDraft by remember { mutableStateOf(initialUser?.displayName ?: "") }
    var bioDraft by remember { mutableStateOf(initialUser?.bio ?: "") }
    var verificationReason by remember { mutableStateOf("") }
    var digitalId by remember { mutableStateOf("") }
    var businessName by remember { mutableStateOf("") }
    var businessCategory by remember { mutableStateOf("") }
    var businessDescription by remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selectedVideo = it }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { selectedAudio = it }

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
            displayNameDraft = me.displayName
            bioDraft = me.bio
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
                listOf("Лента", "Поиск", "Музыка", "Профиль").forEachIndexed { i, label ->
                    val icon = when (i) { 0 -> Icons.Default.Home; 1 -> Icons.Default.Search; 2 -> Icons.Default.MusicNote; else -> Icons.Default.Person }
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
                                    if (video.url.isNotBlank()) {
                                        VideoPlayback(video.url)
                                    }
                                    Text(video.caption, color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))

                                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("♥ ${video.likes}", color = Color.White, modifier = Modifier.clickable {
                                            scope.launch { runCatching { withContext(Dispatchers.IO) { Api.like(base, token, video.id) }; loadFeed() }.onFailure { status = it.message ?: "Ошибка лайка" } }
                                        })
                                        Text("Комментарии: ${video.comments}", color = Color.White, modifier = Modifier.clickable {
                                            commentVideo = video
                                            scope.launch {
                                                try { commentItems = withContext(Dispatchers.IO) { Api.comments(base, token, video.id) } }
                                                catch (e: Exception) { status = e.message ?: "Ошибка комментариев" }
                                            }
                                        })
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
                        items(users) { found ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column { Text("@${found.username}", color = Color.White); Text(found.displayName, color = Color.Gray) }
                                Button(onClick = {
                                    scope.launch {
                                        try { withContext(Dispatchers.IO) { Api.follow(base, token, found.id) }; status = "Подписка обновлена" }
                                        catch (e: Exception) { status = e.message ?: "Ошибка подписки" }
                                    }
                                }, colors = ButtonDefaults.buttonColors(containerColor = AppAccent)) { Text("Подписаться") }
                            }
                            HorizontalDivider(color = Color.DarkGray)
                        }
                    }
                    if (status.isNotBlank()) Text(status, color = Color.LightGray)
                }
                2 -> {
                    Text("Музыка", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 10.dp)) {
                        Button(onClick = { audioPicker.launch(arrayOf("audio/*")) }, colors = ButtonDefaults.buttonColors(containerColor = AppAccent)) { Text("Загрузить аудио") }
                        OutlinedButton(onClick = {
                            scope.launch { try { musicItems = withContext(Dispatchers.IO) { Api.music(base, token) }; status = "Музыка обновлена" } catch (e: Exception) { status = e.message ?: "Ошибка музыки" } }
                        }) { Text("Обновить") }
                    }
                    if (musicItems.isEmpty()) Text("Пока нет загруженных треков. Загрузи первый аудиофайл.", color = Color.LightGray)
                    LazyColumn {
                        items(musicItems, key = { it.id }) { track ->
                            Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = AppSurface)) {
                                Row(Modifier.fillMaxWidth().clickable { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(track.url))) } }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.MusicNote, null, tint = AppAccent)
                                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                        Text(track.title, color = Color.White)
                                        Text("@\${track.username}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                                    }
                                    Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                                }
                            }
                        }
                    }
                    if (status.isNotBlank()) Text(status, color = Color.LightGray)
                }
                else -> {
                    Text(currentUser?.displayName?.ifBlank { currentUser?.username ?: "Профиль" } ?: "Профиль", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Text("@${currentUser?.username ?: ""}", color = Color.LightGray, modifier = Modifier.padding(top = 8.dp))
                    Text(currentUser?.email ?: "", color = Color.Gray)
                    Text(currentUser?.bio ?: "", color = Color.LightGray, modifier = Modifier.padding(top = 12.dp))
                    if (currentUser?.verified == true) Text("Подтверждённый аккаунт", color = Color(0xFF25F4EE))
                    OutlinedTextField(displayNameDraft, { displayNameDraft = it }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), label = { Text("Отображаемое имя") }, singleLine = true)
                    OutlinedTextField(bioDraft, { bioDraft = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("О себе") }, maxLines = 3)
                    Button(onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { Api.updateProfile(base, token, displayNameDraft, bioDraft) }
                                currentUser = withContext(Dispatchers.IO) { Api.me(base, token) }
                                status = "Профиль сохранён"
                            } catch (e: Exception) { status = e.message ?: "Ошибка сохранения профиля" }
                        }
                    }, modifier = Modifier.padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = AppAccent)) { Text("Сохранить профиль") }
                    OutlinedTextField(verificationReason, { verificationReason = it }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), label = { Text("Причина запроса галочки") }, maxLines = 3)
                    OutlinedButton(onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) { Api.requestVerification(base, token, verificationReason) }
                                verificationReason = ""
                                status = "Заявка на верификацию отправлена"
                            } catch (e: Exception) { status = e.message ?: "Ошибка заявки" }
                        }
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Запросить верификацию") }
                    OutlinedTextField(businessName, { businessName = it }, modifier = Modifier.fillMaxWidth().padding(top = 18.dp), label = { Text("Название бизнес-аккаунта") }, singleLine = true)
                    OutlinedTextField(businessCategory, { businessCategory = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Категория бизнеса") }, singleLine = true)
                    OutlinedTextField(businessDescription, { businessDescription = it }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), label = { Text("Описание бизнеса") }, maxLines = 3)
                    Button(onClick = {
                        scope.launch {
                            try { withContext(Dispatchers.IO) { Api.saveBusiness(base, token, businessName, businessCategory, businessDescription) }; status = "Бизнес-профиль сохранён" }
                            catch (e: Exception) { status = e.message ?: "Ошибка бизнес-профиля" }
                        }
                    }, modifier = Modifier.padding(top = 8.dp), colors = ButtonDefaults.buttonColors(containerColor = AppAccent)) { Text("Сохранить бизнес-профиль") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            try { digitalId = withContext(Dispatchers.IO) { Api.digitalId(base, token) }; status = "Цифровой ID загружен" }
                            catch (e: Exception) { status = e.message ?: "Ошибка цифрового ID" }
                        }
                    }, modifier = Modifier.padding(top = 10.dp)) { Text("Мой цифровой ID") }
                    if (digitalId.isNotBlank()) {
                        Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = AppSurface)) {
                            Column(Modifier.padding(14.dp)) {
                                Text("TikTok X · DIGITAL ID", color = AppAccent, style = MaterialTheme.typography.labelLarge)
                                Text(digitalId, color = Color.White, style = MaterialTheme.typography.titleMedium)
                                Text("@" + (currentUser?.username ?: ""), color = Color.LightGray)
                            }
                        }
                    }
                    if (status.isNotBlank()) Text(status, color = Color.LightGray, modifier = Modifier.padding(top = 10.dp))
                }
            }
        }
    }
    commentVideo?.let { selected ->
        AlertDialog(
            onDismissRequest = { commentVideo = null },
            title = { Text("Комментарии") },
            text = {
                Column {
                    if (commentItems.isEmpty()) Text("Комментариев пока нет", color = Color.LightGray)
                    commentItems.forEach { Text("@" + it.username + ": " + it.body, color = Color.White, modifier = Modifier.padding(vertical = 4.dp)) }
                    OutlinedTextField(commentDraft, { commentDraft = it }, label = { Text("Написать комментарий") }, maxLines = 3)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        try {
                            withContext(Dispatchers.IO) { Api.addComment(base, token, selected.id, commentDraft) }
                            commentItems = withContext(Dispatchers.IO) { Api.comments(base, token, selected.id) }
                            commentDraft = ""
                            loadFeed()
                        } catch (e: Exception) { status = e.message ?: "Ошибка отправки комментария" }
                    }
                }) { Text("Отправить", color = AppAccent) }
            },
            dismissButton = { TextButton(onClick = { commentVideo = null }) { Text("Закрыть") } }
        )
    }
    selectedAudio?.let { uri ->
        AlertDialog(
            onDismissRequest = { selectedAudio = null },
            title = { Text("Загрузка музыки") },
            text = { OutlinedTextField(audioTitle, { audioTitle = it }, label = { Text("Название трека") }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = {
                    scope.launch {
                        busy = true
                        try {
                            withContext(Dispatchers.IO) { Api.uploadMusic(context, base, token, uri, audioTitle) }
                            selectedAudio = null
                            audioTitle = ""
                            musicItems = withContext(Dispatchers.IO) { Api.music(base, token) }
                            status = "Трек опубликован"
                        } catch (e: Exception) { status = e.message ?: "Ошибка загрузки музыки" }
                        finally { busy = false }
                    }
                }) { Text(if (busy) "Загрузка..." else "Опубликовать", color = AppAccent) }
            },
            dismissButton = { TextButton(onClick = { selectedAudio = null }) { Text("Отмена") } }
        )
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
                        } catch (e: Exception) { status = e.message ?: "Ошибка загрузки" }
                        finally { busy = false }
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
    fun comments(base: String, token: String, id: Long): List<AppComment> {
        val a = request(base, "/videos/" + id + "/comments", "GET", token = token).optJSONArray("comments") ?: return emptyList()
        return buildList { for (i in 0 until a.length()) { val x = a.getJSONObject(i); add(AppComment(x.getLong("id"), x.optString("username"), x.optString("body"))) } }
    }
    fun addComment(base: String, token: String, id: Long, body: String) {
        require(body.isNotBlank()) { "Комментарий пустой" }
        request(base, "/videos/" + id + "/comments", "POST", JSONObject().put("body", body), token)
    }
    fun uploadVideo(context: Context, base: String, token: String, uri: Uri, caption: String) {
        val boundary = "TikTokX-" + System.currentTimeMillis()
        val c = URL(endpoint(base, "/upload-video")).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 20000
        c.readTimeout = 120000
        c.doOutput = true
        c.setRequestProperty("Authorization", "Bearer " + token)
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
        c.setChunkedStreamingMode(65536)
        c.outputStream.use { out ->
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n" + caption + "\r\n").toByteArray(Charsets.UTF_8))
            val mime = context.contentResolver.getType(uri) ?: "video/mp4"
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"video\"; filename=\"clip\"\r\nContent-Type: " + mime + "\r\n\r\n").toByteArray(Charsets.UTF_8))
            context.contentResolver.openInputStream(uri)?.use { input -> input.copyTo(out, 65536) } ?: throw IllegalStateException("Не удалось прочитать видео")
            out.write(("\r\n--" + boundary + "--\r\n").toByteArray(Charsets.UTF_8))
        }
        val code = c.responseCode
        val raw = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        c.disconnect()
        val json = try { JSONObject(raw) } catch (_: Exception) { throw IllegalStateException("Сервер вернул HTTP " + code) }
        if (code !in 200..299 || !json.optBoolean("ok", false)) throw IllegalStateException(json.optString("error", "Ошибка загрузки"))
    }
    fun music(base: String, token: String): List<AppMusic> {
        val a = request(base, "/music", "GET", token = token).optJSONArray("music") ?: return emptyList()
        return buildList { for (i in 0 until a.length()) { val x = a.getJSONObject(i); add(AppMusic(x.getLong("id"), x.optString("username"), x.optString("title"), x.optString("file_path"))) } }
    }
    fun uploadMusic(context: Context, base: String, token: String, uri: Uri, title: String) {
        require(title.isNotBlank()) { "Укажи название трека" }
        val boundary = "TikTokXAudio-" + System.currentTimeMillis()
        val c = URL(endpoint(base, "/upload-music")).openConnection() as HttpURLConnection
        c.requestMethod = "POST"; c.connectTimeout = 20000; c.readTimeout = 120000; c.doOutput = true
        c.setRequestProperty("Authorization", "Bearer " + token)
        c.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary)
        c.setChunkedStreamingMode(65536)
        c.outputStream.use { out ->
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"title\"\r\n\r\n" + title + "\r\n").toByteArray(Charsets.UTF_8))
            val mime = context.contentResolver.getType(uri) ?: "audio/mpeg"
            out.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"audio\"; filename=\"track\"\r\nContent-Type: " + mime + "\r\n\r\n").toByteArray(Charsets.UTF_8))
            context.contentResolver.openInputStream(uri)?.use { it.copyTo(out, 65536) } ?: throw IllegalStateException("Не удалось прочитать аудио")
            out.write(("\r\n--" + boundary + "--\r\n").toByteArray(Charsets.UTF_8))
        }
        val code = c.responseCode
        val raw = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
        c.disconnect()
        val json = try { JSONObject(raw) } catch (_: Exception) { throw IllegalStateException("Сервер вернул HTTP " + code) }
        if (code !in 200..299 || !json.optBoolean("ok", false)) throw IllegalStateException(json.optString("error", "Ошибка загрузки"))
    }
    fun follow(base: String, token: String, id: Long) { request(base, "/users/" + id + "/follow", "POST", JSONObject(), token) }
    fun updateProfile(base: String, token: String, displayName: String, bio: String) { request(base, "/profile", "PUT", JSONObject().put("display_name", displayName).put("bio", bio), token) }
    fun digitalId(base: String, token: String): String {
        return request(base, "/digital-id", "GET", token = token).optString("digital_id")
    }
    fun saveBusiness(base: String, token: String, name: String, category: String, description: String) {
        require(name.isNotBlank()) { "Укажи название бизнес-аккаунта" }
        request(base, "/business-profile", "PUT", JSONObject().put("business_name", name).put("category", category).put("description", description), token)
    }
    fun requestVerification(base: String, token: String, reason: String) { require(reason.trim().length >= 20) { "Опиши причину минимум в 20 символах" }; request(base, "/verification", "POST", JSONObject().put("reason", reason), token) }
    fun like(base: String, token: String, id: Long) { request(base, "/videos/$id/like", "POST", JSONObject(), token) }
}
