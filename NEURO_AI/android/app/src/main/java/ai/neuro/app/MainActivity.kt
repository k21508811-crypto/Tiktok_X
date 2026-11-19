package ai.neuro.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext

private val bg = Color(0xFF08060D)
private val panel = Color(0xFF191321)
private val purple = Color(0xFF9B4DFF)
private val muted = Color(0xFFB8ADC6)
private data class Service(val title:String,val url:String,val note:String,val category:String)
private val services = listOf(
 Service("ChatGPT","https://chatgpt.com/","Универсальный ИИ-помощник","Чаты"),
 Service("Gemini","https://gemini.google.com/app","ИИ от Google","Чаты"),
 Service("Copilot","https://copilot.microsoft.com/","ИИ от Microsoft","Чаты"),
 Service("DeepSeek","https://chat.deepseek.com/","Диалог и анализ","Чаты"),
 Service("Perplexity","https://www.perplexity.ai/","Поиск с источниками","Поиск"),
 Service("Qwen","https://chat.qwen.ai/","Модели Qwen","Чаты"),
 Service("Mistral","https://chat.mistral.ai/","Le Chat от Mistral","Чаты"),
 Service("HuggingChat","https://huggingface.co/chat/","Открытые модели","Чаты"),
 Service("Claude","https://claude.ai/","ИИ от Anthropic","Чаты"),
 Service("Grok","https://grok.com/","ИИ от xAI","Чаты"),
 Service("Алиса AI","https://alice.yandex.ru/","ИИ от Яндекса","Чаты"),
 Service("Poe","https://poe.com/","Каталог ИИ-ботов","Чаты"),
 Service("Duck.ai","https://duck.ai/","ИИ-чат DuckDuckGo","Приватность")
)

class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState)
  setContent { MaterialTheme(colorScheme=darkColorScheme(primary=purple,background=bg,surface=panel,onPrimary=Color.White,onBackground=Color.White,onSurface=Color.White)){NeuroApp()} }
 }
}

