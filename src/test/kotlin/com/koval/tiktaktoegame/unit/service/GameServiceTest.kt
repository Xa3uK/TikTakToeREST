package com.koval.tiktaktoegame.unit.service

import com.koval.tiktaktoegame.api.dto.request.JoinGameRequest
import com.koval.tiktaktoegame.api.dto.request.MakeMoveRequest
import com.koval.tiktaktoegame.domain.exception.ConcurrentUpdateException
import com.koval.tiktaktoegame.domain.exception.GameNotFoundException
import com.koval.tiktaktoegame.domain.exception.InvalidMoveException
import com.koval.tiktaktoegame.domain.model.Game
import com.koval.tiktaktoegame.domain.model.GameStatus
import com.koval.tiktaktoegame.domain.model.Player
import com.koval.tiktaktoegame.domain.repository.GameRepository
import com.koval.tiktaktoegame.domain.repository.PlayerRepository
import com.koval.tiktaktoegame.domain.service.GameService
import com.koval.tiktaktoegame.domain.service.PlayerService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.dao.OptimisticLockingFailureException
import java.util.Optional

class GameServiceTest {

    private val gameRepository: GameRepository = mockk()
    private val playerRepository: PlayerRepository = mockk()
    private val playerService: PlayerService = mockk()
    private val gameService = GameService(gameRepository, playerRepository, playerService)

    private val playerX = Player(id = 1L, username = "alice", password = "hashed")
    private val playerO = Player(id = 2L, username = "bob", password = "hashed")

    private fun activeGame(board: String = "_________", nextPlayerId: Long = 1L) = Game(
        id = 1L,
        board = board,
        status = GameStatus.IN_PROGRESS,
        playerXId = 1L,
        playerOId = 2L,
        nextPlayerId = nextPlayerId
    )

    private fun captureAndReturn() {
        val slot = slot<Game>()
        every { gameRepository.save(capture(slot)) } answers { slot.captured }
    }

    // --- joinGame ---

