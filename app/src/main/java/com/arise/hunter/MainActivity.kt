package com.arise.hunter

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Ink = Color(0xFF0B0619)
private val Violet = Color(0xFFB784FF)
private val Pale = Color(0xFFEDE0FF)
private val Muted = Color(0xFFB6A5D0)
private val Edge = Color(0xFF62478A)
private val CardShape = RoundedCornerShape(20.dp)

class MainActivity : ComponentActivity() {
    private val model: HunterModel by viewModels()
    private val permissionRequest = registerForActivityResult(PermissionController.createRequestPermissionResultContract()) { model.refresh() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AriseTheme { val state by model.state.collectAsStateWithLifecycle(); AriseApp(state, model::confirm, ::connect, model::refresh, model::selectSamsung) } }
        lifecycleScope.launch { repeatOnLifecycle(Lifecycle.State.STARTED) { while (true) { model.refresh(); delay(60_000) } } }
    }
    private fun connect() {
        if (model.availability() == HealthConnectClient.SDK_AVAILABLE) permissionRequest.launch(model.permissions)
        else try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))) }
        catch (_: Exception) { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"))) }
    }
}

@Composable fun AriseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary = Violet, onPrimary = Color.White, surface = Color(0xFF201334), background = Ink, onSurface = Pale), content = content)
}

private class Art(context: android.content.Context) {
    private val raw = mutableMapOf<String, Bitmap>()
    private val pieces = mutableMapOf<String, ImageBitmap>()
    private val assets = context.applicationContext.assets
    fun bitmap(file: String): Bitmap = raw.getOrPut(file) { assets.open(file).use { BitmapFactory.decodeStream(it) } }
    fun piece(file: String, x: Int = 0, y: Int = 0, w: Int = 0, h: Int = 0): ImageBitmap {
        val key = "$file:$x:$y:$w:$h"
        return pieces.getOrPut(key) { val b = bitmap(file); if (w == 0) b.asImageBitmap() else Bitmap.createBitmap(b,x,y,w.coerceAtMost(b.width-x),h.coerceAtMost(b.height-y)).asImageBitmap() }
    }
    fun icon(index: Int): ImageBitmap {
        val x = listOf(65,435,785,1180)[index % 4]
        return piece("ui-atlas.png",x,if(index<4) 450 else 730,if(index%4==3) 280 else 335,if(index<4) 265 else 290)
    }
    fun character(rank: Int, portrait: Boolean = false): ImageBitmap {
        val edges = intArrayOf(0,252,536,895,1220,1560,1942)
        return piece("ranks.png",edges[rank],if(portrait) 35 else 0,edges[rank+1]-edges[rank],if(portrait) 365 else 809)
    }
}
private val LocalArt = staticCompositionLocalOf<Art> { error("Art not initialized") }

@Composable private fun AriseApp(s: Progress, confirm: (Int,String)->Unit, connect: ()->Unit, refresh: ()->Unit, selectSamsung: (Boolean)->Unit) {
    val context = LocalContext.current
    val art = remember { Art(context) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var exercise by rememberSaveable { mutableIntStateOf(-1) }
    var confirmationDay by rememberSaveable { mutableStateOf("") }
    var selectedRank by rememberSaveable { mutableIntStateOf(-1) }
    var previousRank by rememberSaveable { mutableIntStateOf(s.rank) }
    var rankUp by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(s.rank) { if (s.rank > previousRank) rankUp = true; previousRank = s.rank }
    CompositionLocalProvider(LocalArt provides art) {
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink,Color(0xFF19102F),Color(0xFF301B50))))) {
            Column(Modifier.safeDrawingPadding().widthIn(max=560.dp).fillMaxSize().align(Alignment.TopCenter).padding(horizontal=18.dp)) {
                Image(art.piece("ui-atlas.png",150,5,1230,420),"ARISE",Modifier.align(Alignment.CenterHorizontally).padding(top=12.dp,bottom=6.dp).width(235.dp).height(78.dp),contentScale=ContentScale.Fit)
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when(page) {
                        0 -> Home(s)
                        1 -> Quests(s) { exercise=it; confirmationDay=s.day }
                        2 -> ActivityPage(s,refresh)
                        else -> Profile(s,connect,selectSamsung) { selectedRank=it }
                    }
                }
                BottomNav(page) { page=it }
            }
            if(exercise>=0) ConfirmDialog(exercise,s.reps,{exercise=-1}) { confirm(exercise,confirmationDay); exercise=-1 }
            if(selectedRank>=0) RankDialog(selectedRank,s.rank) { selectedRank=-1 }
            if(rankUp) Dialog(onDismissRequest={rankUp=false}) { Glass(Modifier.fillMaxWidth()) { Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) { Text("RANK UP",color=Violet,letterSpacing=4.sp); Character(s.rank,Modifier.height(260.dp)); Text("Rank ${RankNames[s.rank]} unlocked",fontSize=24.sp); Text("Your effort changed you.",color=Muted,modifier=Modifier.padding(12.dp)); Action("ARISE",{rankUp=false}) } } }
        }
    }
}

