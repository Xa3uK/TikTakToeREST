package com.koval.tiktaktoegame.dto.response

import com.koval.tiktaktoegame.domain.GameStatus

data class GameSummaryResponse(
    val gameId: Long,
    val status: GameStatus,
    val opponentUsername: String?
)
