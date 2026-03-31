package com.koval.tiktaktoegame.integration

import com.koval.tiktaktoegame.domain.GameStatus
import com.koval.tiktaktoegame.dto.request.JoinGameRequest
import com.koval.tiktaktoegame.dto.request.MakeMoveRequest
import com.koval.tiktaktoegame.dto.request.RegisterRequest
import com.koval.tiktaktoegame.exception.InvalidMoveException
import com.koval.tiktaktoegame.service.GameService
import com.koval.tiktaktoegame.service.PlayerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class GameServiceIT : AbstractIT() {

    @Autowired
    private lateinit var playerService: PlayerService

    @Autowired
    private lateinit var gameService: GameService

    private var aliceId: Long = 0L
    private var bobId: Long = 0L

    @BeforeEach
    fun setup() {
        aliceId = playerService.register(RegisterRequest("alice", "pass")).playerId
        bobId = playerService.register(RegisterRequest("bob", "pass")).playerId
    }

    @Test
    fun `joinGame creates WAITING game for the first player`() {
        val response = gameService.joinGame(JoinGameRequest(aliceId, "pass"))

        assertThat(response.status).isEqualTo(GameStatus.WAITING)
        assertThat(response.yourSymbol).isEqualTo("X")
        assertThat(response.yourTurn).isFalse()
    }

    @Test
    fun `joinGame transitions to IN_PROGRESS when second player joins`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        val response = gameService.joinGame(JoinGameRequest(bobId, "pass"))

        assertThat(response.status).isEqualTo(GameStatus.IN_PROGRESS)
        assertThat(response.yourSymbol).isEqualTo("O")
        assertThat(response.yourTurn).isFalse() // X always moves first
    }

    @Test
    fun `getGame returns game state with board and player context`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        val joined = gameService.joinGame(JoinGameRequest(bobId, "pass"))

        val state = gameService.getGame(joined.gameId, requestingPlayerId = aliceId)

        assertThat(state.board).hasSize(5)
        assertThat(state.yourSymbol).isEqualTo("X")
        assertThat(state.yourTurn).isTrue()
        assertThat(state.availableMoves).hasSize(9)
    }

    @Test
    fun `getGamesForPlayer returns all games the player is involved in`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        gameService.joinGame(JoinGameRequest(bobId, "pass"))

        val games = gameService.getGamesForPlayer(aliceId)

        assertThat(games).hasSize(1)
        assertThat(games[0].opponentUsername).isEqualTo("bob")
        assertThat(games[0].status).isEqualTo(GameStatus.IN_PROGRESS)
    }

    @Test
    fun `makeMove applies move and advances turn`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        val joined = gameService.joinGame(JoinGameRequest(bobId, "pass"))
        val gameId = joined.gameId

        val result = gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 0, 0))

        assertThat(result.status).isEqualTo(GameStatus.IN_PROGRESS)
        assertThat(result.nextTurn).isEqualTo("O")
    }

    @Test
    fun `makeMove detects X win`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        val joined = gameService.joinGame(JoinGameRequest(bobId, "pass"))
        val gameId = joined.gameId

        // X: (0,0), O: (1,0), X: (0,1), O: (1,1), X: (0,2) → X wins top row
        gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 0, 0))
        gameService.makeMove(gameId, MakeMoveRequest(bobId,   "pass", 1, 0))
        gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 0, 1))
        gameService.makeMove(gameId, MakeMoveRequest(bobId,   "pass", 1, 1))
        val result = gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 0, 2))

        assertThat(result.status).isEqualTo(GameStatus.FINISHED)
        assertThat(result.winner).isEqualTo("X")
    }

    @Test
    fun `makeMove detects draw`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        val joined = gameService.joinGame(JoinGameRequest(bobId, "pass"))
        val gameId = joined.gameId

        // Fill board to a draw: X O X / X X O / O X O
        // Moves: (0,0)X (0,1)O (0,2)X (1,0)X (1,1)X (1,2)O (2,0)O (2,1)X (2,2)O
        gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 0, 0))
        gameService.makeMove(gameId, MakeMoveRequest(bobId,   "pass", 0, 1))
        gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 0, 2))
        gameService.makeMove(gameId, MakeMoveRequest(bobId,   "pass", 2, 0))
        gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 1, 0))
        gameService.makeMove(gameId, MakeMoveRequest(bobId,   "pass", 1, 2))
        gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 1, 1))
        gameService.makeMove(gameId, MakeMoveRequest(bobId,   "pass", 2, 2))
        val result = gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 2, 1))

        assertThat(result.status).isEqualTo(GameStatus.FINISHED)
        assertThat(result.winner).isNull()
    }

    @Test
    fun `makeMove throws InvalidMoveException when cell is already occupied`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        val joined = gameService.joinGame(JoinGameRequest(bobId, "pass"))
        val gameId = joined.gameId

        gameService.makeMove(gameId, MakeMoveRequest(aliceId, "pass", 0, 0))

        assertThrows<InvalidMoveException> {
            gameService.makeMove(gameId, MakeMoveRequest(bobId, "pass", 0, 0))
        }
    }

    @Test
    fun `makeMove throws InvalidMoveException when it is not the player's turn`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        val joined = gameService.joinGame(JoinGameRequest(bobId, "pass"))
        val gameId = joined.gameId

        assertThrows<InvalidMoveException> {
            gameService.makeMove(gameId, MakeMoveRequest(bobId, "pass", 0, 0))
        }
    }

    @Test
    fun `player cannot join their own waiting game`() {
        gameService.joinGame(JoinGameRequest(aliceId, "pass"))

        // Alice tries to join again — should create another WAITING game, not join her own
        val second = gameService.joinGame(JoinGameRequest(aliceId, "pass"))
        assertThat(second.status).isEqualTo(GameStatus.WAITING)
    }
}
