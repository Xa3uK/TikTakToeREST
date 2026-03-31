package com.koval.tiktaktoegame.controller

import com.koval.tiktaktoegame.controller.api.PlayerApi
import com.koval.tiktaktoegame.dto.request.RegisterRequest
import com.koval.tiktaktoegame.dto.response.GameSummaryResponse
import com.koval.tiktaktoegame.dto.response.PlayerResponse
import com.koval.tiktaktoegame.service.GameService
import com.koval.tiktaktoegame.service.PlayerService
import org.springframework.web.bind.annotation.RestController

@RestController
class PlayerController(
    private val playerService: PlayerService,
    private val gameService: GameService
) : PlayerApi {

    override fun register(request: RegisterRequest): PlayerResponse =
        playerService.register(request)

    override fun getGames(playerId: Long): List<GameSummaryResponse> =
        gameService.getGamesForPlayer(playerId)
}
