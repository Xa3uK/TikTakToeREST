package com.koval.tiktaktoegame.api.dto.response

import com.koval.tiktaktoegame.domain.model.GameStatus

data class GameSummaryResponse(
    val gameId: Long,
    val status: GameStatus,
    val opponentUsername: String?
)
