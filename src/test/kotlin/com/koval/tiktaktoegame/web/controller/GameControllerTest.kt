package com.koval.tiktaktoegame.web.controller

import tools.jackson.module.kotlin.jacksonObjectMapper
import com.koval.tiktaktoegame.controller.GameController
import com.koval.tiktaktoegame.domain.GameStatus
import com.koval.tiktaktoegame.dto.request.JoinGameRequest
import com.koval.tiktaktoegame.dto.request.MakeMoveRequest
import com.koval.tiktaktoegame.dto.response.GameResponse
import com.koval.tiktaktoegame.dto.response.MoveCoordinates
import com.koval.tiktaktoegame.exception.AuthenticationException
import com.koval.tiktaktoegame.exception.ConcurrentUpdateException
import com.koval.tiktaktoegame.exception.GameNotFoundException
import com.koval.tiktaktoegame.exception.GlobalExceptionHandler
import com.koval.tiktaktoegame.exception.InvalidMoveException
import com.koval.tiktaktoegame.service.GameService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders

class GameControllerTest {

    private val gameService: GameService = mockk()
    private val objectMapper = jacksonObjectMapper()

    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(GameController(gameService))
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    private val emptyBoard = listOf("  |   |  ", "--+---+--", "  |   |  ", "--+---+--", "  |   |  ")

    private fun waitingGameResponse() = GameResponse(
        gameId = 1L,
        board = emptyBoard,
        yourSymbol = "X",
        yourTurn = false,
        nextTurn = null,
        status = GameStatus.WAITING,
        winner = null,
        availableMoves = null
    )

    private fun inProgressGameResponse(yourTurn: Boolean = true) = GameResponse(
        gameId = 1L,
        board = emptyBoard,
        yourSymbol = "X",
        yourTurn = yourTurn,
        nextTurn = "X",
        status = GameStatus.IN_PROGRESS,
        winner = null,
        availableMoves = if (yourTurn) List(9) { MoveCoordinates(it / 3, it % 3) } else null
    )

    // --- POST /api/v1/games/join ---

    @Test
    fun `POST games join returns 200 with WAITING status when no match available`() {
        val request = JoinGameRequest(playerId = 1L, password = "secret")
        every { gameService.joinGame(request) } returns waitingGameResponse()

        mockMvc.post("/api/v1/games/join") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.gameId") { value(1L) }
            jsonPath("$.status") { value("WAITING") }
            jsonPath("$.yourSymbol") { value("X") }
            jsonPath("$.yourTurn") { value(false) }
        }
    }

    @Test
    fun `POST games join returns 200 with IN_PROGRESS when match found`() {
        val request = JoinGameRequest(playerId = 2L, password = "secret")
        every { gameService.joinGame(request) } returns inProgressGameResponse(yourTurn = false).copy(yourSymbol = "O")

        mockMvc.post("/api/v1/games/join") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("IN_PROGRESS") }
            jsonPath("$.yourSymbol") { value("O") }
            jsonPath("$.yourTurn") { value(false) }
        }
    }

    @Test
    fun `POST games join returns 401 on invalid credentials`() {
        val request = JoinGameRequest(playerId = 1L, password = "wrong")
        every { gameService.joinGame(request) } throws AuthenticationException()

        mockMvc.post("/api/v1/games/join") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { isNotEmpty() }
        }
    }

    @Test
    fun `POST games join returns 409 on concurrent update conflict`() {
        val request = JoinGameRequest(playerId = 1L, password = "secret")
        every { gameService.joinGame(request) } throws ConcurrentUpdateException()

        mockMvc.post("/api/v1/games/join") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error") { isNotEmpty() }
        }
    }

    // --- GET /api/v1/games/{gameId} ---

    @Test
    fun `GET games returns 200 with spectator view when no playerId provided`() {
        every { gameService.getGame(1L, null) } returns inProgressGameResponse().copy(
            yourSymbol = null, yourTurn = null, availableMoves = null
        )

        mockMvc.get("/api/v1/games/1").andExpect {
            status { isOk() }
            jsonPath("$.gameId") { value(1L) }
            jsonPath("$.status") { value("IN_PROGRESS") }
            jsonPath("$.board") { isArray() }
        }
    }

    @Test
    fun `GET games returns 200 with player context when playerId provided`() {
        every { gameService.getGame(1L, 1L) } returns inProgressGameResponse(yourTurn = true)

        mockMvc.get("/api/v1/games/1?playerId=1").andExpect {
            status { isOk() }
            jsonPath("$.yourSymbol") { value("X") }
            jsonPath("$.yourTurn") { value(true) }
            jsonPath("$.availableMoves") { isArray() }
            jsonPath("$.availableMoves.length()") { value(9) }
        }
    }

    @Test
    fun `GET games returns 404 when game does not exist`() {
        every { gameService.getGame(99L, null) } throws GameNotFoundException(99L)

        mockMvc.get("/api/v1/games/99").andExpect {
            status { isNotFound() }
            jsonPath("$.error") { isNotEmpty() }
        }
    }

    // --- POST /api/v1/games/{gameId}/moves ---

    @Test
    fun `POST games moves returns 200 after successful move`() {
        val request = MakeMoveRequest(playerId = 1L, password = "secret", row = 0, col = 0)
        every { gameService.makeMove(1L, request) } returns inProgressGameResponse(yourTurn = false).copy(nextTurn = "O")

        mockMvc.post("/api/v1/games/1/moves") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("IN_PROGRESS") }
            jsonPath("$.nextTurn") { value("O") }
        }
    }

    @Test
    fun `POST games moves returns 200 with FINISHED status when move wins the game`() {
        val request = MakeMoveRequest(playerId = 1L, password = "secret", row = 0, col = 2)
        every { gameService.makeMove(1L, request) } returns GameResponse(
            gameId = 1L, board = emptyBoard,
            yourSymbol = "X", yourTurn = false,
            nextTurn = null, status = GameStatus.FINISHED,
            winner = "X", availableMoves = null
        )

        mockMvc.post("/api/v1/games/1/moves") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.status") { value("FINISHED") }
            jsonPath("$.winner") { value("X") }
        }
    }

    @Test
    fun `POST games moves returns 401 on invalid credentials`() {
        val request = MakeMoveRequest(playerId = 1L, password = "wrong", row = 0, col = 0)
        every { gameService.makeMove(1L, request) } throws AuthenticationException()

        mockMvc.post("/api/v1/games/1/moves") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.error") { isNotEmpty() }
        }
    }

    @Test
    fun `POST games moves returns 404 when game does not exist`() {
        val request = MakeMoveRequest(playerId = 1L, password = "secret", row = 0, col = 0)
        every { gameService.makeMove(99L, request) } throws GameNotFoundException(99L)

        mockMvc.post("/api/v1/games/99/moves") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isNotFound() }
            jsonPath("$.error") { isNotEmpty() }
        }
    }

    @Test
    fun `POST games moves returns 409 on invalid move`() {
        val request = MakeMoveRequest(playerId = 1L, password = "secret", row = 0, col = 0)
        every { gameService.makeMove(1L, request) } throws InvalidMoveException("Cell already occupied")

        mockMvc.post("/api/v1/games/1/moves") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error") { value("Cell already occupied") }
        }
    }

    @Test
    fun `POST games moves returns 409 on concurrent update conflict`() {
        val request = MakeMoveRequest(playerId = 1L, password = "secret", row = 0, col = 0)
        every { gameService.makeMove(1L, request) } throws ConcurrentUpdateException()

        mockMvc.post("/api/v1/games/1/moves") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isConflict() }
            jsonPath("$.error") { isNotEmpty() }
        }
    }
}
