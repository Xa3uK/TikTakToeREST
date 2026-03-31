package com.koval.tiktaktoegame.repository

import com.koval.tiktaktoegame.domain.Player
import org.springframework.data.repository.CrudRepository

interface PlayerRepository : CrudRepository<Player, Long> {
    fun existsByUsername(username: String): Boolean
}
