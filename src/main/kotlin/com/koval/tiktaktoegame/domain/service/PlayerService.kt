package com.koval.tiktaktoegame.domain.service

import com.koval.tiktaktoegame.api.dto.request.RegisterRequest
import com.koval.tiktaktoegame.api.dto.response.PlayerResponse
import com.koval.tiktaktoegame.domain.exception.AuthenticationException
import com.koval.tiktaktoegame.domain.exception.PlayerNotFoundException
import com.koval.tiktaktoegame.domain.model.Player
import com.koval.tiktaktoegame.domain.repository.PlayerRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service

@Service
class PlayerService(private val playerRepository: PlayerRepository) {

    private val logger = LoggerFactory.getLogger(PlayerService::class.java)
    private val encoder = BCryptPasswordEncoder()

    fun register(request: RegisterRequest): PlayerResponse {
        logger.info("Registering player with username '{}'", request.username)
        require(!playerRepository.existsByUsername(request.username)) {
            "Username '${request.username}' is already taken"
        }
        val player = playerRepository.save(
            Player(username = request.username, password = encoder.encode(request.password)!!)
        )
        logger.info("Player registered successfully: id={}, username='{}'", player.id, player.username)
        return PlayerResponse(playerId = player.id!!, username = player.username)
    }

    fun authenticate(playerId: Long, rawPassword: String): Player {
        logger.debug("Authenticating player id={}", playerId)
        val player = playerRepository.findById(playerId).orElseThrow { PlayerNotFoundException(playerId) }
        if (!encoder.matches(rawPassword, player.password)) {
            logger.warn("Authentication failed for player id={}", playerId)
            throw AuthenticationException()
        }
        logger.debug("Player id={} authenticated successfully", playerId)
        return player
    }
}
