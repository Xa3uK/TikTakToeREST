package com.koval.tiktaktoegame.unit.service

import com.koval.tiktaktoegame.api.dto.request.RegisterRequest
import com.koval.tiktaktoegame.domain.exception.AuthenticationException
import com.koval.tiktaktoegame.domain.exception.PlayerNotFoundException
import com.koval.tiktaktoegame.domain.model.Player
import com.koval.tiktaktoegame.domain.repository.PlayerRepository
import com.koval.tiktaktoegame.domain.service.PlayerService
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.util.Optional

class PlayerServiceTest {

    private val playerRepository: PlayerRepository = mockk()
    private val playerService = PlayerService(playerRepository)
    private val encoder = BCryptPasswordEncoder()

    @Test
    fun `register returns PlayerResponse on success`() {
        every { playerRepository.existsByUsername("alice") } returns false
        every { playerRepository.save(any()) } answers { firstArg<Player>().copy(id = 1L) }

        val result = playerService.register(RegisterRequest("alice", "secret"))

        assertThat(result.playerId).isEqualTo(1L)
        assertThat(result.username).isEqualTo("alice")
    }

    @Test
    fun `register throws IllegalArgumentException when username already taken`() {
        every { playerRepository.existsByUsername("alice") } returns true

        assertThrows<IllegalArgumentException> {
            playerService.register(RegisterRequest("alice", "secret"))
        }
    }

    @Test
    fun `authenticate returns player on valid credentials`() {
        val player = Player(id = 1L, username = "alice", password = encoder.encode("secret")!!)
        every { playerRepository.findById(1L) } returns Optional.of(player)

        val result = playerService.authenticate(1L, "secret")

        assertThat(result).isEqualTo(player)
    }

    @Test
    fun `authenticate throws PlayerNotFoundException when player does not exist`() {
        every { playerRepository.findById(99L) } returns Optional.empty()

        assertThrows<PlayerNotFoundException> {
            playerService.authenticate(99L, "secret")
        }
    }

    @Test
    fun `authenticate throws AuthenticationException on wrong password`() {
        val player = Player(id = 1L, username = "alice", password = encoder.encode("correct")!!)
        every { playerRepository.findById(1L) } returns Optional.of(player)

        assertThrows<AuthenticationException> {
            playerService.authenticate(1L, "wrong")
        }
    }
}
