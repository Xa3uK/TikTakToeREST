package com.koval.tiktaktoegame.domain.model

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime

@Table("players")
data class Player(
    @Id val id: Long? = null,
    val username: String,
    val password: String,
    val createdAt: OffsetDateTime = OffsetDateTime.now()
)