@Composable private fun Glass(modifier: Modifier=Modifier, content: @Composable ()->Unit) {
    Box(modifier.clip(CardShape).background(Brush.linearGradient(listOf(Color(0xE62D1D47),Color(0xE6150F25)))).border(1.dp,Edge.copy(alpha=.65f),CardShape)) { content() }
}
@Composable private fun ArtIcon(index:Int,modifier:Modifier=Modifier.size(36.dp)) { Image(LocalArt.current.icon(index),null,modifier,contentScale=ContentScale.Fit) }
@Composable private fun Character(rank:Int,modifier:Modifier,portrait:Boolean=false) { Image(LocalArt.current.character(rank,portrait),"Rank ${RankNames[rank]} character",modifier,contentScale=if(portrait) ContentScale.Crop else ContentScale.Fit) }
@Composable private fun RankBadge(rank:Int,modifier:Modifier=Modifier.size(64.dp)) {
    Box(modifier,contentAlignment=Alignment.Center) { ArtIcon(3,Modifier.fillMaxSize()); Text(RankNames[rank],fontSize=30.sp,fontWeight=FontWeight.Bold,color=Pale,modifier=Modifier.offset(y=(-2).dp)) }
}
@Composable private fun XpBar(s:Progress,modifier:Modifier=Modifier) {
    Column(modifier,horizontalAlignment=Alignment.CenterHorizontally) {
        Text("${s.xp%200} / 200 XP",fontSize=13.sp,color=Pale)
        Spacer(Modifier.height(7.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Color(0xFF352749))) {
            val amount by animateFloatAsState((s.xp%200)/200f,label="experience")
            Box(Modifier.fillMaxWidth(amount).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color(0xFF8143D8),Color(0xFFD9B1FF)))))
        }
    }
}
@Composable private fun Home(s:Progress) {
    Column(Modifier.fillMaxSize().padding(bottom=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth(),contentAlignment=Alignment.Center) {
            val infinite=rememberInfiniteTransition(label="aura")
            val opacity by infinite.animateFloat(.25f,.5f,infiniteRepeatable(tween(2400),RepeatMode.Reverse),label="aura glow")
            val drift by infinite.animateFloat(-3f,3f,infiniteRepeatable(tween(2600),RepeatMode.Reverse),label="character motion")
            Box(Modifier.fillMaxWidth(.88f).aspectRatio(1f).background(Brush.radialGradient(listOf(Violet.copy(alpha=opacity),Color.Transparent))))
            Character(s.rank,Modifier.fillMaxHeight().fillMaxWidth(.60f).offset(y=drift.dp))
            Column(Modifier.align(Alignment.CenterStart),horizontalAlignment=Alignment.CenterHorizontally) { Text("RANK",fontSize=11.sp,color=Muted,letterSpacing=1.sp); RankBadge(s.rank) }
            Column(Modifier.align(Alignment.CenterEnd).width(62.dp),horizontalAlignment=Alignment.CenterHorizontally) { Text("LEVEL",fontSize=11.sp,color=Muted,letterSpacing=1.sp); Text("${s.level}",fontSize=34.sp,color=Pale,fontWeight=FontWeight.Light) }
        }
        XpBar(s,Modifier.fillMaxWidth(.82f).padding(top=10.dp,bottom=18.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Metric(4,String.format(Locale.US,"%,d",s.steps),"Steps",Modifier.weight(1f))
            Metric(5,String.format(Locale.US,"%.1f km",s.km),if(s.distanceEstimated) "Est. distance" else "Distance",Modifier.weight(1f))
            Metric(1,"${s.completed} / 5","Quests",Modifier.weight(1f))
        }
    }
}
@Composable private fun Metric(icon:Int,value:String,label:String,modifier:Modifier=Modifier) {
    Glass(modifier) { Column(Modifier.fillMaxWidth().padding(vertical=12.dp,horizontal=4.dp),horizontalAlignment=Alignment.CenterHorizontally) { ArtIcon(icon,Modifier.size(32.dp)); Text(value,fontSize=16.sp,color=Pale,fontWeight=FontWeight.Medium,maxLines=1); Text(label,fontSize=11.sp,color=Muted) } }
}
@Composable private fun BottomNav(selected:Int,onSelect:(Int)->Unit) {
    Glass(Modifier.fillMaxWidth().padding(top=8.dp,bottom=10.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical=5.dp)) {
            listOf("Home","Quests","Activity","Profile").forEachIndexed { index,name ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(if(index==selected) Violet.copy(alpha=.13f) else Color.Transparent).clickable { onSelect(index) }.semantics { contentDescription="$name tab"; this.selected=index==selected }.padding(vertical=8.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                    ArtIcon(index,Modifier.size(29.dp).alpha(if(index==selected) 1f else .6f)); Spacer(Modifier.height(3.dp)); Text(name,fontSize=10.sp,color=if(index==selected) Pale else Muted)
                }
            }
        }
    }
}
@Composable private fun Quests(s:Progress,onConfirm:(Int)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top=6.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Text("Daily quests",fontSize=22.sp,fontWeight=FontWeight.Medium)
        Text("Start light. Grow steadily.  +40 XP each",fontSize=12.sp,color=Muted)
        QuestRow(4,"Steps","${s.steps} / ${s.stepTarget}",s.steps.toFloat()/s.stepTarget,(s.earned and 1)!=0,null)
        QuestRow(5,if(s.distanceEstimated) "Distance (est.)" else "Distance",String.format(Locale.US,"%.1f / %.1f km",s.km,s.distanceTarget),(s.km/s.distanceTarget).toFloat(),(s.earned and 2)!=0,null)
        ExerciseNames.forEachIndexed { i,name -> QuestRow(6,name,"Target: ${s.reps} reps",if(s.earned and (4 shl i)!=0) 1f else 0f,s.earned and (4 shl i)!=0) {onConfirm(i)} }
        Text("Exercise completion is self-reported.",fontSize=11.sp,color=Muted,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
    }
}
@Composable private fun QuestRow(icon:Int,title:String,subtitle:String,progress:Float,done:Boolean,onComplete:(()->Unit)?) {
    Glass(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            ArtIcon(icon,Modifier.size(37.dp))
            Column(Modifier.weight(1f)) { Text(title,fontSize=15.sp); Text(subtitle,fontSize=12.sp,color=Muted); if(onComplete==null) { Spacer(Modifier.height(7.dp)); Box(Modifier.fillMaxWidth().height(5.dp).clip(CircleShape).background(Color(0xFF3B2A54))) {Box(Modifier.fillMaxWidth(progress.coerceIn(0f,1f)).fillMaxHeight().background(Violet))} } }
            if(done) Text("✓ Done",fontSize=12.sp,color=Color(0xFFD8C0FF)) else if(onComplete!=null) OutlinedButton(onClick=onComplete,contentPadding=PaddingValues(horizontal=10.dp,vertical=4.dp),shape=RoundedCornerShape(10.dp),border=BorderStroke(1.dp,Edge)) { Text("Complete",fontSize=11.sp,color=Pale) }
        }
    }
}
@Composable private fun ActivityPage(s:Progress,refresh:()->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top=8.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("Your activity",fontSize=24.sp,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
        Text(if(s.samsungOnly) "Source: Samsung Health" else "Source: Health Connect combined",fontSize=12.sp,color=Muted,modifier=Modifier.fillMaxWidth(),textAlign=TextAlign.Center)
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) { Metric(4,String.format(Locale.US,"%,d",s.steps),"Steps today",Modifier.weight(1f)); Metric(5,String.format(Locale.US,"%.1f km",s.km),if(s.distanceEstimated) "Est. distance" else "Distance",Modifier.weight(1f)) }
        Glass(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) { Text("Last import",fontSize=13.sp,color=Muted); Text(if(s.lastSync==0L) "Not synced yet" else Instant.ofEpochMilli(s.lastSync).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofPattern("MMM d, HH:mm")),fontSize=16.sp); Text(s.status,fontSize=12.sp,color=Muted) } }
        Action(if(s.busy) "Importing…" else "Refresh activity",refresh,enabled=!s.busy)
        Text("Your phone or watch records activity. ARISE imports today’s records on return and while open, without GPS recording.",fontSize=12.sp,color=Muted,lineHeight=19.sp)
    }
}
@Composable private fun Profile(s:Progress,connect:()->Unit,selectSamsung:(Boolean)->Unit,onRank:(Int)->Unit) {
    val context=LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top=8.dp,bottom=12.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
            Character(s.rank,Modifier.size(78.dp).clip(CircleShape).background(Color(0xFF34204B)).border(1.dp,Edge,CircleShape),portrait=true)
            Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("Your rank",fontSize=12.sp,color=Muted); RankBadge(s.rank,Modifier.size(55.dp)) }
            Column(horizontalAlignment=Alignment.CenterHorizontally) { Text("Level",fontSize=12.sp,color=Muted); Text("${s.level}",fontSize=32.sp) }
        }
        XpBar(s,Modifier.fillMaxWidth().padding(horizontal=20.dp))
        repeat(2) { row -> Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { repeat(3) { col -> val rank=row*3+col
            Glass(Modifier.weight(1f).clickable {onRank(rank)}.semantics { contentDescription="Rank ${RankNames[rank]}, level ${rank*10+1}, ${if(rank<=s.rank) "unlocked" else "locked"}" }) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) { Character(rank,Modifier.fillMaxWidth().height(91.dp).alpha(if(rank<=s.rank) 1f else .65f),true); Text(RankNames[rank]+if(rank>s.rank) " · Locked" else "",fontSize=if(rank>s.rank) 13.sp else 19.sp,color=Pale); Text("Level ${rank*10+1}",fontSize=10.sp,color=Muted,modifier=Modifier.padding(bottom=7.dp)) }
            }
        } } }
        Glass(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("Connected devices",fontSize=17.sp)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Text("Samsung Health only",modifier=Modifier.weight(1f),fontSize=14.sp); Switch(checked=s.samsungOnly,onCheckedChange=selectSamsung) }
                Text(if(s.samsungOnly) "Phone sources outside Samsung Health are excluded. Sync your watch in Samsung Health and allow it to share Steps with Health Connect." else "Combined total follows Health Connect source priorities.",fontSize=11.sp,color=Muted)
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) { Column(horizontalAlignment=Alignment.CenterHorizontally) {Text("▯",fontSize=38.sp,color=Violet);Text("Phone",fontSize=12.sp,color=Muted)};Text("↔",fontSize=24.sp,color=Violet);Column(horizontalAlignment=Alignment.CenterHorizontally) {ArtIcon(7,Modifier.size(44.dp));Text("Smartwatch",fontSize=12.sp,color=Muted)} }
                Action("Allow Health Connect access",connect)
                Text("Imports while open and when you return.",fontSize=10.sp,color=Muted)
                Text("Watch data requires sharing from its companion app.",fontSize=10.sp,color=Muted)
            }
        }
        Text(s.status,fontSize=11.sp,color=Muted)
        OutlinedButton(onClick={context.startActivity(Intent(context,PrivacyActivity::class.java))},modifier=Modifier.fillMaxWidth(),shape=RoundedCornerShape(14.dp),border=BorderStroke(1.dp,Edge)) { Text("Activity privacy  ›",color=Pale) }
    }
}
@Composable private fun Action(label:String,onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    Button(onClick=onClick,enabled=enabled,modifier=modifier.fillMaxWidth().heightIn(min=46.dp),shape=RoundedCornerShape(12.dp),colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF8952CE),contentColor=Color.White)) { Text(label,fontSize=13.sp,textAlign=TextAlign.Center) }
}
@Composable private fun ConfirmDialog(index:Int,reps:Int,dismiss:()->Unit,confirm:()->Unit) {
    Dialog(onDismissRequest=dismiss,properties=DialogProperties(usePlatformDefaultWidth=false)) {
        Glass(Modifier.padding(24.dp).widthIn(max=440.dp).fillMaxWidth()) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom=18.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                val art=LocalArt.current
                val image=when(index) {0->art.piece("pushup-reference.png",160,245,885,365);1->art.piece("situp.png");else->art.piece("squat.png")}
                Image(image,"${ExerciseNames[index]} illustration",Modifier.fillMaxWidth().heightIn(max=225.dp).aspectRatio(1.65f),contentScale=ContentScale.Fit)
                ArtIcon(6,Modifier.size(46.dp).padding(top=4.dp))
                Text("Complete ${ExerciseNames[index]}?",fontSize=23.sp,fontWeight=FontWeight.Medium,modifier=Modifier.padding(top=10.dp),textAlign=TextAlign.Center)
                Text("Don’t fool yourself.",fontSize=16.sp,color=Muted,modifier=Modifier.padding(top=12.dp,bottom=18.dp))
                Text("✓  I completed all $reps reps.",fontSize=13.sp,color=Pale)
                Row(Modifier.padding(horizontal=18.dp).padding(top=20.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick=dismiss,modifier=Modifier.weight(1f).heightIn(min=46.dp),shape=RoundedCornerShape(12.dp),border=BorderStroke(1.dp,Edge)) {Text("Not yet",color=Pale,fontSize=12.sp)}
                    Action("Yes, completed",confirm,Modifier.weight(1f))
                }
            }
        }
    }
}
@Composable private fun RankDialog(rank:Int,current:Int,dismiss:()->Unit) {
    Dialog(onDismissRequest=dismiss) { Glass { Column(Modifier.padding(22.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Character(rank,Modifier.fillMaxWidth().height(300.dp));Text("Rank ${RankNames[rank]}",fontSize=27.sp);Text(if(rank<=current) "Unlocked" else "Unlocks at level ${rank*10+1}",color=Muted,modifier=Modifier.padding(12.dp));Action("Continue",dismiss)
    } } }
}
class PrivacyActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState);setContent {AriseTheme {Column(Modifier.fillMaxSize().background(Ink).safeDrawingPadding().verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
        Text("Activity privacy",fontSize=26.sp)
        Text("ARISE reads today’s steps, total recorded distance from Health Connect. Your phone or watch’s companion app must share records there. Device support and source sync timing vary. When distance is unavailable, ARISE estimates it using 0.70 metres per step and labels it as estimated. Recorded distance may include other activities. Estimated distance also counts toward the distance quest.")
        Text("Push-ups, sit-ups and squats are self-reported. Confirm only the repetitions you performed. Illustrations are decorative and do not assess form.")
        Text("Imports run when you return and every minute while the app is open. ARISE does not record GPS, run a background sensor service, write health records or upload data. Your health app can keep recording while ARISE is closed.")
        Text("Progress stays on this phone. Revoke access in Health Connect settings. Clear ARISE storage to erase local progress. This Kotlin update preserves version 3 progress.")
        Text("ARISE is an unofficial personal fan app.",color=Muted)
        Action("Back",{finish()})
    }}}}
}