@Composable private fun NeuroApp(){
 val context=LocalContext.current
 val prefs=remember{context.getSharedPreferences("neuro_preferences",0)}
 var selected by remember{mutableStateOf(services.first())}
 var query by remember{mutableStateOf("")}
 var category by remember{mutableStateOf("Все")}
 var cookieChoice by remember{mutableStateOf(if(prefs.contains("third_party_cookies")) prefs.getBoolean("third_party_cookies",false) else null)}
 var askCookies by remember{mutableStateOf(false)}
 var privacy by remember{mutableStateOf(false)}
 var web by remember{mutableStateOf<WebView?>(null)}
 var favorites by remember{mutableStateOf(prefs.getStringSet("favorites",emptySet())?.toSet()?:emptySet())}
 var favoriteOnly by remember{mutableStateOf(false)}
 BackHandler(enabled=web?.canGoBack()==true){web?.goBack()}

 Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF090711),bg,Color(0xFF12091B)))).statusBarsPadding().navigationBarsPadding()){
  Row(Modifier.fillMaxWidth().padding(horizontal=18.dp,vertical=13.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(purple,Color(0xFF35105D))),RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center){Text("N",fontSize=28.sp,fontWeight=FontWeight.Black)}
   Spacer(Modifier.width(11.dp))
   Column(Modifier.weight(1f)){Text("NEURO AI",fontSize=21.sp,fontWeight=FontWeight.Black,letterSpacing=1.2.sp);Text("ТВОЯ ПАНЕЛЬ ИИ-СЕРВИСОВ",fontSize=9.sp,color=muted,letterSpacing=1.sp)}
   TextButton(onClick={privacy=true}){Text("ПРИВАТНОСТЬ",fontSize=9.sp,color=Color(0xFFD8B6FF))}
  }
  LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(horizontal=18.dp,vertical=8.dp),verticalArrangement=Arrangement.spacedBy(13.dp)){
   item{
    Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(Color(0xFF35134E),Color(0xFF17101F),Color(0xFF241039))),RoundedCornerShape(23.dp)).padding(20.dp)){
     Text("NEON AI DIRECTORY  /  BETA",color=Color(0xFFE2C9FF),fontSize=9.sp,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp)
     Spacer(Modifier.height(9.dp));Text("Найди своего\nИИ-помощника",fontSize=27.sp,lineHeight=31.sp,fontWeight=FontWeight.ExtraBold)
     Spacer(Modifier.height(8.dp));Text("Каталог независимых AI-сервисов с поиском, категориями и избранным.",fontSize=12.sp,lineHeight=17.sp,color=muted)
    }
   }
   item{
    Text("ПОИСК ПО КАТАЛОГУ",fontSize=10.sp,color=muted,fontWeight=FontWeight.Bold,letterSpacing=1.2.sp);Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth().background(panel,RoundedCornerShape(15.dp)).padding(14.dp),verticalAlignment=Alignment.CenterVertically){
     Text("⌕",fontSize=22.sp,color=purple);Spacer(Modifier.width(9.dp))
     BasicTextField(query,{query=it},Modifier.weight(1f),singleLine=true,textStyle=TextStyle(color=Color.White,fontSize=14.sp),decorationBox={inner->if(query.isEmpty())Text("Название сервиса или задача…",color=Color(0xFF847A90),fontSize=12.sp);inner()})
     if(query.isNotEmpty()) Text("✕",Modifier.clickable{query=""},color=muted)
    }
   }
   item{
    Row(horizontalArrangement=Arrangement.spacedBy(7.dp)){
     listOf("Все","Чаты","Поиск","Приватность").forEach{c->Surface(color=if(category==c)purple.copy(alpha=.32f) else panel,shape=RoundedCornerShape(30.dp),modifier=Modifier.clickable{category=c}){Text(c,Modifier.padding(horizontal=12.dp,vertical=9.dp),fontSize=10.sp,color=if(category==c)Color.White else muted,fontWeight=FontWeight.SemiBold)}}
    }
   }
   item{Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("ИИ-СЕРВИСЫ",fontSize=17.sp,fontWeight=FontWeight.ExtraBold);Text("Выбери сервис, чтобы открыть его сайт",fontSize=10.sp,color=muted)};TextButton(onClick={favoriteOnly=!favoriteOnly}){Text(if(favoriteOnly)"ВСЕ" else "★ ИЗБРАННОЕ",fontSize=9.sp,color=Color(0xFFD7B4FF))}}}
   val visible=services.filter{(category=="Все"||it.category==category)&&(!favoriteOnly||favorites.contains(it.title))&&(it.title.contains(query,true)||it.note.contains(query,true))}
   items(visible.size){i->
    val s=visible[i];val active=selected.title==s.title
    val tint by animateColorAsState(if(active)Color(0xFF48216A) else panel,label="service")
    Row(Modifier.fillMaxWidth().background(tint,RoundedCornerShape(16.dp)).clickable{selected=s;web=null;if(cookieChoice==null)askCookies=true}.padding(13.dp),verticalAlignment=Alignment.CenterVertically){
     Box(Modifier.size(42.dp).background(Color(0xFF30203F),RoundedCornerShape(13.dp)),contentAlignment=Alignment.Center){Text(s.title.take(1).uppercase(),fontSize=18.sp,color=Color(0xFFE7CCFF),fontWeight=FontWeight.Black)}
     Spacer(Modifier.width(11.dp));Column(Modifier.weight(1f)){Text(s.title,fontSize=14.sp,fontWeight=FontWeight.Bold);Text(s.note,fontSize=10.sp,color=muted)}
     Text(if(favorites.contains(s.title))"★" else "☆",Modifier.clickable{favorites=if(favorites.contains(s.title))favorites-s.title else favorites+s.title;prefs.edit().putStringSet("favorites",favorites).apply()}.padding(horizontal=8.dp),fontSize=21.sp,color=Color(0xFFD5B0FF))
     Text(if(active)"ОТКРЫТ" else "›",fontSize=10.sp,color=if(active)Color(0xFFD3A5FF) else muted,fontWeight=FontWeight.Bold)
    }
   }
   if(visible.isEmpty())item{Text("Ничего не найдено. Измени запрос или категорию.",color=muted,modifier=Modifier.padding(12.dp))}
   item{
    Column(Modifier.fillMaxWidth().background(Color(0xFF130F1B),RoundedCornerShape(17.dp)).padding(14.dp)){
     Text("О СТОРОННИХ СЕРВИСАХ",fontSize=10.sp,color=Color(0xFFE2C9FF),fontWeight=FontWeight.Bold)
     Spacer(Modifier.height(5.dp));Text("NEURO AI — независимый каталог. Сервисы принадлежат своим разработчикам. Вход, доступность и обработка данных регулируются их условиями.",fontSize=10.sp,color=muted,lineHeight=15.sp)
    }
   }
   item{
    Text("ВЫБРАНО: ${selected.title}",fontSize=13.sp,fontWeight=FontWeight.Bold)
    Text(selected.note,fontSize=10.sp,color=muted)
    Spacer(Modifier.height(7.dp))
    WebPanel(selected.url,cookieChoice==true){web=it}
   }
  }
  Row(Modifier.fillMaxWidth().background(Color(0xFF0C0911)).padding(horizontal=18.dp,vertical=6.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
   Text("NEURO AI • BETA",fontSize=9.sp,color=Color(0xFF8D829A));TextButton(onClick={privacy=true}){Text("COOKIE И ДАННЫЕ",fontSize=9.sp,color=Color(0xFFCDA5FF))}
  }
 }

 if(askCookies) AlertDialog(onDismissRequest={askCookies=false},containerColor=Color(0xFF21162D),
  title={Text("Настройки cookie",color=Color.White,fontWeight=FontWeight.Bold)},
  text={Text("Сайт выбранного стороннего сервиса может сохранять cookie и данные локального хранилища для входа и настроек. Эти данные обрабатываются владельцем сайта по его правилам. Разрешить или отклонить сторонние cookie можно здесь. Основные cookie сайта остаются включёнными.",color=muted,fontSize=13.sp)},
  confirmButton={TextButton(onClick={cookieChoice=true;prefs.edit().putBoolean("third_party_cookies",true).apply();askCookies=false}){Text("РАЗРЕШИТЬ",color=Color(0xFFD9B4FF),fontWeight=FontWeight.Bold)}},
  dismissButton={TextButton(onClick={cookieChoice=false;prefs.edit().putBoolean("third_party_cookies",false).apply();askCookies=false}){Text("ОТКЛОНИТЬ",color=muted)}})
 if(privacy) AlertDialog(onDismissRequest={privacy=false},containerColor=Color(0xFF21162D),
  title={Text("Приватность и cookie",color=Color.White,fontWeight=FontWeight.Bold)},
  text={Text("NEURO AI не управляет политиками сторонних сайтов. При открытии внешнего сервиса его провайдер может получать технические данные и использовать cookie. Выбор сторонних cookie можно изменить. Приложение не является официальным продуктом OpenAI, Google, Microsoft, Яндекса или других перечисленных провайдеров.",color=muted,fontSize=13.sp)},
  confirmButton={TextButton(onClick={cookieChoice=null;prefs.edit().remove("third_party_cookies").apply();privacy=false;askCookies=true}){Text("ИЗМЕНИТЬ ВЫБОР",color=Color(0xFFD9B4FF))}},
  dismissButton={TextButton(onClick={privacy=false}){Text("ЗАКРЫТЬ",color=muted)}})
}

@SuppressLint("SetJavaScriptEnabled")
@Composable private fun WebPanel(url:String,allowThirdParty:Boolean,onCreated:(WebView)->Unit){
 val context=LocalContext.current
 AndroidView(Modifier.fillMaxWidth().height(500.dp).background(Color.White,RoundedCornerShape(18.dp)),factory={
  WebView(context).apply{
   setBackgroundColor(android.graphics.Color.WHITE)
   settings.javaScriptEnabled=true;settings.domStorageEnabled=true;settings.loadsImagesAutomatically=true
   settings.javaScriptCanOpenWindowsAutomatically=false;settings.setSupportMultipleWindows(false)
   settings.useWideViewPort=true;settings.loadWithOverviewMode=true;settings.allowFileAccess=false;settings.allowContentAccess=false
   CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(this,allowThirdParty)
   webViewClient=WebViewClient();webChromeClient=WebChromeClient();onCreated(this);loadUrl(url)
  }
 },update={CookieManager.getInstance().setAcceptThirdPartyCookies(it,allowThirdParty);onCreated(it)})
}
