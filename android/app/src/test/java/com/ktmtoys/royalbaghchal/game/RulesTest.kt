package com.ktmtoys.royalbaghchal.game
import org.junit.Assert.*
import org.junit.Test

class RulesTest {
    @Test fun openingFourTigersAndTwentyPlacements(){
        val game=Rules.initial()
        assertEquals(Side.GOAT,game.turn)
        assertEquals(listOf(0,4,20,24),game.board.indices.filter { game.board[it]?.side==Side.TIGER })
        assertEquals(21,Rules.legalMoves(game).size)
        assertTrue(Rules.legalMoves(game).all { it.from==null })
    }
    @Test fun diagonalConnectionOnlyOnEvenParity(){
        assertTrue(Rules.connected(0,6))
        assertTrue(Rules.connected(6,12))
        assertFalse(Rules.connected(1,7))
        assertFalse(Rules.connected(0,12))
        assertTrue(Rules.connected(11,12))
    }
    @Test fun illegalMovesRejectAndPreserveOriginal(){
        val start=Rules.initial()
        try { Rules.apply(start,Move(0,2));fail("illegal tiger turn") }
        catch(_:IllegalArgumentException){}
        assertEquals(0,start.ply)
        assertEquals(Side.GOAT,start.turn)
    }
    @Test fun tigerCaptureUsingTwoConnectedDiagonals(){
        val afterPlace=Rules.apply(Rules.initial(),Move(null,6))
        assertEquals(Side.TIGER,afterPlace.turn)
        assertTrue(Rules.legalMoves(afterPlace).contains(Move(0,12,6)))
        val captured=Rules.apply(afterPlace,Move(0,12,6))
        assertNull(captured.board[6])
        assertEquals(Side.TIGER,captured.board[12]?.side)
        assertEquals(1,captured.goatsCaptured)
        assertEquals(2,captured.ply)
    }
    @Test fun localComputerChoicesAreLegal(){
        val after=Rules.apply(Rules.initial(),Move(null,12))
        for(level in Difficulty.entries){
            val choice=GameAi.choose(after,level)
            assertNotNull(choice)
            assertTrue(Rules.legalMoves(after).contains(choice))
        }
    }
    @Test fun immutableHistoryForUndo(){
        val start=Rules.initial()
        val after=Rules.apply(start,Move(null,12))
        assertNull(start.board[12])
        assertEquals(Side.GOAT,after.board[12]?.side)
        assertEquals(1,after.goatsPlaced)
        assertEquals(0,start.goatsPlaced)
    }
}
