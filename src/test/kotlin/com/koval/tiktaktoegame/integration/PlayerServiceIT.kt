package com.koval.tiktaktoegame.integration

import com.koval.tiktaktoegame.api.dto.request.RegisterRequest
import com.koval.tiktaktoegame.domain.exception.AuthenticationException
import com.koval.tiktaktoegame.domain.exception.PlayerNotFoundException
import com.koval.tiktaktoegame.domain.service.PlayerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired

class PlayerServiceIT : AbstractIT() {

    @Autowired
    private lateinit var playerService: PlayerService

    @Test
    fun `register saves player and returns response with generated ID`() {
        val response = playerService.register(RegisterRequest("alice", "secret"))

        assertThat(response.playerId).isNotNull()
        assertThat(response.username).isEqualTo("alice")
    }

    @Test
    fun `register throws IllegalArgumentException when username is already taken`() {
        playerService.register(RegisterRequest("alice", "secret"))

        assertThrows<IllegalArgumentException> {
            playerService.register(RegisterRequest("alice", "other"))
        }
    }

    @Test
    fun `authenticate returns player when credentials are correct`() {
        val response = playerService.register(RegisterRequest("alice", "secret"))

        val player = playerService.authenticate(response.playerId, "secret")

        assertThat(player.username).isEqualTo("alice")
    }

    @Test
    fun `authenticate throws AuthenticationException on wrong password`() {
        val response = playerService.register(RegisterRequest("alice", "secret"))

        assertThrows<AuthenticationException> {
            playerService.authenticate(response.playerId, "wrong")
        }
    }

    @Test
    fun `authenticate throws PlayerNotFoundException for unknown player`() {
        assertThrows<PlayerNotFoundException> {
            playerService.authenticate(Long.MAX_VALUE, "secret")
        }
    }
}
