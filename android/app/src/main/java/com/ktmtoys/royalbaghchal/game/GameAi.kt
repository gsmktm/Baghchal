package com.ktmtoys.royalbaghchal.game

import kotlin.math.abs
import kotlin.random.Random

enum class Difficulty { EASY, MEDIUM, HARD }

object GameAi {
    fun evaluate(game: GameState): Double {
        when(game.winner) {
            Winner.TIGER -> return 100_000.0
            Winner.GOAT -> return -100_000.0
            Winner.DRAW -> return 0.0
            null -> {}
        }
        val moves=Rules.legalMoves(game,Side.TIGER)
        val threats=moves.count { it.capture!=null }
        val positions=game.board.indices.filter { game.board[it]?.side==Side.TIGER }
        val stranded=positions.count { p -> moves.none { it.from==p } }
        val central=positions.sumOf {
            val (r,c)=Rules.rowCol(it)
            (2-abs(2-r))+(2-abs(2-c))
        }
        return game.goatsCaptured*190.0+moves.size*3.5+threats*26.0+
            central*3.0-stranded*55.0-game.goatsPlaced*3.0
    }
    private fun ranked(game: GameState, limit: Int): List<Move> {
        val all=Rules.legalMoves(game).map { it to evaluate(Rules.apply(game,it)) }
        val sorted=if(game.turn==Side.TIGER)all.sortedByDescending { it.second }
                   else all.sortedBy { it.second }
        return (if(limit>0)sorted.take(limit) else sorted).map { it.first }
    }
    private fun search(game:GameState,depth:Int,alpha0:Double,beta0:Double,beam:Int):Double{
        if(depth<=0||game.winner!=null)return evaluate(game)
        val moves=ranked(game,beam)
        if(moves.isEmpty())return if(game.turn==Side.TIGER)-100000.0 else 100000.0
        var alpha=alpha0;var beta=beta0
        if(game.turn==Side.TIGER){
            var value=Double.NEGATIVE_INFINITY
            for(move in moves){
                value=maxOf(value,search(Rules.apply(game,move),depth-1,alpha,beta,beam))
                alpha=maxOf(alpha,value)
                if(alpha>=beta)break
            }
            return value
        }
        var value=Double.POSITIVE_INFINITY
        for(move in moves){
            value=minOf(value,search(Rules.apply(game,move),depth-1,alpha,beta,beam))
            beta=minOf(beta,value)
            if(alpha>=beta)break
        }
        return value
    }
    fun choose(game:GameState,difficulty:Difficulty):Move?{
        val moves=Rules.legalMoves(game)
        if(moves.isEmpty())return null
        if(difficulty==Difficulty.EASY){
            val captures=moves.filter { it.capture!=null }
            return (if(captures.isNotEmpty()&&Random.nextFloat()<.65f)captures else moves).random()
        }
        val depth=if(difficulty==Difficulty.HARD)3 else 2
        val beam=if(difficulty==Difficulty.HARD)10 else 8
        var best:Move?=null
        var score=if(game.turn==Side.TIGER)Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        for(move in ranked(game,0)){
            val value=search(Rules.apply(game,move),depth-1,
                Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY,beam)
            if(best==null || (if(game.turn==Side.TIGER)value>score else value<score)){
                best=move;score=value
            }
        }
        return best
    }
}
