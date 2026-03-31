package com.koval.tiktaktoegame.controller

import com.koval.tiktaktoegame.controller.api.GameApi
import com.koval.tiktaktoegame.dto.request.JoinGameRequest
import com.koval.tiktaktoegame.dto.request.MakeMoveRequest
import com.koval.tiktaktoegame.dto.response.GameResponse
import com.koval.tiktaktoegame.service.GameService
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
