package com.koval.tiktaktoegame.domain.exception

class GameNotFoundException(gameId: Long) : RuntimeException("Game $gameId not found")
