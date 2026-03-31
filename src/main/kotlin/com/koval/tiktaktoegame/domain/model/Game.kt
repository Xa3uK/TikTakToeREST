package com.koval.tiktaktoegame.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Version
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

enum class GameStatus { WAITING, IN_PROGRESS, FINISHED }

@Table("games")
data class Game(
    @Id val id: Long? = null,
    val board: String = "_________",
    val status: GameStatus = GameStatus.WAITING,
    val playerXId: Long,
    val playerOId: Long? = null,
    val nextPlayerId: Long? = null,
    val winnerId: Long? = null,
    @Version val version: Int = 0,
    val createdAt: OffsetDateTime = OffsetDateTime.now(),
    val updatedAt: OffsetDateTime = OffsetDateTime.now()
)
