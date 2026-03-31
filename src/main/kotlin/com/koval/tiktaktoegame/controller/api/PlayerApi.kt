package com.koval.tiktaktoegame.controller.api

import com.koval.tiktaktoegame.dto.request.RegisterRequest
import com.koval.tiktaktoegame.dto.response.GameSummaryResponse
import com.koval.tiktaktoegame.dto.response.PlayerResponse
import com.koval.tiktaktoegame.exception.ErrorResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus

@Tag(name = "Players", description = "Player registration and game history")
@RequestMapping("/api/v1/players")
interface PlayerApi {

    @Operation(summary = "Register a new player")
    @ApiResponses(
        ApiResponse(responseCode = "201", description = "Player registered successfully"),
        ApiResponse(
            responseCode = "409", description = "Username already taken",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest): PlayerResponse

    @Operation(summary = "List all games the player is assigned to")
    @ApiResponses(
        ApiResponse(responseCode = "200", description = "List of games"),
        ApiResponse(
            responseCode = "404", description = "Player not found",
            content = [Content(schema = Schema(implementation = ErrorResponse::class))]
        )
    )
    @GetMapping("/{playerId}/games")
    fun getGames(@PathVariable playerId: Long): List<GameSummaryResponse>
}
