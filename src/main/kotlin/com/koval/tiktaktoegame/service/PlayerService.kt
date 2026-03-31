package com.koval.tiktaktoegame.service

import com.koval.tiktaktoegame.domain.Player
import com.koval.tiktaktoegame.dto.request.RegisterRequest
import com.koval.tiktaktoegame.dto.response.PlayerResponse
import com.koval.tiktaktoegame.exception.AuthenticationException
import com.koval.tiktaktoegame.exception.PlayerNotFoundException
import com.koval.tiktaktoegame.repository.PlayerRepository
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class PlayerService(private val playerRepository: PlayerRepository) {

    private val encoder = BCryptPasswordEncoder()

    fun register(request: RegisterRequest): PlayerResponse {
        require(!playerRepository.existsByUsername(request.username)) {
            "Username '${request.username}' is already taken"
        }
        val player = playerRepository.save(
            Player(username = request.username, password = encoder.encode(request.password)!!)
        )
        return PlayerResponse(playerId = player.id!!, username = player.username)
    }

    fun authenticate(playerId: Long, rawPassword: String): Player {
        val player = playerRepository.findById(playerId).orElseThrow { PlayerNotFoundException(playerId) }
        if (!encoder.matches(rawPassword, player.password)) throw AuthenticationException()
        return player
    }
}
