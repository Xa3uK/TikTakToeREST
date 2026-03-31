package com.koval.tiktaktoegame.domain.repository

import com.koval.tiktaktoegame.domain.model.Player
import org.springframework.data.repository.CrudRepository

interface PlayerRepository : CrudRepository<Player, Long> {
    fun existsByUsername(username: String): Boolean
}
