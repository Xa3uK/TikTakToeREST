package com.koval.tiktaktoegame.api.dto.request

data class JoinGameRequest(
    val playerId: Long,
    val password: String
)
