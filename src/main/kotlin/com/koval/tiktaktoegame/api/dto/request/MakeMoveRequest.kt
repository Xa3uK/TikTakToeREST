package com.koval.tiktaktoegame.api.dto.request

data class MakeMoveRequest(
    val playerId: Long,
    val password: String,
    val row: Int,
    val col: Int
)
