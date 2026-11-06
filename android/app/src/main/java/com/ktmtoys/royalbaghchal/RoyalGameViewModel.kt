package com.ktmtoys.royalbaghchal

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ktmtoys.royalbaghchal.game.*
import com.ktmtoys.royalbaghchal.online.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

enum class Screen { HOME, PLAY, ARENA, RANKS, PROFILE, SETTINGS, RULES, BLUETOOTH }
enum class Mode { SOLO, LOCAL, ONLINE }
class RoyalGameViewModel(application:Application):AndroidViewModel(application){
    private val prefs=application.getSharedPreferences("royal_baghchal_v1",0)
    var screen by mutableStateOf(Screen.HOME);private set
    var mode by mutableStateOf(Mode.SOLO);private set
    var human by mutableStateOf(Side.GOAT);private set
    var difficulty by mutableStateOf(Difficulty.MEDIUM);private set
    var game by mutableStateOf(Rules.initial());private set
    var selected by mutableStateOf<Int?>(null);private set
    var hint by mutableStateOf<Move?>(null);private set
    var thinking by mutableStateOf(false);private set
    var moves by mutableStateOf<List<PlayedMove>>(emptyList());private set
    var undoStack by mutableStateOf<List<GameState>>(emptyList());private set
    var language by mutableStateOf(prefs.getString("lang","ne")?:"ne");private set
    var sounds by mutableStateOf(prefs.getBoolean("sounds",true));private set
    var haptics by mutableStateOf(prefs.getBoolean("haptics",true));private set
    var nickname by mutableStateOf(prefs.getString("nickname","Royal Player")?:"Royal Player");private set
    var country by mutableStateOf(prefs.getString("country","NP")?:"NP");private set
    var serverUrl by mutableStateOf(prefs.getString("server","")?:"");private set
    var onlineStatus by mutableStateOf("offline");private set
    var onlineError by mutableStateOf("");private set
    var queueing by mutableStateOf(false);private set
    var onlineRoom by mutableStateOf<ArenaRoom?>(null);private set
    var leaderboard by mutableStateOf<List<ArenaPlayer>>(emptyList());private set
    var rating by mutableStateOf(1200);private set
    var bleStatus by mutableStateOf("not connected");private set
    var notification by mutableStateOf("");private set
    var pendingLeave by mutableStateOf<Screen?>(null);private set
    var pendingMove by mutableStateOf(false);private set
    var bleEmitter:((Move,GameState)->Unit)?=null
    private var aiJob:Job?=null
    private val client=OnlineClient(
        tokenFor={prefs.getString("token:"+it,null)},
        saveToken={url,token -> if(token.matches(Regex("[a-f0-9]{64}")))
            prefs.edit().putString("token:"+url,token).apply()},
        onState={onlineStatus=it;if(it=="network_error")onlineError="Could not reach the game server"},
        onEvent={onEvent(it)}
    )
    init {
        try{
            val s=prefs.getString("local_game",null)
            if(!s.isNullOrBlank())game=parseGame(JSONObject(s))
        }catch(_:Exception){game=Rules.initial()}
        if(game.winner!=null)game=Rules.initial()
    }
    fun label(key:String)=L.text(language,key)
    fun notify(message:String){notification=message}
    fun clearNotice(){notification=""}
    fun navigate(next:Screen){
        if(mode==Mode.ONLINE && onlineRoom?.status=="active" &&
            next !in listOf(Screen.PLAY,Screen.ARENA,Screen.RANKS)){
            pendingLeave=next;return
        }
        screen=next
    }
    fun confirmNavigation(leave:Boolean){
        val next=pendingLeave
        pendingLeave=null
        if(leave){client.send("leave");onlineRoom=null;queueing=false;mode=Mode.SOLO;game=Rules.initial();screen=next?:Screen.HOME}
    }
    fun startSolo(side:Side=human,level:Difficulty=difficulty){
        stopAi()
        client.send("cancelQueue")
        if(onlineRoom!=null)client.send("leave")
        onlineRoom=null;queueing=false;mode=Mode.SOLO;human=side;difficulty=level
        newOfflineMatch();screen=Screen.PLAY;scheduleAi()
    }
    fun startLocal(){
        stopAi()
        if(onlineRoom!=null)client.send("leave")
        onlineRoom=null;queueing=false;mode=Mode.LOCAL
        newOfflineMatch();screen=Screen.PLAY
    }
    fun openArena(){stopAi();mode=Mode.ONLINE;screen=Screen.ARENA}
    private fun newOfflineMatch(){
        game=Rules.initial();selected=null;hint=null;moves=emptyList();undoStack=emptyList()
        saveLocal()
    }
    fun newMatch(){
        if(mode==Mode.ONLINE){notify(label("leaveRoomFirst"));return}
        stopAi();newOfflineMatch();scheduleAi()
    }
    fun setHuman(side:Side){human=side;startSolo(side,difficulty)}
    fun setDifficulty(d:Difficulty){difficulty=d;saveSettings()}
    fun setLanguage(tag:String){
        if(tag in listOf("ne","en","es","fr","de","zh","ja")){
            language=tag;saveSettings()
        }
    }
    fun setSound(value:Boolean){sounds=value;saveSettings()}
    fun setHaptics(value:Boolean){haptics=value;saveSettings()}
    fun setNickname(value:String){
        nickname=value.filter { it.isLetterOrDigit()||it.isWhitespace()||it in ".-_" }.trim().take(32).ifBlank { "Royal Player" }
        saveSettings()
    }
    fun setCountry(value:String){
        country=value.uppercase().filter { it in 'A'..'Z' }.take(2).ifBlank { "NP" }
        saveSettings()
    }
    fun setServer(value:String){serverUrl=value.trim();saveSettings()}
    private fun saveSettings(){
        prefs.edit().putString("lang",language).putString("nickname",nickname)
            .putString("country",country).putString("server",serverUrl)
            .putBoolean("sounds",sounds).putBoolean("haptics",haptics).apply()
    }
    private fun toJson(s:GameState):JSONObject{
        val board=JSONArray()
        s.board.forEach { p ->
            board.put(if(p==null)JSONObject.NULL else JSONObject().put("side",p.side.wire).put("id",p.id))
        }
        val o=JSONObject().put("board",board).put("turn",s.turn.wire)
            .put("goatsPlaced",s.goatsPlaced).put("goatsCaptured",s.goatsCaptured)
            .put("ply",s.ply).put("winner",s.winner?.name?.lowercase()?:JSONObject.NULL)
            .put("reason",s.reason?:JSONObject.NULL).put("counts",JSONObject(s.counts))
        s.lastMove?.let { m -> o.put("lastMove",JSONObject()
            .put("from",m.from?:JSONObject.NULL).put("to",m.to)
            .put("capture",m.capture?:JSONObject.NULL)) }
        return o
    }
    private fun saveLocal(){
        if(mode!=Mode.ONLINE)prefs.edit().putString("local_game",toJson(game).toString()).apply()
    }
    fun canPlay():Boolean {
        if(game.winner!=null ||thinking)return false
        if(mode==Mode.ONLINE)return onlineStatus=="connected"&&onlineRoom?.status=="active"&&
            onlineRoom?.mySide==game.turn&&!pendingMove
        if(mode==Mode.SOLO && game.turn!=human)return false
        return true
    }
    fun legal():List<Move> = if(canPlay())Rules.legalMoves(game) else emptyList()
    fun tapCell(position:Int){
        if(position !in 0..24||!canPlay())return
        val choices=legal()
        if(game.turn==Side.GOAT && game.goatsPlaced<Rules.GOAT_TOTAL){
            val move=choices.firstOrNull { it.from==null&&it.to==position }
            if(move!=null)commit(move)
            return
        }
        val move=choices.firstOrNull { it.from==selected&&it.to==position }
        if(selected!=null && move!=null){commit(move);return}
        selected=if(game.board[position]?.side==game.turn){
            if(selected==position)null else position
        }else null
        hint=null
    }
    private fun commit(move:Move){
        selected=null;hint=null
        if(mode==Mode.ONLINE){
            val version=onlineRoom?.version?:return
            if(client.send("move",JSONObject().put("version",version)
                    .put("from",move.from?:JSONObject.NULL).put("to",move.to)))pendingMove=true
            else notify(label("offline"))
            return
        }
        applyOffline(move)
    }
    private fun applyOffline(move:Move,fromBle:Boolean=false){
        val before=game
        val next=try{Rules.apply(before,move)}catch(_:Exception){return}
        undoStack=(undoStack+before).takeLast(120)
        moves=(moves+PlayedMove(move,before.turn,next.ply)).takeLast(120)
        game=next;selected=null;hint=null;saveLocal()
        if(!fromBle)bleEmitter?.invoke(move,next)
        scheduleAi()
    }
    fun bleMove(from:Int?,to:Int){
        if(mode==Mode.ONLINE||!canPlay())return
        Rules.legalMoves(game).find { it.from==from&&it.to==to }?.let{applyOffline(it,true)}
    }
    fun setBleStatus(value:String){bleStatus=value}
    fun undo(){
        if(mode==Mode.ONLINE||undoStack.isEmpty())return
        stopAi()
        var stack=undoStack.toMutableList()
        var record=moves.toMutableList()
        var restored=stack.removeAt(stack.lastIndex)
        if(record.isNotEmpty())record.removeAt(record.lastIndex)
        if(mode==Mode.SOLO && restored.turn!=human && stack.isNotEmpty()){
            restored=stack.removeAt(stack.lastIndex)
            if(record.isNotEmpty())record.removeAt(record.lastIndex)
        }
        game=restored;undoStack=stack;moves=record
        selected=null;hint=null;saveLocal();scheduleAi()
    }
    fun requestHint(){
        if(!canPlay())return
        val snapshot=game
        viewModelScope.launch {
            val suggestion=withContext(Dispatchers.Default){GameAi.choose(snapshot,Difficulty.MEDIUM)}
            if(game==snapshot)hint=suggestion
        }
    }
    private fun scheduleAi(){
        if(mode!=Mode.SOLO ||game.winner!=null||game.turn==human)return
        stopAi()
        val snapshot=game
        aiJob=viewModelScope.launch {
            thinking=true
            try{
                val move=withContext(Dispatchers.Default){GameAi.choose(snapshot,difficulty)}
                if(mode==Mode.SOLO && game==snapshot && move!=null)applyOffline(move)
            } finally {thinking=false}
        }
    }
    private fun stopAi(){aiJob?.cancel();aiJob=null;thinking=false}
    fun connect(){
        onlineError=""
        try{client.connect(serverUrl)}catch(e:Exception){onlineError=e.message?:"Invalid secure server"}
    }
    fun disconnect(){client.disconnect();onlineRoom=null;queueing=false;pendingMove=false}
    fun quickMatch(){if(client.send("quick")){queueing=true;onlineError=""}else notify(label("offline"))}
    fun cancelQueue(){client.send("cancelQueue");queueing=false}
    fun createRoom(side:Side){if(!client.send("create",JSONObject().put("side",side.wire)))notify(label("offline"))}
    fun joinRoom(code:String,spectate:Boolean=false){
        val safe=code.uppercase().filter { it in 'A'..'Z' || it in '2'..'9' }.take(6)
        if(safe.length!=6){notify(label("invalidCode"));return}
        client.send(if(spectate)"spectate" else "join",JSONObject().put("code",safe))
    }
    fun resign(){client.send("resign")}
    fun leaveRoom(){client.send("leave");queueing=false}
    fun rematch(){client.send("rematch")}
    fun refreshRanking(){client.send("leaderboard")}
    fun updateProfile(){client.send("profile",JSONObject().put("name",nickname).put("country",country))}
    private fun onEvent(message:JSONObject){
        when(message.optString("type")){
            "welcome" -> {
                val profile=message.optJSONObject("profile")
                rating=profile?.optInt("rating",1200)?:1200
                refreshRanking();updateProfile()
            }
            "profile" -> rating=message.optJSONObject("profile")?.optInt("rating",rating)?:rating
            "snapshot" -> try {
                val room=parseRoom(message.getJSONObject("room"))
                onlineRoom=room;queueing=false;pendingMove=false
                game=room.game;selected=null;hint=null
                mode=Mode.ONLINE;screen=Screen.PLAY
            }catch(_:Exception){onlineError="Invalid server snapshot"}
            "queue" -> queueing=message.optBoolean("waiting")
            "left" -> {onlineRoom=null;pendingMove=false;queueing=false;game=Rules.initial();screen=Screen.ARENA}
            "leaderboard" -> {
                val list=message.optJSONArray("players")?:JSONArray()
                leaderboard=(0 until list.length()).mapNotNull { index ->
                    val p=list.optJSONObject(index)?:return@mapNotNull null
                    ArenaPlayer(p.optString("name","Guest"),p.optString("country","XX"),
                        p.optInt("rating",1200),true,p.optInt("matches"),p.optInt("wins"))
                }
            }
            "rating" -> {rating+=message.optInt("delta");refreshRanking()}
            "error" -> {pendingMove=false;onlineError=message.optString("code","Server error").replace('_',' ')}
        }
    }
    override fun onCleared(){stopAi();client.close();super.onCleared()}
}
