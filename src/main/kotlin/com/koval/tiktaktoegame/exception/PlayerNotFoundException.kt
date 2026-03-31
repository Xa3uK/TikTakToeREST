package com.koval.tiktaktoegame.exception

class PlayerNotFoundException(playerId: Long) : RuntimeException("Player $playerId not found")
