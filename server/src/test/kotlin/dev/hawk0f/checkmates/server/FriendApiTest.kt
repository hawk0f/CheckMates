package dev.hawk0f.checkmates.server

import dev.hawk0f.checkmates.shared.domain.GameOverReason
import dev.hawk0f.checkmates.shared.domain.PieceColor
import dev.hawk0f.checkmates.shared.protocol.AddFriendRequest
import dev.hawk0f.checkmates.shared.protocol.ChallengeRequest
import dev.hawk0f.checkmates.shared.protocol.ChallengeResponse
import dev.hawk0f.checkmates.shared.protocol.FriendSummary
import dev.hawk0f.checkmates.shared.protocol.FriendsResponse
import dev.hawk0f.checkmates.shared.protocol.GameRecordRequest
import dev.hawk0f.checkmates.shared.protocol.PushTokenRequest
import dev.hawk0f.checkmates.shared.protocol.TimeControl
import java.io.File
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class FriendApiTest {

    private class Fixture {
        val database = run {
            val file = File.createTempFile("friends-${UUID.randomUUID()}", ".db")
            file.delete()
            file.deleteOnExit()
            Db.init(file.absolutePath)
        }
        val users = UserRepository(database)
        val friends = FriendRepository(database)
    }

    private suspend fun newUser(users: UserRepository, login: String, name: String): Long =
        (users.register(login, "password123", name) as AuthResult.Success).profile.id

    @Test
    fun aPlayerCanBeAddedByDisplayName() = runTest {
        val fixture = Fixture()
        val me = newUser(fixture.users, "annalogin", "Anna")
        newUser(fixture.users, "borislogin", "Boris")

        val added = assertNotNull(fixture.friends.add(me, "Boris"))
        assertEquals("Boris", added.displayName)
        assertEquals(listOf("Boris"), fixture.friends.list(me).map(FriendSummary::displayName))
    }

    @Test
    fun anAmbiguousDisplayNameIsRejectedButTheLoginResolves() = runTest {
        val fixture = Fixture()
        val me = newUser(fixture.users, "annalogin", "Anna")
        newUser(fixture.users, "borislogin", "Boris")
        val second = newUser(fixture.users, "boris2login", "Boris")

        assertNull(fixture.friends.add(me, "Boris"))
        val added = assertNotNull(fixture.friends.add(me, "boris2login"))
        assertEquals(second, added.userId)
        assertEquals("boris2login", added.login)
        assertTrue(fixture.friends.isFriend(me, second))
        assertFalse(fixture.friends.isFriend(second, me))
    }

    @Test
    fun addingAnUnknownNameOrYourselfFails() = runTest {
        val fixture = Fixture()
        val me = newUser(fixture.users, "annalogin", "Anna")

        assertNull(fixture.friends.add(me, "Nobody"))
        assertNull(fixture.friends.add(me, "Anna"))
        assertTrue(fixture.friends.list(me).isEmpty())
    }

    @Test
    fun addingTheSameFriendTwiceKeepsOneEntry() = runTest {
        val fixture = Fixture()
        val me = newUser(fixture.users, "annalogin", "Anna")
        newUser(fixture.users, "borislogin", "Boris")

        fixture.friends.add(me, "Boris")
        fixture.friends.add(me, "Boris")
        assertEquals(1, fixture.friends.list(me).size)
    }

    @Test
    fun friendsCanBeRemoved() = runTest {
        val fixture = Fixture()
        val me = newUser(fixture.users, "annalogin", "Anna")
        val boris = newUser(fixture.users, "borislogin", "Boris")

        fixture.friends.add(me, "Boris")
        assertTrue(fixture.friends.remove(me, boris))
        assertTrue(fixture.friends.list(me).isEmpty())
        assertFalse(fixture.friends.remove(me, boris))
    }

    @Test
    fun recentOpponentsComeFromFinishedGamesNewestFirst() = runTest {
        val fixture = Fixture()
        val me = newUser(fixture.users, "annalogin", "Anna")
        newUser(fixture.users, "borislogin", "Boris")

        fixture.users.insertGame(
            me,
            GameRecordRequest(
                mode = "online",
                myColor = PieceColor.WHITE,
                whiteName = "Anna",
                blackName = "Boris",
                winner = PieceColor.WHITE,
                reason = GameOverReason.CHECKMATE,
                uciHistory = listOf("e2e4"),
                finishedAtMillis = 1_000
            )
        )
        fixture.users.insertGame(
            me,
            GameRecordRequest(
                mode = "online",
                myColor = PieceColor.BLACK,
                whiteName = "Clara",
                blackName = "Anna",
                winner = null,
                reason = GameOverReason.DRAW_AGREED,
                uciHistory = listOf("d2d4"),
                finishedAtMillis = 2_000
            )
        )

        val recent = fixture.friends.recentOpponents(me)
        assertEquals(listOf("Clara", "Boris"), recent.map(FriendSummary::displayName))
        assertEquals(2_000, recent.first().lastPlayedMillis)
        assertEquals(-1, recent.first().userId)
    }

    @Test
    fun pushTokensAreStoredPerUser() = runTest {
        val fixture = Fixture()
        val me = newUser(fixture.users, "annalogin", "Anna")

        assertNull(fixture.friends.pushTokenOf(me))
        fixture.friends.savePushToken(me, "device-token")
        assertEquals("device-token", fixture.friends.pushTokenOf(me))
    }

    @Test
    fun aChallengeCreatesAJoinableRoomForTheChallenger() = runTest {
        val fixture = Fixture()
        val me = newUser(fixture.users, "annalogin", "Anna")
        val registry = RoomRegistry("http://localhost")

        val created = registry.create(
            hostName = fixture.friends.displayNameOf(me).orEmpty(),
            timeControl = TimeControl(300, 3),
            hostUserId = me
        )
        val room = assertNotNull(registry.byCode(created.shortCode))
        assertEquals(RoomStatus.WAITING_FOR_GUEST, room.status)
        assertEquals("Anna", room.hostName)

        val response = ChallengeResponse(
            gameId = created.gameId,
            shortCode = created.shortCode,
            joinUrl = registry.joinUrl(created.shortCode),
            playerToken = created.playerToken,
            pushed = false
        )
        assertTrue(response.joinUrl.endsWith(created.shortCode))
    }

    @Test
    fun requestPayloadsCarryWhatTheApiNeeds() {
        assertEquals("Boris", AddFriendRequest("Boris").query)
        assertEquals(7L, ChallengeRequest(7L).friendUserId)
        assertEquals("abc", PushTokenRequest("abc").token)
        assertTrue(FriendsResponse(emptyList(), emptyList()).friends.isEmpty())
    }
}
