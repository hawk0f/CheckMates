package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.PieceColor
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class CorrespondenceRepositoryTest {

    @Test
    fun playersAlternateAndIllegalMovesAreRejected() = runTest {
        val path = Files.createTempFile("checkmates-correspondence", ".db").toString()
        val database = Db.init(path)
        val users = UserRepository(database)
        val white = assertIs<AuthResult.Success>(users.register("white", "secret123", "White"))
        val black = assertIs<AuthResult.Success>(users.register("black", "secret123", "Black"))
        val games = CorrespondenceRepository(database)
        val game = assertNotNull(games.create(white.profile.id, black.profile.id))

        assertEquals(
            CorrespondenceMoveResult.NotYourTurn,
            games.move(game.id, black.profile.id, "e7e5")
        )
        assertEquals(
            CorrespondenceMoveResult.IllegalMove,
            games.move(game.id, white.profile.id, "e2e5")
        )
        val first = assertIs<CorrespondenceMoveResult.Success>(
            games.move(game.id, white.profile.id, "e2e4")
        )
        assertEquals(PieceColor.BLACK, first.game.sideToMove)
        assertEquals(listOf("e2e4"), first.game.uciHistory)
        assertEquals(
            CorrespondenceMoveResult.NotYourTurn,
            games.move(game.id, white.profile.id, "d2d4")
        )
        val second = assertIs<CorrespondenceMoveResult.Success>(
            games.move(game.id, black.profile.id, "e7e5")
        )
        assertEquals(listOf("e2e4", "e7e5"), second.game.uciHistory)
    }

    @Test
    fun checkmateFinishesTheGame() = runTest {
        val path = Files.createTempFile("checkmates-correspondence-mate", ".db").toString()
        val database = Db.init(path)
        val users = UserRepository(database)
        val white = assertIs<AuthResult.Success>(users.register("white2", "secret123", "White"))
        val black = assertIs<AuthResult.Success>(users.register("black2", "secret123", "Black"))
        val games = CorrespondenceRepository(database)
        val game = assertNotNull(games.create(white.profile.id, black.profile.id))
        val moves = listOf(
            white.profile.id to "f2f3",
            black.profile.id to "e7e5",
            white.profile.id to "g2g4",
            black.profile.id to "d8h4"
        )

        var result: CorrespondenceMoveResult.Success? = null
        for ((userId, move) in moves) {
            result = assertIs(games.move(game.id, userId, move))
        }

        assertEquals(PieceColor.BLACK, result?.game?.winner)
        assertEquals(CorrespondenceMoveResult.Finished, games.move(game.id, white.profile.id, "e2e4"))
    }
}
