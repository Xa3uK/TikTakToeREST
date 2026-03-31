package com.koval.tiktaktoegame.controller

import com.koval.tiktaktoegame.dto.request.RegisterRequest
import com.koval.tiktaktoegame.dto.response.GameSummaryResponse
import com.koval.tiktaktoegame.dto.response.PlayerResponse
import com.koval.tiktaktoegame.service.GameService
import com.koval.tiktaktoegame.service.PlayerService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/players")
class PlayerController(
    private val playerService: PlayerService,
    private val gameService: GameService
) {

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@RequestBody request: RegisterRequest): PlayerResponse =
        playerService.register(request)

    @GetMapping("/{playerId}/games")
    fun getGames(@PathVariable playerId: Long): List<GameSummaryResponse> =
        gameService.getGamesForPlayer(playerId)
}
