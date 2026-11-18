package com.tiktokx.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.launch

private val bg=Color(0xFF050507)
private val accent=Color(0xFFFF3158)
private val muted=Color(0xFFB8B8C0)
private data class Clip(val id:String,val author:String,val title:String,val url:String,val likes:Int)
private val clips=listOf(
 Clip("1","@nature_daily","Красота природы — короткая передышка","https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",12840),
 Clip("2","@movie_time","Небольшой кинематографичный момент","https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",8421),
 Clip("3","@travel_world","Путешествия начинаются с первого шага","https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerJoyrides.mp4",5310),
 Clip("4","@daily_video","Демонстрационный ролик номер четыре","https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerMeltdowns.mp4",2940),
 Clip("5","@creator_x","Добро пожаловать в TikTok X","https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",19320)
)
class MainActivity:ComponentActivity(){
 override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);window.statusBarColor=android.graphics.Color.BLACK;window.navigationBarColor=android.graphics.Color.BLACK;setContent{TikTokXApp()}}
}
@Composable private fun TikTokXApp(){
 val ctx=LocalContext.current
 val prefs=remember{ctx.getSharedPreferences("tiktok_x",0)}
 val liked=remember{mutableStateListOf<String>().apply{addAll(prefs.getStringSet("liked",emptySet())?:emptySet())}}
 val saved=remember{mutableStateListOf<String>().apply{addAll(prefs.getStringSet("saved",emptySet())?:emptySet())}}
 val history=remember{mutableStateListOf<String>().apply{addAll(prefs.getString("history","")!!.split(",").filter{it.isNotBlank()})}}
 val comments=remember{mutableStateMapOf<String,MutableList<String>>()}
 var tab by remember{mutableStateOf("Главная")}
 var query by remember{mutableStateOf("")}
 var commentClip by remember{mutableStateOf<Clip?>(null)}
 var commentText by remember{mutableStateOf("")}
 val pager=rememberPagerState(pageCount={clips.size})
 val scope=rememberCoroutineScope()
 val player=remember{ExoPlayer.Builder(ctx).build().apply{repeatMode=ExoPlayer.REPEAT_MODE_ONE}}
 LaunchedEffect(tab,pager.currentPage){
  if(tab=="Главная"){
   val clip=clips[pager.currentPage]
   player.setMediaItem(MediaItem.fromUri(clip.url));player.prepare();player.play()
   history.remove(clip.id);history.add(0,clip.id);while(history.size>30)history.removeAt(history.lastIndex)
   prefs.edit().putString("history",history.joinToString(",")).apply()
  }else player.pause()
 }
 DisposableEffect(Unit){onDispose{player.release()}}
 MaterialTheme(colorScheme=darkColorScheme(primary=accent,background=bg,surface=Color(0xFF15151B))){
  Scaffold(containerColor=bg,bottomBar={
   NavigationBar(containerColor=Color(0xFF101015)){
    listOf("Главная","Поиск","Избранное","Профиль").forEach{item->
     val icon=when(item){"Главная"->Icons.Default.Home;"Поиск"->Icons.Default.Search;"Избранное"->Icons.Default.Bookmark;else->Icons.Default.Person}
     NavigationBarItem(selected=tab==item,onClick={tab=item},icon={Icon(icon,null)},label={Text(item,fontSize=10.sp)},colors=NavigationBarItemDefaults.colors(selectedIconColor=accent,selectedTextColor=accent,indicatorColor=accent.copy(.12f),unselectedIconColor=Color.LightGray,unselectedTextColor=Color.LightGray))
    }
   }
  }){padding->
   Box(Modifier.fillMaxSize().padding(padding)){
    when(tab){
     "Главная"->VerticalPager(state=pager,modifier=Modifier.fillMaxSize(),key={clips[it].id}){index->
      val clip=clips[index]
      Box(Modifier.fillMaxSize().background(Color.Black)){
       if(index==pager.currentPage)AndroidView(factory={viewContext->PlayerView(viewContext).apply{useController=false;resizeMode=AspectRatioFrameLayout.RESIZE_MODE_ZOOM;this.player=player}},update={it.player=player},modifier=Modifier.fillMaxSize())
       Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(.35f),Color.Transparent,Color.Black.copy(.78f)))))
       Text("Подписки       Для вас",Modifier.align(Alignment.TopCenter).padding(top=14.dp),color=Color.White,fontWeight=FontWeight.Bold,fontSize=17.sp)
       Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(.78f).padding(start=16.dp,bottom=22.dp)){
        Text(clip.author,color=Color.White,fontWeight=FontWeight.Bold,fontSize=16.sp)
        Text(clip.title,color=Color.White,fontSize=14.sp,modifier=Modifier.padding(top=8.dp))
        Text("♫ Original sound · TikTok X",color=Color.White,fontSize=12.sp,modifier=Modifier.padding(top=10.dp))
        Text("Демонстрационный ролик",color=Color.White.copy(.7f),fontSize=10.sp)
       }
       Column(Modifier.align(Alignment.BottomEnd).padding(end=12.dp,bottom=26.dp),horizontalAlignment=Alignment.CenterHorizontally){
        Box(Modifier.size(44.dp).background(Color.White,CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.Person,null,tint=Color.DarkGray)}
        Spacer(Modifier.height(18.dp))
        FeedAction(if(clip.id in liked)Icons.Default.Favorite else Icons.Default.FavoriteBorder,if(clip.id in liked)accent else Color.White,shortCount(clip.likes+if(clip.id in liked)1 else 0)){
         if(clip.id in liked)liked.remove(clip.id)else liked.add(clip.id);prefs.edit().putStringSet("liked",liked.toSet()).apply()
        }
        FeedAction(Icons.Default.ChatBubble,Color.White,"Комменты"){commentClip=clip}
        FeedAction(if(clip.id in saved)Icons.Default.Bookmark else Icons.Default.BookmarkBorder,if(clip.id in saved)accent else Color.White,"Сохранить"){
         if(clip.id in saved)saved.remove(clip.id)else saved.add(clip.id);prefs.edit().putStringSet("saved",saved.toSet()).apply()
        }
        FeedAction(Icons.Default.Share,Color.White,"Поделиться"){
         val send=Intent(Intent.ACTION_SEND).apply{type="text/plain";putExtra(Intent.EXTRA_TEXT,"TikTok X — "+clip.title+"\n"+clip.url)}
         ctx.startActivity(Intent.createChooser(send,"Поделиться роликом"))
        }
       }
      }
     }
     "Поиск"->Column(Modifier.fillMaxSize().padding(18.dp)){
      Text("Поиск",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)
      OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth().padding(top=14.dp),singleLine=true,placeholder={Text("Найти автора или видео")},leadingIcon={Icon(Icons.Default.Search,null)})
      clips.filter{it.author.contains(query,true)||it.title.contains(query,true)}.forEach{clip->
       Row(Modifier.fillMaxWidth().clickable{tab="Главная";scope.launch{pager.scrollToPage(clips.indexOf(clip))}}.padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically){
        Icon(Icons.Default.PlayCircle,null,tint=accent,modifier=Modifier.size(36.dp))
        Column(Modifier.padding(start=12.dp)){Text(clip.title,color=Color.White);Text(clip.author,color=muted,fontSize=12.sp)}
       };HorizontalDivider(color=Color.DarkGray)
      }
     }
     "Избранное"->Column(Modifier.fillMaxSize().padding(18.dp)){
      Text("Избранное",color=Color.White,fontSize=26.sp,fontWeight=FontWeight.Bold)
      val items=clips.filter{it.id in saved}
      if(items.isEmpty())Text("Сохраняй понравившиеся ролики кнопкой закладки в ленте.",color=muted,modifier=Modifier.padding(top=18.dp))
      items.forEach{clip->SavedClipRow(clip){tab="Главная";scope.launch{pager.scrollToPage(clips.indexOf(clip))}}}
     }
     else->Column(Modifier.fillMaxSize().padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally){
      Spacer(Modifier.height(30.dp))
      Box(Modifier.size(90.dp).background(Brush.linearGradient(listOf(accent,Color(0xFF6639C9))),CircleShape),contentAlignment=Alignment.Center){Icon(Icons.Default.Person,null,tint=Color.White,modifier=Modifier.size(52.dp))}
      Text("Пользователь TikTok X",color=Color.White,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=16.dp))
      Text("@user_tiktokx",color=muted)
      Row(Modifier.fillMaxWidth().padding(vertical=28.dp),horizontalArrangement=Arrangement.SpaceEvenly){ProfileStat("Лайки",liked.size.toString());ProfileStat("Избранное",saved.size.toString());ProfileStat("История",history.size.toString())}
      Text("Локальный профиль демонстрационной версии. Регистрация и синхронизация пока не подключены.",color=muted,fontSize=13.sp)
      OutlinedButton(onClick={tab="Главная"}){Text("Открыть видеоленту")}
     }
    }
   }
  }
 }
 commentClip?.let{clip->AlertDialog(onDismissRequest={commentClip=null},title={Text("Комментарии")},text={
  Column{
   val list=comments[clip.id]?:emptyList()
   if(list.isEmpty())Text("Пока нет комментариев. Будь первым!",color=muted)
   list.forEach{Text(it,color=Color.White,modifier=Modifier.padding(vertical=4.dp))}
   OutlinedTextField(commentText,{commentText=it},label={Text("Написать комментарий")},singleLine=true)
  }
 },confirmButton={TextButton(onClick={
  if(commentText.isNotBlank()){val list=comments.getOrPut(clip.id){mutableStateListOf<String>()};list.add("@user_tiktokx: "+commentText.trim());commentText=""}
  commentClip=null
 }){Text("Отправить",color=accent)}},dismissButton={TextButton(onClick={commentClip=null}){Text("Закрыть")}},containerColor=Color(0xFF191920))}
}
@Composable private fun FeedAction(icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,label:String,onClick:()->Unit){
 Column(Modifier.padding(bottom=18.dp).clickable(onClick=onClick),horizontalAlignment=Alignment.CenterHorizontally){Icon(icon,null,tint=tint,modifier=Modifier.size(30.dp));Text(label,color=Color.White,fontSize=9.sp,modifier=Modifier.padding(top=3.dp))}
}
@Composable private fun SavedClipRow(clip:Clip,onClick:()->Unit){
 Row(Modifier.fillMaxWidth().clickable(onClick=onClick).padding(vertical=14.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.PlayCircle,null,tint=accent,modifier=Modifier.size(34.dp));Column(Modifier.padding(start=12.dp)){Text(clip.title,color=Color.White);Text(clip.author,color=muted,fontSize=12.sp)}}
 HorizontalDivider(color=Color.DarkGray)
}
@Composable private fun ProfileStat(label:String,value:String){Column(horizontalAlignment=Alignment.CenterHorizontally){Text(value,color=Color.White,fontSize=19.sp,fontWeight=FontWeight.Bold);Text(label,color=muted,fontSize=12.sp)}}
private fun shortCount(value:Int)=if(value>=1000)String.format("%.1fK",value/1000f)else value.toString()
