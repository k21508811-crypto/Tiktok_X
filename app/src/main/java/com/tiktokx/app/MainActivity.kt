package com.tiktokx.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Black = Color(0xFF08090D)
private val Panel = Color(0xFF171922)
private val Red = Color(0xFFFF3158)
private val Cyan = Color(0xFF25D9E8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TikTokXApp() }
    }
}

@Composable
fun TikTokXApp() {
    val context = LocalContext.current
    var tab by remember { mutableStateOf("Главная") }
    var autoScroll by remember { mutableStateOf(false) }
    var highQuality by remember { mutableStateOf(true) }
    var favorite by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Black, surface = Panel, primary = Red,
        secondary = Cyan, onBackground = Color.White, onSurface = Color.White
    )) {
        Scaffold(containerColor = Black, bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Color.White) {
                listOf("Главная", "Избранное", "История", "Настройки").forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item; showSettings = item == "Настройки" },
                        icon = {
                            Icon(when (item) {
                                "Избранное" -> Icons.Default.Bookmark
                                "История" -> Icons.Default.History
                                "Настройки" -> Icons.Default.Settings
                                else -> Icons.Default.PlayArrow
                            }, contentDescription = item)
                        },
                        label = { Text(item) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Red, selectedTextColor = Red,
                            indicatorColor = Red.copy(alpha = .14f),
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray
                        )
                    )
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 18.dp)) {
                Row(Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 18.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("TIKTOK", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Color.White)
                    Text(" X", fontSize = 25.sp, fontWeight = FontWeight.Black, color = Red)
                    Spacer(Modifier.weight(1f))
                    Text("НЕОФИЦИАЛЬНО", color = Cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                when {
                    showSettings -> {
                        Text("Настройки", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        SettingRow("Автопрокрутка ленты", autoScroll) { autoScroll = it }
                        SettingRow("Предпочитать высокое качество", highQuality) { highQuality = it }
                        Text("Настройки применяются к интерфейсу приложения. Управление воспроизведением на стороннем сайте не гарантируется.",
                            color = Color.Gray, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                    tab == "Избранное" -> {
                        Text("Избранное", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text(if (favorite) "Сохранён демонстрационный элемент." else "Пока ничего не сохранено.",
                            color = Color.LightGray, modifier = Modifier.padding(top = 16.dp))
                    }
                    tab == "История" -> {
                        Text("История просмотров", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("История просмотров внутри внешнего TikTok не считывается. Здесь будут отображаться только элементы, открытые в самом TikTok X.",
                            color = Color.LightGray, modifier = Modifier.padding(top = 16.dp))
                    }
                    else -> {
                        Text("Твоя видеолента", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(14.dp))
                        Box(
                            Modifier.fillMaxWidth().weight(1f).background(
                                Brush.linearGradient(listOf(Color(0xFF202431), Color(0xFF11131A), Color(0xFF30202B))),
                                RoundedCornerShape(24.dp)
                            ).padding(22.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("TIKTOK X", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                                Text("Видео открываются на публичном сайте TikTok",
                                    color = Color.LightGray, fontSize = 13.sp)
                                Spacer(Modifier.height(22.dp))
                                Button(
                                    onClick = {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.tiktok.com/foryou")))
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Red)
                                ) {
                                    Icon(Icons.Default.PlayArrow, null)
                                    Spacer(Modifier.width(6.dp))
                                    Text("Открыть TikTok")
                                }
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { favorite = !favorite }) {
                                    Icon(Icons.Default.Favorite, null, tint = if (favorite) Red else Color.White)
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (favorite) "В избранном" else "В избранное")
                                }
                            }
                        }
                        Text("TikTok X — независимый интерфейс. Для просмотра внешнего контента используется официальный публичный сайт.",
                            color = Color.Gray, fontSize = 11.sp, modifier = Modifier.padding(vertical = 12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(title: String, checked: Boolean, onChecked: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, modifier = Modifier.weight(1f), color = Color.White)
        Switch(checked = checked, onCheckedChange = onChecked,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Red))
    }
}
