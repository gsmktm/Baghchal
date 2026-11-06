package com.ktmtoys.royalbaghchal.online

import android.os.Handler
import android.os.Looper
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import com.ktmtoys.royalbaghchal.game.*
import java.net.URI
import java.util.concurrent.TimeUnit

data class ArenaPlayer(
    val name:String,
    val country:String,
    val rating:Int,
    val connected:Boolean=true,
    val matches:Int=0,
    val wins:Int=0
)
data class ArenaRoom(
    val code:String, val status:String, val ranked:Boolean,
    val version:Int, val mySide:Side?,
    val goats:ArenaPlayer?, val tigers:ArenaPlayer?,
    val spectators:Int,val rematch:Int,
    val goatsTime:Long,val tigersTime:Long,
    val clockAt:Long,val game:GameState
)
fun parseGame(o:JSONObject):GameState{
    val board=o.getJSONArray("board")
    val pieces=(0 until board.length()).map { index ->
        val p=board.optJSONObject(index)
        if(p==null)null else Side.from(p.optString("side"))?.let { Piece(it,p.optString("id")) }
    }
    require(pieces.size==25){"Wrong board size"}
    val countsObject=o.optJSONObject("counts")
    val counts=mutableMapOf<String,Int>()
    if(countsObject!=null) {
        val names=countsObject.keys()
        while(names.hasNext()){ val key=names.next();counts[key]=countsObject.optInt(key) }
    }
    val m=o.optJSONObject("lastMove")
    val last=if(m!=null) Move(
        if(m.isNull("from"))null else m.optInt("from"),
        m.optInt("to"),
        if(m.isNull("capture"))null else m.optInt("capture")
    )else null
    return GameState(pieces,Side.from(o.optString("turn"))?:Side.GOAT,
        o.optInt("goatsPlaced"),o.optInt("goatsCaptured"),
        Winner.from(if(o.isNull("winner"))null else o.optString("winner")),
        if(o.isNull("reason"))null else o.optString("reason"),
        o.optInt("ply"),last,counts)
}
private fun player(o:JSONObject?):ArenaPlayer?{
    if(o==null)return null
    return ArenaPlayer(o.optString("name","Guest"),o.optString("country","XX"),
        o.optInt("rating",1200),o.optBoolean("connected",true),
        o.optInt("matches"),o.optInt("wins"))
}
fun parseRoom(o:JSONObject):ArenaRoom{
    val players=o.optJSONObject("players")
    val clock=o.optJSONObject("clock")
    val remaining=clock?.optJSONObject("remaining")
    return ArenaRoom(
        code=o.getString("code"),status=o.optString("status","waiting"),
        ranked=o.optBoolean("ranked"),version=o.optInt("version"),
        mySide=Side.from(if(o.isNull("mySide"))null else o.optString("mySide")),
        goats=player(players?.optJSONObject("goat")),
        tigers=player(players?.optJSONObject("tiger")),
        spectators=o.optInt("spectators"),rematch=o.optInt("rematch"),
        goatsTime=remaining?.optLong("goat",600000L)?:600000L,
        tigersTime=remaining?.optLong("tiger",600000L)?:600000L,
        clockAt=clock?.optLong("serverTime",System.currentTimeMillis())?:System.currentTimeMillis(),
        game=parseGame(o.getJSONObject("game"))
    )
}
class OnlineClient(
    private val tokenFor:(String)->String?,
    private val saveToken:(String,String)->Unit,
    private val onState:(String)->Unit,
    private val onEvent:(JSONObject)->Unit
){
    private val http=OkHttpClient.Builder().readTimeout(0,TimeUnit.MILLISECONDS)
        .pingInterval(15,TimeUnit.SECONDS).build()
    private val handler=Handler(Looper.getMainLooper())
    private var ws:WebSocket?=null
    private var active=false
    private var endpoint=""
    private var retries=0
    private val reconnect=Runnable { if(active)open() }
    companion object {
        fun validated(url:String):String{
            val uri=URI(url.trim())
            require(uri.scheme=="wss" && !uri.host.isNullOrBlank() &&
                uri.rawUserInfo==null && uri.rawFragment==null &&
                uri.rawQuery==null && uri.path=="/ws") {
                "Enter a secure server URL (wss://your-server/ws)"
            }
            return uri.toString()
        }
    }
    fun connect(url:String) {
        val checked=validated(url)
        disconnect()
        endpoint=checked;active=true;retries=0;open()
    }
    private fun open(){
        if(!active)return
        onState(if(retries==0)"connecting" else "reconnecting")
        try {
            val request=Request.Builder().url(endpoint).build()
            ws=http.newWebSocket(request,object:WebSocketListener(){
                override fun onOpen(webSocket:WebSocket,response:Response){
                    handler.post {
                        if(!active||webSocket!==ws)return@post
                        retries=0
                        onState("connected")
                        val greeting=JSONObject().put("type","hello")
                        val token=tokenFor(endpoint)
                        if(!token.isNullOrBlank())greeting.put("token",token)
                        webSocket.send(greeting.toString())
                    }
                }
                override fun onMessage(webSocket:WebSocket,text:String){
                    val message=try{JSONObject(text)}catch(_:Exception){return}
                    handler.post {
                        if(!active||ws!==webSocket)return@post
                        if(message.optString("type")=="welcome")
                            saveToken(endpoint,message.optString("token"))
                        onEvent(message)
                    }
                }
                override fun onFailure(webSocket:WebSocket,t:Throwable,response:Response?){
                    handler.post {
                        if(active&&ws===webSocket){
                            onState("network_error")
                            schedule()
                        }
                    }
                }
                override fun onClosed(webSocket:WebSocket,code:Int,reason:String){
                    handler.post {
                        if(active&&ws===webSocket){
                            onState("disconnected")
                            schedule()
                        }
                    }
                }
            })
        }catch(e:Exception){onState("network_error");schedule()}
    }
    private fun schedule(){
        if(!active)return
        handler.removeCallbacks(reconnect)
        val delay=(700L shl minOf(retries++,4)).coerceAtMost(12000L)
        handler.postDelayed(reconnect,delay)
    }
    fun send(type:String,attributes:JSONObject=JSONObject()):Boolean{
        val payload=attributes.put("type",type)
        return ws?.send(payload.toString())?:false
    }
    fun disconnect(){
        active=false;handler.removeCallbacks(reconnect)
        val old=ws;ws=null
        old?.close(1000,"Leaving")
        onState("offline")
    }
    fun close(){disconnect();http.dispatcher.executorService.shutdown()}
}
