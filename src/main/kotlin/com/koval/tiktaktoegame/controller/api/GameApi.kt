package com.koval.tiktaktoegame.controller.api

import com.koval.tiktaktoegame.dto.request.JoinGameRequest
import com.koval.tiktaktoegame.dto.request.MakeMoveRequest
import com.koval.tiktaktoegame.dto.response.GameResponse
import com.koval.tiktaktoegame.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Games", description = "Matchmaking and gameplay")
@RequestMapping("/api/v1/games")
interface GameApi {

    @Operation(summary = "Join matchmaking — joins an existing waiting game or creates a new one")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Joined or created a game"),
        ApiResponse(
            responseCode = "401", description = "Invalid credentials",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "Concurrent join conflict — retry",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/join")
    fun joinGame(@RequestBody request: JoinGameRequest): GameResponse

    @Operation(summary = "Get current game state")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Game state returned"),
        ApiResponse(
            responseCode = "404", description = "Game not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{gameId}")
    fun getGame(
        @PathVariable gameId: Long,
        @Parameter(description = "Provide your player ID to get yourSymbol, yourTurn and availableMoves")
        @RequestParam(required = false) playerId: Long?
    ): GameResponse

    @Operation(summary = "Make a move")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "Move applied, updated game state returned"),
        ApiResponse(
            responseCode = "401", description = "Invalid credentials",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "404", description = "Game not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        ),
        ApiResponse(
            responseCode = "409", description = "Invalid move or concurrent update conflict",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping("/{gameId}/moves")
    fun makeMove(
        @PathVariable gameId: Long,
        @RequestBody request: MakeMoveRequest
    ): GameResponse
}
