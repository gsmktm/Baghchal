package com.ktmtoys.royalbaghchal.ui

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.ktmtoys.royalbaghchal.L
import com.ktmtoys.royalbaghchal.Mode
import com.ktmtoys.royalbaghchal.RoyalGameViewModel
import com.ktmtoys.royalbaghchal.Screen
import com.ktmtoys.royalbaghchal.game.*
import com.ktmtoys.royalbaghchal.online.ArenaPlayer
import kotlinx.coroutines.launch

@Composable
fun RoyalApp(vm:RoyalGameViewModel,onPair:()->Unit,onDisconnectBle:()->Unit){
    val snack=remember{SnackbarHostState()}
    val scope=rememberCoroutineScope()
    LaunchedEffect(vm.notification){
        if(vm.notification.isNotBlank()){
            val message=vm.notification
            vm.clearNotice()
            snack.showSnackbar(message,withDismissAction=true)
        }
    }
    BackHandler(vm.screen!=Screen.HOME){vm.navigate(Screen.HOME)}
    RoyalBackdrop(Modifier.fillMaxSize().safeDrawingPadding()){
        Scaffold(
            modifier=Modifier.fillMaxSize(),
            containerColor=Color.Transparent,
            snackbarHost={SnackbarHost(snack)},
            bottomBar={RoyalBottomNav(vm)},
            contentWindowInsets=WindowInsets(0,0,0,0)
        ){padding->
            Column(Modifier.fillMaxSize().padding(padding)) {
                RoyalTop(vm)
                when(vm.screen){
                    Screen.HOME -> HomeScreen(vm)
                    Screen.PLAY -> PlayScreen(vm)
                    Screen.ARENA -> ArenaScreen(vm)
                    Screen.RANKS -> RankScreen(vm)
                    Screen.PROFILE -> ProfileScreen(vm)
                    Screen.SETTINGS -> SettingsScreen(vm)
                    Screen.RULES -> RulesScreen(vm)
                    Screen.BLUETOOTH -> BluetoothScreen(vm,onPair,onDisconnectBle)
                }
            }
        }
        if(vm.pendingLeave!=null)AlertDialog(
            onDismissRequest={vm.confirmNavigation(false)},
            containerColor=Royal.Panel,
            title={Text(vm.label("leave"),color=Royal.GoldLight)},
            text={Text(vm.label("confirmLeave"),color=Royal.Ivory)},
            confirmButton={TextButton(onClick={vm.confirmNavigation(true)}){
                Text(vm.label("yesLeave"),color=Royal.RubyLight)}},
            dismissButton={TextButton(onClick={vm.confirmNavigation(false)}){
                Text(vm.label("stay"),color=Royal.GoldLight)}}
        )
    }
}
@Composable
private fun RoyalTop(vm:RoyalGameViewModel){
    Row(
        Modifier.fillMaxWidth().height(65.dp).padding(horizontal=18.dp),
        verticalAlignment=Alignment.CenterVertically
    ){
        if(vm.screen!=Screen.HOME) {
            Text("‹",Modifier.size(39.dp).clickable{vm.navigate(Screen.HOME)},
                color=Royal.GoldLight,fontSize=32.sp,textAlign=TextAlign.Center)
        }else CrownEmblem(size=43.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)){
            Text(if(vm.language=="ne")"बाघचाल" else "ROYAL BAGHCHAL",
                color=Royal.GoldLight,fontWeight=FontWeight.ExtraBold,fontSize=19.sp)
            Text("KTM TOYS  ·  NEPAL",fontSize=9.sp,color=Royal.Fog,letterSpacing=2.sp)
        }
        Text("☾",fontSize=24.sp,color=Royal.GoldLight)
        Spacer(Modifier.width(8.dp))
        Text("☀",fontSize=22.sp,color=Royal.GoldLight)
    }
    HorizontalDivider(color=Royal.GoldDark.copy(alpha=.4f))
}
@Composable
private fun RoyalBottomNav(vm:RoyalGameViewModel){
    val nav=listOf(
        Triple(Screen.HOME,"home","⌂"),
        Triple(Screen.PLAY,"play","♜"),
        Triple(Screen.ARENA,"arena","◎"),
        Triple(Screen.PROFILE,"profile","♛")
    )
    Row(Modifier.fillMaxWidth().background(Royal.Navy2)
        .border(1.dp,Royal.GoldDark.copy(alpha=.35f))
        .padding(vertical=7.dp,horizontal=6.dp)){
        for((screen,label,icon) in nav){
            val active=when(screen){
                Screen.PROFILE -> vm.screen in listOf(Screen.PROFILE,Screen.SETTINGS,Screen.BLUETOOTH,Screen.RULES)
                else -> vm.screen==screen
            }
            Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                .background(if(active)Royal.GoldLight.copy(alpha=.15f) else Color.Transparent)
                .clickable {
                    if(screen==Screen.ARENA)vm.openArena()
                    else vm.navigate(screen)
                }
                .padding(vertical=6.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text(icon,fontSize=21.sp,color=if(active)Royal.GoldLight else Royal.Fog)
                Text(vm.label(label),fontSize=10.sp,
                    color=if(active)Royal.GoldLight else Royal.Fog,
                    fontWeight=if(active)FontWeight.Bold else FontWeight.Normal,
                    maxLines=1)
            }
        }
    }
}
@Composable
private fun ScreenTitle(title:String,description:String?=null){
    Column(Modifier.padding(top=8.dp,bottom=15.dp)){
        MicroLabel("✦  ROYAL  ·  KTM TOYS")
        Spacer(Modifier.height(3.dp))
        Text(title,style=MaterialTheme.typography.headlineSmall,color=Royal.Ivory)
        if(description!=null){
            Spacer(Modifier.height(5.dp))
            Text(description,color=Royal.Fog,fontSize=12.sp)
        }
    }
}
@Composable
private fun HomeScreen(vm:RoyalGameViewModel){
    var chooseSolo by remember { mutableStateOf(false) }
    var side by remember {mutableStateOf(vm.human)}
    var level by remember {mutableStateOf(vm.difficulty)}
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(horizontal=19.dp,vertical=12.dp),
        verticalArrangement=Arrangement.spacedBy(15.dp)){
        item {
            RoyalPanel(Modifier.fillMaxWidth(),padding=22.dp){
                Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.CenterHorizontally){
                    CrownEmblem(size=108.dp)
                    Spacer(Modifier.height(7.dp))
                    MicroLabel(vm.label("welcome"))
                    Spacer(Modifier.height(5.dp))
                    Text("बाघचाल",fontSize=51.sp,
                        fontWeight=FontWeight.ExtraBold,color=Royal.GoldLight)
                    Text("ROYAL BAGH CHAL",fontSize=15.sp,letterSpacing=3.sp,
                        fontWeight=FontWeight.Bold,color=Royal.Ivory)
                    Spacer(Modifier.height(9.dp))
                    Text(vm.label("subtitle"),textAlign=TextAlign.Center,
                        color=Royal.Fog,fontSize=13.sp)
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)){
                        StatPill("TIGERS","04",Modifier.weight(1f))
                        StatPill("GOATS","20",Modifier.weight(1f))
                        StatPill("POINTS","25",Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(18.dp))
                    GoldButton(vm.label("begin"),{chooseSolo=true},
                        Modifier.fillMaxWidth())
                    if(vm.game.ply>0 && vm.game.winner==null){
                        Spacer(Modifier.height(8.dp))
                        GoldButton(vm.label("continue"),{vm.navigate(Screen.PLAY)},
                            Modifier.fillMaxWidth(),primary=false)
                    }
                }
            }
        }
        item {MicroLabel(vm.label("explore"),Modifier.padding(top=5.dp))}
        item {
            ModeCard("♜",vm.label("solo"),vm.label("soloDesc"),Royal.GoldLight)
            {chooseSolo=true}
        }
        item {
            ModeCard("⚔",vm.label("local"),vm.label("localDesc"),Royal.Mint)
            {vm.startLocal()}
        }
        item {
            ModeCard("◎",vm.label("online"),vm.label("onlineDesc"),Royal.RubyLight)
            {vm.openArena()}
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("☾   "+vm.label("heritage")+"   ☀",Modifier.fillMaxWidth(),
                textAlign=TextAlign.Center,color=Royal.Gold,fontSize=12.sp)
            Spacer(Modifier.height(16.dp))
        }
    }
    if(chooseSolo)AlertDialog(
        onDismissRequest={chooseSolo=false},containerColor=Royal.Panel,
        title={Text(vm.label("solo"),color=Royal.GoldLight)},
        text={
            Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
                MicroLabel(vm.label("playAs"))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    ChoiceChip(vm.label("goat"),side==Side.GOAT,{side=Side.GOAT},Modifier.weight(1f))
                    ChoiceChip(vm.label("tiger"),side==Side.TIGER,{side=Side.TIGER},Modifier.weight(1f))
                }
                MicroLabel(vm.label("difficulty"))
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    for(d in Difficulty.entries){
                        ChoiceChip(vm.label(d.name.lowercase()),level==d,{level=d},Modifier.weight(1f))
                    }
                }
            }
        },
        confirmButton={
            GoldButton(vm.label("begin"),{
                chooseSolo=false;vm.startSolo(side,level)
            })
        },
        dismissButton={TextButton(onClick={chooseSolo=false}){
            Text(vm.label("close"),color=Royal.Fog)}}
    )
}
@Composable
private fun ModeCard(icon:String,title:String,description:String,color:Color,onClick:()->Unit){
    RoyalPanel(Modifier.fillMaxWidth().clickable(onClick=onClick),padding=16.dp){
        Row(verticalAlignment=Alignment.CenterVertically){
            Box(Modifier.size(57.dp).clip(RoundedCornerShape(15.dp))
                .background(color.copy(alpha=.14f))
                .border(1.dp,color.copy(alpha=.5f),RoundedCornerShape(15.dp)),
                contentAlignment=Alignment.Center){
                Text(icon,fontSize=30.sp,color=color)
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)){
                Text(title,fontWeight=FontWeight.Bold,fontSize=17.sp,color=Royal.Ivory)
                Text(description,fontSize=12.sp,color=Royal.Fog,maxLines=2)
            }
            Text("›",color=Royal.GoldLight,fontSize=30.sp)
        }
    }
}
@Composable
private fun ChoiceChip(text:String,active:Boolean,onClick:()->Unit,modifier:Modifier=Modifier){
    Box(modifier.heightIn(min=45.dp).clip(RoundedCornerShape(10.dp))
        .background(if(active)Royal.GoldLight else Royal.Navy2)
        .border(1.dp,if(active)Royal.Ivory else Royal.GoldDark,
            RoundedCornerShape(10.dp))
        .clickable(onClick=onClick).padding(7.dp),contentAlignment=Alignment.Center){
        Text(text,color=if(active)Royal.Navy else Royal.Ivory,
            fontSize=12.sp,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
    }
}
@Composable
private fun PlayScreen(vm:RoyalGameViewModel){
    val game=vm.game
    var showResult by remember(game.winner,game.ply){mutableStateOf(game.winner!=null)}
    val current=game.turn
    val phase=if(game.goatsPlaced<20)vm.label("placing") else vm.label("moving")
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=16.dp,vertical=13.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{
            RoyalPanel(Modifier.fillMaxWidth(),padding=13.dp){
                Row(verticalAlignment=Alignment.CenterVertically){
                    PieceToken(current,size=55.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)){
                        MicroLabel(vm.label("turn"))
                        Text(vm.label(current.wire),fontSize=22.sp,
                            color=Royal.Ivory,fontWeight=FontWeight.Bold)
                        Text(if(vm.thinking)vm.label("aiThinking") else phase,
                            color=Royal.Fog,fontSize=12.sp)
                    }
                    if(vm.onlineRoom!=null) {
                        Text(if(vm.onlineRoom?.ranked==true)vm.label("ranked") else vm.label("casual"),
                            color=Royal.Mint,fontSize=11.sp)
                    }else Text(if(vm.mode==Mode.SOLO)"AI" else "2P",color=Royal.GoldLight,
                        fontSize=17.sp,fontWeight=FontWeight.Bold)
                }
            }
        }
        item{
            RoyalBoard(game,vm.selected,vm.hint,vm.legal(),vm.canPlay(),vm.language){vm.tapCell(it)}
        }
        item{
            Text(
                when {
                    game.winner!=null -> vm.label("winSubtitle")
                    vm.thinking -> vm.label("aiThinking")
                    !vm.canPlay() -> vm.label("waitTurn")
                    game.turn==Side.GOAT && game.goatsPlaced<20 -> vm.label("putGoat")
                    else -> vm.label("select")
                },color=Royal.GoldLight,fontSize=12.sp,textAlign=TextAlign.Center,
                modifier=Modifier.fillMaxWidth().padding(vertical=3.dp))
        }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                StatPill(vm.label("placed"),"${game.goatsPlaced}/20",Modifier.weight(1f))
                StatPill(vm.label("capture"),"${game.goatsCaptured}/5",Modifier.weight(1f))
                StatPill(vm.label("moves"),"${game.ply}",Modifier.weight(1f))
            }
        }
        item{
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                GoldButton(vm.label("undo"),{vm.undo()},Modifier.weight(1f),
                    enabled=vm.mode!=Mode.ONLINE &&vm.undoStack.isNotEmpty()&&!vm.thinking,
                    primary=false)
                GoldButton(vm.label("hint"),{vm.requestHint()},Modifier.weight(1f),
                    enabled=vm.mode!=Mode.ONLINE &&vm.canPlay(),primary=false)
                GoldButton(vm.label("new"),{vm.newMatch()},Modifier.weight(1f),
                    enabled=vm.mode!=Mode.ONLINE)
            }
        }
        vm.onlineRoom?.let { room ->
            item {
                RoyalPanel(Modifier.fillMaxWidth()){
                    MicroLabel(vm.label("room")+" · "+room.code)
                    Spacer(Modifier.height(6.dp))
                    Text("${room.goats?.name?:"Waiting"}  🐐   vs   🐯  ${room.tigers?.name?:"Waiting"}",
                        color=Royal.Ivory)
                    Spacer(Modifier.height(6.dp))
                    Text("${vm.label("spectators")}: ${room.spectators}",
                        color=Royal.Fog,fontSize=12.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        GoldButton(vm.label("leave"),{vm.leaveRoom()},Modifier.weight(1f),primary=false)
                        if(room.status=="active"&&room.mySide!=null)
                            GoldButton(vm.label("resign"),{vm.resign()},Modifier.weight(1f),primary=false)
                        else if(room.status=="complete" && !room.ranked)
                            GoldButton(vm.label("rematch"),{vm.rematch()},Modifier.weight(1f))
                    }
                }
            }
        }
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                MicroLabel(vm.label("recent"))
                Spacer(Modifier.height(9.dp))
                if(vm.moves.isEmpty()){
                    Text(vm.label("noMoves"),color=Royal.Fog,fontSize=12.sp)
                } else vm.moves.takeLast(6).reversed().forEach { entry ->
                    val m=entry.move
                    Row(Modifier.fillMaxWidth().padding(vertical=4.dp),
                        verticalAlignment=Alignment.CenterVertically){
                        PieceToken(entry.side,size=28.dp)
                        Spacer(Modifier.width(9.dp))
                        Text("#${entry.ply}   "+
                            (if(m.from==null)"+" else "${m.from} → ")+
                            "${m.to}"+(if(m.capture!=null)"  ✦" else ""),
                            fontSize=12.sp,color=Royal.Ivory)
                    }
                }
            }
        }
        item{Spacer(Modifier.height(18.dp))}
    }
    if(showResult){
        AlertDialog(onDismissRequest={showResult=false},
            containerColor=Royal.Panel,
            icon={CrownEmblem(size=82.dp)},
            title={Text(vm.label(when(game.winner){
                Winner.GOAT->"winnerGoat";Winner.TIGER->"winnerTiger";else->"draw"
            }),color=Royal.GoldLight,textAlign=TextAlign.Center)},
            text={Text(vm.label("winSubtitle"),color=Royal.Ivory,textAlign=TextAlign.Center)},
            confirmButton={
                GoldButton(vm.label("playAgain"),{
                    showResult=false
                    if(vm.mode==Mode.ONLINE){
                        if(vm.onlineRoom?.ranked==false)vm.rematch() else vm.leaveRoom()
                    }else vm.newMatch()
                })
            },
            dismissButton={TextButton(onClick={showResult=false}){
                Text(vm.label("close"),color=Royal.Fog)}}
        )
    }
}
@Composable
private fun ArenaScreen(vm:RoyalGameViewModel){
    var roomCode by remember {mutableStateOf("")}
    var createSide by remember {mutableStateOf(Side.GOAT)}
    val clipboard=LocalClipboardManager.current
    LazyColumn(Modifier.fillMaxSize(),
        contentPadding=PaddingValues(horizontal=18.dp,vertical=15.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{ScreenTitle(vm.label("world"),vm.label("onlineDesc"))}
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                Row(verticalAlignment=Alignment.CenterVertically){
                    MicroLabel(vm.label("connection"),Modifier.weight(1f))
                    val ok=vm.onlineStatus=="connected"
                    Text("● "+vm.onlineStatus.uppercase(),
                        fontSize=11.sp,color=if(ok)Royal.Mint else Royal.RubyLight)
                }
                Spacer(Modifier.height(9.dp))
                RoyalField(vm.label("server"),vm.serverUrl,vm::setServer,
                    placeholder="wss://play.example.com/ws")
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    GoldButton(vm.label("connect"),{vm.connect()},Modifier.weight(1f))
                    GoldButton(vm.label("disconnect"),{vm.disconnect()},Modifier.weight(1f),primary=false)
                }
                if(vm.onlineError.isNotBlank()){
                    Spacer(Modifier.height(10.dp))
                    Text(vm.onlineError,color=Royal.RubyLight,fontSize=12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(vm.label("serverNote"),color=Royal.Fog,fontSize=12.sp)
            }
        }
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                MicroLabel("QUICK MATCH")
                Spacer(Modifier.height(7.dp))
                Text(vm.label("matchmaking"),fontWeight=FontWeight.Bold,
                    fontSize=19.sp,color=Royal.Ivory)
                Spacer(Modifier.height(10.dp))
                GoldButton(if(vm.queueing)vm.label("cancel") else vm.label("matchmaking"),
                    {if(vm.queueing)vm.cancelQueue() else vm.quickMatch()},
                    Modifier.fillMaxWidth(),enabled=vm.onlineStatus=="connected")
            }
        }
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                MicroLabel(vm.label("room"))
                Spacer(Modifier.height(9.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    ChoiceChip(vm.label("goats"),createSide==Side.GOAT,
                        {createSide=Side.GOAT},Modifier.weight(1f))
                    ChoiceChip(vm.label("tigers"),createSide==Side.TIGER,
                        {createSide=Side.TIGER},Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                GoldButton(vm.label("create"),{vm.createRoom(createSide)},
                    Modifier.fillMaxWidth(),enabled=vm.onlineStatus=="connected")
                Spacer(Modifier.height(15.dp))
                RoyalField(vm.label("roomCode"),roomCode,{roomCode=it.uppercase().take(6)},
                    placeholder="ABC234")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    GoldButton(vm.label("join"),{vm.joinRoom(roomCode)},
                        Modifier.weight(1f),enabled=vm.onlineStatus=="connected",primary=false)
                    GoldButton(vm.label("watch"),{vm.joinRoom(roomCode,true)},
                        Modifier.weight(1f),enabled=vm.onlineStatus=="connected",primary=false)
                }
            }
        }
        vm.onlineRoom?.let{room->
            item{
                RoyalPanel(Modifier.fillMaxWidth()){
                    MicroLabel(vm.label("room")+" · "+room.code)
                    Text(if(room.status=="waiting")vm.label("roomWaiting")
                        else room.status.uppercase(),color=Royal.Ivory)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                        GoldButton(vm.label("copy"),{
                            clipboard.setText(AnnotatedString(room.code))
                            vm.notify(vm.label("copy"))
                        },Modifier.weight(1f),primary=false)
                        GoldButton(vm.label("play"),{vm.navigate(Screen.PLAY)},
                            Modifier.weight(1f))
                    }
                }
            }
        }
        item{GoldButton(vm.label("leaderboard"),{vm.refreshRanking();vm.navigate(Screen.RANKS)},
            Modifier.fillMaxWidth(),primary=false)}
        item{Spacer(Modifier.height(16.dp))}
    }
}
@Composable
private fun RankScreen(vm:RoyalGameViewModel){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(11.dp)){
        item{ScreenTitle(vm.label("leaderboard"),"WORLD ARENA · GUEST ELO")}
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                Text(vm.label("guestNote"),color=Royal.Fog,fontSize=12.sp)
                Spacer(Modifier.height(10.dp))
                GoldButton("↻  "+vm.label("leaderboard"),
                    {vm.refreshRanking()},Modifier.fillMaxWidth(),primary=false)
            }
        }
        if(vm.leaderboard.isEmpty())item{
            RoyalPanel(Modifier.fillMaxWidth()){
                CrownEmblem(Modifier.align(Alignment.CenterHorizontally),size=70.dp)
                Text(vm.label("rankEmpty"),Modifier.fillMaxWidth(),color=Royal.Fog,
                    textAlign=TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                GoldButton(vm.label("connect"),{vm.openArena()},Modifier.fillMaxWidth())
            }
        }else itemsIndexed(vm.leaderboard){index,p->
            RoyalPanel(Modifier.fillMaxWidth(),padding=14.dp){
                Row(verticalAlignment=Alignment.CenterVertically){
                    Text("#${index+1}",fontSize=17.sp,color=Royal.GoldLight,
                        fontWeight=FontWeight.Bold)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)){
                        Text(p.name,color=Royal.Ivory,fontWeight=FontWeight.Bold)
                        Text(p.country+"  ·  "+p.matches+" games",
                            color=Royal.Fog,fontSize=12.sp)
                    }
                    Text("${p.rating}",color=Royal.GoldLight,fontSize=19.sp,
                        fontWeight=FontWeight.Bold)
                }
            }
        }
    }
}
@Composable
private fun ProfileScreen(vm:RoyalGameViewModel){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{ScreenTitle(vm.label("profile"),vm.label("heritage"))}
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                Row(verticalAlignment=Alignment.CenterVertically){
                    CrownEmblem(size=74.dp)
                    Spacer(Modifier.width(10.dp))
                    Column{
                        Text(vm.nickname,color=Royal.Ivory,fontSize=21.sp,
                            fontWeight=FontWeight.Bold)
                        Text(vm.country+" · "+vm.label("rating")+" "+vm.rating,
                            color=Royal.GoldLight,fontSize=13.sp)
                    }
                }
                Spacer(Modifier.height(17.dp))
                RoyalField(vm.label("name"),vm.nickname,vm::setNickname)
                Spacer(Modifier.height(9.dp))
                RoyalField(vm.label("country"),vm.country,vm::setCountry,placeholder="NP")
                Spacer(Modifier.height(9.dp))
                GoldButton(vm.label("save"),{vm.updateProfile();vm.notify(vm.label("save"))},
                    Modifier.fillMaxWidth())
            }
        }
        item{ModeCard("⚙",vm.label("settings"),vm.label("language"),Royal.GoldLight){
            vm.navigate(Screen.SETTINGS)
        }}
        item{ModeCard("⌁",vm.label("bluetooth"),vm.label("bluetoothStatus"),Royal.Mint){
            vm.navigate(Screen.BLUETOOTH)
        }}
        item{ModeCard("ⓘ",vm.label("rules"),vm.label("tradition"),Royal.RubyLight){
            vm.navigate(Screen.RULES)
        }}
        item{
            Text("ROYAL BAGHCHAL · ANDROID 1.0",Modifier.fillMaxWidth(),
                textAlign=TextAlign.Center,color=Royal.Fog,fontSize=10.sp,letterSpacing=1.5.sp)
        }
    }
}
@Composable
private fun SettingsScreen(vm:RoyalGameViewModel){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(14.dp)){
        item{ScreenTitle(vm.label("settings"),vm.label("heritage"))}
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                MicroLabel(vm.label("language"))
                Spacer(Modifier.height(11.dp))
                for(chunk in L.languages.chunked(3)){
                    Row(horizontalArrangement=Arrangement.spacedBy(7.dp),
                        modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)){
                        for((tag,name) in chunk)
                            ChoiceChip(name,vm.language==tag,{vm.setLanguage(tag)},Modifier.weight(1f))
                        repeat(3-chunk.size){Spacer(Modifier.weight(1f))}
                    }
                }
                Spacer(Modifier.height(11.dp))
                HorizontalDivider(color=Royal.GoldDark.copy(alpha=.4f))
                SettingSwitch(vm.label("sound"),vm.sounds,vm::setSound)
                SettingSwitch(vm.label("haptics"),vm.haptics,vm::setHaptics)
            }
        }
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                MicroLabel(vm.label("difficulty"))
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    for(d in Difficulty.entries)
                        ChoiceChip(vm.label(d.name.lowercase()),vm.difficulty==d,
                            {vm.setDifficulty(d)},Modifier.weight(1f))
                }
            }
        }
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                MicroLabel(vm.label("private"))
                Spacer(Modifier.height(10.dp))
                Text(vm.label("privateDesc"),fontSize=13.sp,color=Royal.Fog)
                Spacer(Modifier.height(10.dp))
                Text("● Offline AI/local play does not require Internet.\n"+
                    "● Bluetooth scan permission is requested only when pairing.\n"+
                    "● Online rooms use the server that you explicitly enter.",
                    color=Royal.Ivory,fontSize=12.sp,lineHeight=19.sp)
            }
        }
    }
}
@Composable
private fun SettingSwitch(title:String,checked:Boolean,onChecked:(Boolean)->Unit){
    Row(Modifier.fillMaxWidth().heightIn(min=54.dp),verticalAlignment=Alignment.CenterVertically){
        Text(title,Modifier.weight(1f),color=Royal.Ivory)
        Switch(checked=checked,onCheckedChange=onChecked,
            colors=SwitchDefaults.colors(checkedThumbColor=Royal.GoldLight,
                checkedTrackColor=Royal.GoldDark))
    }
}
@Composable
private fun RulesScreen(vm:RoyalGameViewModel){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{ScreenTitle(vm.label("rules"),vm.label("subtitle"))}
        itemsIndexed((1..6).toList()){index,item->
            RoyalPanel(Modifier.fillMaxWidth()){
                Row(verticalAlignment=Alignment.Top){
                    Text("${index+1}",color=Royal.GoldLight,fontSize=27.sp,
                        fontWeight=FontWeight.Bold)
                    Spacer(Modifier.width(17.dp))
                    Text(vm.label("rules$item"),color=Royal.Ivory,fontSize=14.sp,
                        lineHeight=22.sp)
                }
            }
        }
        item{GoldButton(vm.label("play"),{vm.navigate(Screen.PLAY)},
            Modifier.fillMaxWidth())}
    }
}
@Composable
private fun BluetoothScreen(vm:RoyalGameViewModel,onPair:()->Unit,onDisconnect:()->Unit){
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(18.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)){
        item{ScreenTitle(vm.label("bluetooth"),"ROYAL BOARD · BLE GATT")}
        item{
            RoyalPanel(Modifier.fillMaxWidth(),padding=20.dp){
                CrownEmblem(Modifier.align(Alignment.CenterHorizontally),size=87.dp)
                Spacer(Modifier.height(9.dp))
                MicroLabel(vm.label("bluetoothStatus"))
                Text(vm.bleStatus,color=Royal.Ivory,fontSize=18.sp,
                    fontWeight=FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Text(vm.label("bleNote"),color=Royal.Fog,fontSize=13.sp)
                Spacer(Modifier.height(14.dp))
                GoldButton(vm.label("blePair"),onPair,
                    Modifier.fillMaxWidth(),enabled=vm.mode!=Mode.ONLINE)
                Spacer(Modifier.height(8.dp))
                GoldButton(vm.label("bleDisconnect"),onDisconnect,
                    Modifier.fillMaxWidth(),primary=false)
            }
        }
        item{
            RoyalPanel(Modifier.fillMaxWidth()){
                MicroLabel("COMPATIBILITY")
                Spacer(Modifier.height(10.dp))
                Text(vm.label("bleMissing"),color=Royal.GoldLight)
                Text("BLE service: b0a6c7a1-…0001\n"+
                    "Write: …0002  ·  Notify: …0003\n"+
                    "Only paired boards using this protocol can send legal offline moves.",
                    color=Royal.Fog,fontSize=12.sp,lineHeight=18.sp)
            }
        }
    }
}
@Composable
private fun RoyalField(label:String,value:String,onValue:(String)->Unit,
    placeholder:String=""){
    OutlinedTextField(value=value,onValueChange=onValue,
        modifier=Modifier.fillMaxWidth(),singleLine=true,
        label={Text(label,fontSize=12.sp)},
        placeholder={Text(placeholder,color=Royal.Fog,fontSize=12.sp)},
        colors=OutlinedTextFieldDefaults.colors(
            focusedTextColor=Royal.Ivory,unfocusedTextColor=Royal.Ivory,
            focusedBorderColor=Royal.GoldLight,unfocusedBorderColor=Royal.GoldDark,
            focusedLabelColor=Royal.GoldLight,unfocusedLabelColor=Royal.Fog,
            cursorColor=Royal.GoldLight
        ))
}
