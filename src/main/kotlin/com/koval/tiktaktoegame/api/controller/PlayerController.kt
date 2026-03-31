package com.koval.tiktaktoegame.api.controller

import com.koval.tiktaktoegame.api.controller.spec.PlayerApi
import com.koval.tiktaktoegame.api.dto.request.RegisterRequest
import com.koval.tiktaktoegame.api.dto.response.GameSummaryResponse
import com.koval.tiktaktoegame.api.dto.response.PlayerResponse
import com.koval.tiktaktoegame.domain.service.GameService
import com.koval.tiktaktoegame.domain.service.PlayerService
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
