package com.koval.tiktaktoegame.web.controller

import com.koval.tiktaktoegame.api.controller.PlayerController
import com.koval.tiktaktoegame.api.dto.request.RegisterRequest
import com.koval.tiktaktoegame.api.dto.response.GameSummaryResponse
import com.koval.tiktaktoegame.api.dto.response.PlayerResponse
import com.koval.tiktaktoegame.api.exception.GlobalExceptionHandler
import com.koval.tiktaktoegame.domain.exception.PlayerNotFoundException
import com.koval.tiktaktoegame.domain.model.GameStatus
import com.koval.tiktaktoegame.domain.service.GameService
import com.koval.tiktaktoegame.domain.service.PlayerService
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.module.kotlin.jacksonObjectMapper

class PlayerControllerTest {

    private val playerService: PlayerService = mockk()
    private val gameService: GameService = mockk()
    private val objectMapper = jacksonObjectMapper()

    private val mockMvc: MockMvc = MockMvcBuilders
        .standaloneSetup(PlayerController(playerService, gameService))
        .setControllerAdvice(GlobalExceptionHandler())
        .build()

    @Test
    fun `POST players returns 201 with PlayerResponse on success`() {
        val request = RegisterRequest("alice", "secret")
        every { playerService.register(request) } returns PlayerResponse(playerId = 1L, username = "alice")

        mockMvc.post("/api/v1/players") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.playerId") { value(1L) }
            jsonPath("$.username") { value("alice") }
        }
    }

    @Test
    fun `POST players returns 400 when username is already taken`() {
        val request = RegisterRequest("alice", "secret")
        every { playerService.register(request) } throws IllegalArgumentException("Username already taken")

        mockMvc.post("/api/v1/players") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.error") { value("Username already taken") }
        }
    }

    @Test
    fun `GET players games returns 200 with list of game summaries`() {
        val summaries = listOf(
            GameSummaryResponse(gameId = 1L, status = GameStatus.IN_PROGRESS, opponentUsername = "bob")
        )
        every { gameService.getGamesForPlayer(1L) } returns summaries

        mockMvc.get("/api/v1/players/1/games").andExpect {
            status { isOk() }
            jsonPath("$[0].gameId") { value(1L) }
            jsonPath("$[0].status") { value("IN_PROGRESS") }
            jsonPath("$[0].opponentUsername") { value("bob") }
        }
    }

    @Test
    fun `GET players games returns 200 with empty list when player has no games`() {
        every { gameService.getGamesForPlayer(1L) } returns emptyList()

        mockMvc.get("/api/v1/players/1/games").andExpect {
            status { isOk() }
            jsonPath("$") { isEmpty() }
        }
    }

    @Test
    fun `GET players games returns 404 when player does not exist`() {
        every { gameService.getGamesForPlayer(99L) } throws PlayerNotFoundException(99L)

        mockMvc.get("/api/v1/players/99/games").andExpect {
            status { isNotFound() }
            jsonPath("$.error") { isNotEmpty() }
        }
    }
}
