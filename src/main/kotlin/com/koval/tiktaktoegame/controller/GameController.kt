package com.koval.tiktaktoegame.controller

import com.koval.tiktaktoegame.dto.request.JoinGameRequest
import com.koval.tiktaktoegame.dto.request.MakeMoveRequest
import com.koval.tiktaktoegame.dto.response.GameResponse
import com.koval.tiktaktoegame.service.GameService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/games")
class GameController(private val gameService: GameService) {

    @PostMapping("/join")
    fun joinGame(@RequestBody request: JoinGameRequest): GameResponse =
        gameService.joinGame(request)

    @GetMapping("/{gameId}")
    fun getGame(
        @PathVariable gameId: Long,
        @RequestParam(required = false) playerId: Long?
    ): GameResponse = gameService.getGame(gameId, playerId)

    @PostMapping("/{gameId}/moves")
    fun makeMove(
        @PathVariable gameId: Long,
        @RequestBody request: MakeMoveRequest
    ): GameResponse = gameService.makeMove(gameId, request)
}