    @Test
    fun `joinGame creates WAITING game when no existing game is available`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findFirstWaitingGameNotOwnedBy(1L) } returns null
        every { gameRepository.save(any()) } answers { firstArg<Game>().copy(id = 1L) }

        val result = gameService.joinGame(JoinGameRequest(1L, "secret"))

        assertThat(result.status).isEqualTo(GameStatus.WAITING)
        assertThat(result.yourSymbol).isEqualTo("X")
        assertThat(result.yourTurn).isFalse()
    }

    @Test
    fun `joinGame joins existing WAITING game and transitions to IN_PROGRESS`() {
        val waitingGame = Game(id = 2L, playerXId = 1L, status = GameStatus.WAITING)
        every { playerService.authenticate(2L, "secret") } returns playerO
        every { gameRepository.findFirstWaitingGameNotOwnedBy(2L) } returns waitingGame
        captureAndReturn()

        val result = gameService.joinGame(JoinGameRequest(2L, "secret"))

        assertThat(result.status).isEqualTo(GameStatus.IN_PROGRESS)
        assertThat(result.yourSymbol).isEqualTo("O")
        assertThat(result.yourTurn).isFalse()
    }

    @Test
    fun `joinGame throws ConcurrentUpdateException on optimistic lock failure`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findFirstWaitingGameNotOwnedBy(1L) } returns null
        every { gameRepository.save(any()) } throws OptimisticLockingFailureException("conflict")

        assertThrows<ConcurrentUpdateException> {
            gameService.joinGame(JoinGameRequest(1L, "secret"))
        }
    }

    // --- getGame ---

    @Test
    fun `getGame returns response with player context when playerId provided`() {
        every { gameRepository.findById(1L) } returns Optional.of(activeGame())

        val result = gameService.getGame(1L, requestingPlayerId = 1L)

        assertThat(result.gameId).isEqualTo(1L)
        assertThat(result.yourSymbol).isEqualTo("X")
        assertThat(result.yourTurn).isTrue()
        assertThat(result.board).hasSize(5)
        assertThat(result.availableMoves).hasSize(9)
    }

    @Test
    fun `getGame returns response without player context when no playerId given`() {
        every { gameRepository.findById(1L) } returns Optional.of(activeGame())

        val result = gameService.getGame(1L, requestingPlayerId = null)

        assertThat(result.yourSymbol).isNull()
        assertThat(result.yourTurn).isNull()
        assertThat(result.availableMoves).isNull()
    }

    @Test
    fun `getGame renders board correctly`() {
        every { gameRepository.findById(1L) } returns Optional.of(activeGame(board = "X________"))

        val result = gameService.getGame(1L, requestingPlayerId = null)

        assertThat(result.board[0]).isEqualTo("X |   |  ")
        assertThat(result.board[1]).isEqualTo("--+---+--")
        assertThat(result.board[2]).isEqualTo("  |   |  ")
        assertThat(result.board[3]).isEqualTo("--+---+--")
        assertThat(result.board[4]).isEqualTo("  |   |  ")
    }

    @Test
    fun `getGame throws GameNotFoundException when game does not exist`() {
        every { gameRepository.findById(99L) } returns Optional.empty()

        assertThrows<GameNotFoundException> {
            gameService.getGame(99L, null)
        }
    }

    // --- getGamesForPlayer ---

    @Test
    fun `getGamesForPlayer returns list of game summaries with opponent username`() {
        every { gameRepository.findAllByPlayerId(1L) } returns listOf(activeGame())
        every { playerRepository.findAllById(any<Set<Long>>()) } returns listOf(playerX, playerO)

        val result = gameService.getGamesForPlayer(1L)

        assertThat(result).hasSize(1)
        assertThat(result[0].gameId).isEqualTo(1L)
        assertThat(result[0].status).isEqualTo(GameStatus.IN_PROGRESS)
        assertThat(result[0].opponentUsername).isEqualTo("bob")
    }

    @Test
    fun `getGamesForPlayer returns empty list when player has no games`() {
        every { gameRepository.findAllByPlayerId(1L) } returns emptyList()
        every { playerRepository.findAllById(any<Set<Long>>()) } returns emptyList()

        val result = gameService.getGamesForPlayer(1L)

        assertThat(result).isEmpty()
    }

    // --- makeMove ---

    @Test
    fun `makeMove applies move and flips turn to O`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame())
        captureAndReturn()

        val result = gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 0, 0))

        assertThat(result.status).isEqualTo(GameStatus.IN_PROGRESS)
        assertThat(result.nextTurn).isEqualTo("O")
    }

    @Test
    fun `makeMove detects X win via top row`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame(board = "XX_______"))
        captureAndReturn()

        val result = gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 0, 2))

        assertThat(result.status).isEqualTo(GameStatus.FINISHED)
        assertThat(result.winner).isEqualTo("X")
    }

    @Test
    fun `makeMove detects X win via diagonal`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame(board = "XO_OX____"))
        captureAndReturn()

        val result = gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 2, 2))

        assertThat(result.status).isEqualTo(GameStatus.FINISHED)
        assertThat(result.winner).isEqualTo("X")
    }

    @Test
    fun `makeMove detects O win via middle row`() {
        every { playerService.authenticate(2L, "secret") } returns playerO
        every { gameRepository.findById(1L) } returns Optional.of(activeGame(board = "_X_OO_X_X", nextPlayerId = 2L))
        captureAndReturn()

        val result = gameService.makeMove(1L, MakeMoveRequest(2L, "secret", 1, 2))

        assertThat(result.status).isEqualTo(GameStatus.FINISHED)
        assertThat(result.winner).isEqualTo("O")
    }

    @Test
    fun `makeMove detects draw when board is full with no winner`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame(board = "XOXOOX_XO", nextPlayerId = 1L))
        captureAndReturn()

        val result = gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 2, 0))

        assertThat(result.status).isEqualTo(GameStatus.FINISHED)
        assertThat(result.winner).isNull()
    }

    @Test
    fun `makeMove shows available moves only when it is the requesting player's turn after move`() {
        every { playerService.authenticate(2L, "secret") } returns playerO
        every { gameRepository.findById(1L) } returns Optional.of(activeGame(board = "X________", nextPlayerId = 2L))
        captureAndReturn()

        val result = gameService.makeMove(1L, MakeMoveRequest(2L, "secret", 1, 1))

        assertThat(result.yourTurn).isFalse()
        assertThat(result.availableMoves).isNull()
    }

    @Test
    fun `makeMove throws InvalidMoveException when player is not part of the game`() {
        val outsider = Player(id = 99L, username = "outsider", password = "hashed")
        every { playerService.authenticate(99L, "secret") } returns outsider
        every { gameRepository.findById(1L) } returns Optional.of(activeGame())

        assertThrows<InvalidMoveException> {
            gameService.makeMove(1L, MakeMoveRequest(99L, "secret", 0, 0))
        }
    }

    @Test
    fun `makeMove throws InvalidMoveException when game is WAITING`() {
        val waitingGame = Game(id = 1L, playerXId = 1L, status = GameStatus.WAITING)
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(waitingGame)

        assertThrows<InvalidMoveException> {
            gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 0, 0))
        }
    }

    @Test
    fun `makeMove throws InvalidMoveException when game is FINISHED`() {
        val finishedGame = activeGame().copy(status = GameStatus.FINISHED, nextPlayerId = null)
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(finishedGame)

        assertThrows<InvalidMoveException> {
            gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 0, 0))
        }
    }

    @Test
    fun `makeMove throws InvalidMoveException when it is not the player's turn`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame(nextPlayerId = 2L))

        assertThrows<InvalidMoveException> {
            gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 0, 0))
        }
    }

    @Test
    fun `makeMove throws InvalidMoveException when cell is already occupied`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame(board = "X________"))

        assertThrows<InvalidMoveException> {
            gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 0, 0))
        }
    }

    @Test
    fun `makeMove throws InvalidMoveException when row is out of range`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame())

        assertThrows<InvalidMoveException> {
            gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 3, 0))
        }
    }

    @Test
    fun `makeMove throws InvalidMoveException when col is out of range`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame())

        assertThrows<InvalidMoveException> {
            gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 0, -1))
        }
    }

    @Test
    fun `makeMove throws ConcurrentUpdateException on optimistic lock failure`() {
        every { playerService.authenticate(1L, "secret") } returns playerX
        every { gameRepository.findById(1L) } returns Optional.of(activeGame())
        every { gameRepository.save(any()) } throws OptimisticLockingFailureException("conflict")

        assertThrows<ConcurrentUpdateException> {
            gameService.makeMove(1L, MakeMoveRequest(1L, "secret", 0, 0))
        }
    }
}
