package com.koval.tiktaktoegame.api.controller

import com.koval.tiktaktoegame.api.controller.spec.GameApi
import com.koval.tiktaktoegame.api.dto.request.JoinGameRequest
import com.koval.tiktaktoegame.api.dto.request.MakeMoveRequest
import com.koval.tiktaktoegame.api.dto.response.GameResponse
import com.koval.tiktaktoegame.domain.service.GameService
import org.springframework.web.bind.annotation.RestController

@RestController
class GameController(private val gameService: GameService) : GameApi {

    override fun joinGame(request: JoinGameRequest): GameResponse =
        gameService.joinGame(request)

    override fun getGame(gameId: Long, playerId: Long?): GameResponse =
        gameService.getGame(gameId, playerId)

    override fun makeMove(gameId: Long, request: MakeMoveRequest): GameResponse =
        gameService.makeMove(gameId, request)
}
