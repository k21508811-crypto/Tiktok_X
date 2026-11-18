package ai.neuro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
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

private val backgroundTop = Color(0xFF090B14)
private val backgroundBottom = Color(0xFF171126)
private val violet = Color(0xFF9B72FF)
private val lavender = Color(0xFFC7A8FF)
private val muted = Color(0xFFA9A4B8)

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
                NeuroChatBetaScreen()
            }
        }
    }
}

@Composable
private fun NeuroChatBetaScreen() {
    var showProInfo by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(backgroundTop, Color(0xFF11101D), backgroundBottom)
                )
            )
            .padding(horizontal = 22.dp, vertical = 18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            Brush.linearGradient(listOf(violet, Color(0xFF5435A6))),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("N", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(11.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("AI NEURO", fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
                    Text("ИНТЕЛЛЕКТ НОВОГО ПОКОЛЕНИЯ", fontSize = 9.sp, color = muted, letterSpacing = 1.sp)
                }
                Surface(
                    color = Color(0xFF302342),
                    shape = RoundedCornerShape(50)
                ) {
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

            Spacer(Modifier.weight(1f))

            Box(
                modifier = Modifier
                    .size(112.dp)
                    .shadow(24.dp, CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF5B3BA0), Color(0xFF241C3D), Color(0xFF171426))
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("✦", color = lavender, fontSize = 58.sp)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                "AI Neuro Chat",
                color = Color.White,
                fontSize = 29.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(9.dp))
            Text(
                "Ваш интеллектуальный помощник",
                color = muted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(28.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xCC201B30),
                shape = RoundedCornerShape(22.dp),
                tonalElevation = 3.dp
            ) {
                Column(modifier = Modifier.padding(21.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Здравствуйте!", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("●", color = Color(0xFFFFC66D), fontSize = 10.sp)
                        Spacer(Modifier.width(5.dp))
                        Text("БЕТА-ТЕСТ", color = Color(0xFFFFC66D), fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Это бета-версия AI Neuro. Сейчас наш чат временно недоступен. Оформите подписку AI Neuro PRO, чтобы открыть доступ к чату.",
                        color = Color(0xFFE0DCEB),
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(Modifier.height(17.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF171426),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(modifier = Modifier.padding(17.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✧", color = lavender, fontSize = 22.sp)
                        Spacer(Modifier.width(9.dp))
                        Text("AI NEURO PRO", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(Modifier.weight(1f))
                        Text("PRO", color = lavender, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    Spacer(Modifier.height(7.dp))
                    Text(
                        "Доступ к ИИ-чату и расширенным возможностям платформы.",
                        color = muted,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(15.dp))
                    Button(
                        onClick = { showProInfo = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = violet)
                    ) {
                        Text(
                            "ОФОРМИТЬ PRO",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                "AI NEURO  •  ВАШ ИНТЕЛЛЕКТ БЕЗ ГРАНИЦ",
                color = Color(0xFF777184),
                fontSize = 9.sp,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }

    if (showProInfo) {
        AlertDialog(
            onDismissRequest = { showProInfo = false },
            containerColor = Color(0xFF211B31),
            title = { Text("AI Neuro PRO", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Подключение оплаты и проверка PRO-подписки ещё не настроены. Мы не будем списывать деньги или показывать фиктивную оплату. Доступ к чату откроется после подключения безопасной системы подписок.",
                    color = Color(0xFFD8D2E5)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showProInfo = false },
                    colors = ButtonDefaults.buttonColors(containerColor = violet)
                ) {
                    Text("ПОНЯТНО")
                }
            }
        )
    }
}
