package com.koval.tiktaktoegame.dto.response

import com.koval.tiktaktoegame.domain.GameStatus

data class GameResponse(
    val gameId: Long,
    val board: List<String>,
    val yourSymbol: String?,
    val yourTurn: Boolean?,
    val nextTurn: String?,
    val status: GameStatus,
    val winner: String?,
    val availableMoves: List<MoveCoordinates>?
)

data class MoveCoordinates(val row: Int, val col: Int)
