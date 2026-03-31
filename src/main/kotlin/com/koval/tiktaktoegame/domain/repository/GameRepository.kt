package com.koval.tiktaktoegame.domain.repository

import com.koval.tiktaktoegame.domain.model.Game
import org.springframework.data.jdbc.repository.query.Query
import org.springframework.data.repository.CrudRepository

interface GameRepository : CrudRepository<Game, Long> {

    @Query("SELECT * FROM games WHERE status = 'WAITING' AND player_x_id != :playerId LIMIT 1")
    fun findFirstWaitingGameNotOwnedBy(playerId: Long): Game?

    @Query("SELECT * FROM games WHERE player_x_id = :playerId OR player_o_id = :playerId")
    fun findAllByPlayerId(playerId: Long): List<Game>
}
