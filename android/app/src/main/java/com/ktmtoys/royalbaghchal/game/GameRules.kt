package com.ktmtoys.royalbaghchal.game

import kotlin.math.abs

enum class Side(val wire: String) { GOAT("goat"), TIGER("tiger");
    fun opponent(): Side = if (this == GOAT) TIGER else GOAT
    companion object { fun from(value: String?): Side? = entries.firstOrNull { it.wire == value } }
}
enum class Winner { GOAT, TIGER, DRAW;
    companion object { fun from(value: String?): Winner? = entries.firstOrNull { it.name.equals(value, true) } }
}
data class Piece(val side: Side, val id: String)
data class Move(val from: Int?, val to: Int, val capture: Int? = null)
data class PlayedMove(val move: Move, val side: Side, val ply: Int)
data class GameState(
    val board: List<Piece?>,
    val turn: Side = Side.GOAT,
    val goatsPlaced: Int = 0,
    val goatsCaptured: Int = 0,
    val winner: Winner? = null,
    val reason: String? = null,
    val ply: Int = 0,
    val lastMove: Move? = null,
    val counts: Map<String, Int> = emptyMap()
)
object Rules {
    const val GOAT_TOTAL = 20
    const val CAPTURE_TARGET = 5
    private val dirs = listOf(1 to 0, -1 to 0, 0 to 1, 0 to -1, 1 to 1, 1 to -1, -1 to 1, -1 to -1)
    fun initial(): GameState {
        val board = MutableList<Piece?>(25) { null }
        listOf(0, 4, 20, 24).forEachIndexed { index, p -> board[p] = Piece(Side.TIGER, "t${index + 1}") }
        return GameState(board)
    }
    fun point(row: Int, col: Int): Int = row * 5 + col
    fun rowCol(point: Int): Pair<Int, Int> = point / 5 to point % 5
    fun valid(row: Int, col: Int) = row in 0..4 && col in 0..4
    fun connected(a: Int, b: Int): Boolean {
        if(a !in 0..24 || b !in 0..24)return false
        val (ar, ac)=rowCol(a)
        val (br, bc)=rowCol(b)
        val dr=abs(ar-br); val dc=abs(ac-bc)
        return dr+dc==1 || (dr==1 && dc==1 && (ar+ac)%2==0)
    }
    fun neighbors(p: Int): List<Int> {
        val (r,c)=rowCol(p)
        return dirs.map { (dr,dc) -> r+dr to c+dc }
            .filter { (nr,nc)->valid(nr,nc)&&connected(p,point(nr,nc)) }
            .map { (nr,nc)->point(nr,nc) }
    }
    fun positionKey(state: GameState): String {
        val cells=state.board.joinToString("") { when(it?.side) {
            Side.TIGER -> "T"; Side.GOAT -> "G"; null -> "."
        } }
        return "${state.turn.wire}|${state.goatsPlaced}|${state.goatsCaptured}|$cells"
    }
    fun legalMoves(game: GameState, side: Side = game.turn): List<Move> {
        if(game.winner!=null)return emptyList()
        val board=game.board
        if(side==Side.GOAT && game.goatsPlaced<GOAT_TOTAL)
            return board.indices.filter { board[it]==null }.map { Move(null,it) }
        val result=mutableListOf<Move>()
        board.indices.forEach { from ->
            if(board[from]?.side!=side)return@forEach
            neighbors(from).forEach { to -> if(board[to]==null)result+=Move(from,to) }
            if(side!=Side.TIGER)return@forEach
            val (r,c)=rowCol(from)
            dirs.forEach { (dr,dc) ->
                val mr=r+dr;val mc=c+dc;val tr=r+2*dr;val tc=c+2*dc
                if(!valid(mr,mc)||!valid(tr,tc))return@forEach
                val middle=point(mr,mc);val to=point(tr,tc)
                if(connected(from,middle)&&connected(middle,to)&&
                    board[middle]?.side==Side.GOAT&&board[to]==null)result+=Move(from,to,middle)
            }
        }
        return result
    }
    fun apply(game: GameState, proposed: Move): GameState {
        require(game.winner==null){"Match finished"}
        val move=legalMoves(game).firstOrNull {
            it.from==proposed.from && it.to==proposed.to && it.capture==proposed.capture
        } ?: throw IllegalArgumentException("Illegal Baghchal move")
        val board=game.board.toMutableList()
        var placed=game.goatsPlaced;var captured=game.goatsCaptured
        if(move.from==null) {
            placed++;board[move.to]=Piece(Side.GOAT,"g$placed")
        } else {
            board[move.to]=board[move.from];board[move.from]=null
            if(move.capture!=null) { board[move.capture]=null;captured++ }
        }
        var next=game.copy(board=board,turn=game.turn.opponent(),goatsPlaced=placed,
            goatsCaptured=captured,winner=null,reason=null,ply=game.ply+1,lastMove=move)
        next=when {
            captured>=CAPTURE_TARGET -> next.copy(winner=Winner.TIGER,reason="captured")
            legalMoves(next).isEmpty() -> next.copy(
                winner=if(game.turn==Side.GOAT)Winner.GOAT else Winner.TIGER,
                reason=if(game.turn==Side.GOAT)"tigersBlocked" else "goatsBlocked"
            )
            placed==GOAT_TOTAL -> {
                val key=positionKey(next)
                val counts=game.counts+(key to ((game.counts[key]?:0)+1))
                next.copy(counts=counts,
                    winner=if((counts[key]?:0)>=3)Winner.DRAW else null,
                    reason=if((counts[key]?:0)>=3)"repetition" else null)
            }
            else -> next
        }
        return next
    }
}
