package com.koval.tiktaktoegame.domain.exception

class PlayerNotFoundException(playerId: Long) : RuntimeException("Player $playerId not found")
