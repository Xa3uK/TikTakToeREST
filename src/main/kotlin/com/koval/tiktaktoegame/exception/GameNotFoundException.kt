package com.koval.tiktaktoegame.exception

class GameNotFoundException(gameId: Long) : RuntimeException("Game $gameId not found")
