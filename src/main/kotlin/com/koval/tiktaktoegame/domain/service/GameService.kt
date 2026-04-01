package com.koval.tiktaktoegame.domain.service

import com.koval.tiktaktoegame.api.dto.request.JoinGameRequest
import com.koval.tiktaktoegame.api.dto.request.MakeMoveRequest
import com.koval.tiktaktoegame.api.dto.response.GameResponse
import com.koval.tiktaktoegame.api.dto.response.GameSummaryResponse
import com.koval.tiktaktoegame.api.dto.response.MoveCoordinates
import com.koval.tiktaktoegame.domain.exception.ConcurrentUpdateException
import com.koval.tiktaktoegame.domain.exception.GameNotFoundException
import com.koval.tiktaktoegame.domain.exception.InvalidMoveException
import com.koval.tiktaktoegame.domain.model.Game
import com.koval.tiktaktoegame.domain.model.GameStatus
import com.koval.tiktaktoegame.domain.repository.GameRepository
import com.koval.tiktaktoegame.domain.repository.PlayerRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GameService(
    private val gameRepository: GameRepository,
    private val playerRepository: PlayerRepository,
    private val playerService: PlayerService
) {

    private val logger = LoggerFactory.getLogger(GameService::class.java)

    @Transactional
    fun joinGame(request: JoinGameRequest): GameResponse {
        logger.info("Player id={} attempting to join a game", request.playerId)
        val player = playerService.authenticate(request.playerId, request.password)
        val playerId = requireNotNull(player.id) { "Player id must not be null after authentication" }

        val existingGame = gameRepository.findFirstWaitingGameNotOwnedBy(playerId)
        val game = try {
            if (existingGame != null) {
                logger.info("Player id={} joining existing game id={}", playerId, existingGame.id)
                gameRepository.save(
                    existingGame.copy(
                        playerOId = playerId,
                        nextPlayerId = existingGame.playerXId,
                        status = GameStatus.IN_PROGRESS
                    )
                )
            } else {
                logger.info("No waiting game found; creating new game for player id={}", playerId)
                gameRepository.save(Game(playerXId = playerId))
            }
        } catch (ex: OptimisticLockingFailureException) {
            logger.warn("Concurrent update conflict for player id={} during joinGame", playerId)
            throw ConcurrentUpdateException()
        }

        logger.info("Player id={} joined game id={} with status={}", playerId, game.id, game.status)
        return toGameResponse(game, requestingPlayerId = playerId)
    }

    fun getGame(gameId: Long, requestingPlayerId: Long?): GameResponse {
        logger.debug("Fetching game id={} for player id={}", gameId, requestingPlayerId)
        val game = gameRepository.findById(gameId).orElseThrow { GameNotFoundException(gameId) }
        return toGameResponse(game, requestingPlayerId)
    }

    fun getGamesForPlayer(playerId: Long): List<GameSummaryResponse> {
        logger.debug("Fetching all games for player id={}", playerId)
        val games = gameRepository.findAllByPlayerId(playerId)
        val playerIds = games.flatMap { listOfNotNull(it.playerXId, it.playerOId) }.toSet()
        val players = playerRepository.findAllById(playerIds).associateBy { it.id }

        return games.map { game ->
            val opponentId = if (game.playerXId == playerId) game.playerOId else game.playerXId
            GameSummaryResponse(
                gameId = requireNotNull(game.id) { "Game id must not be null" },
                status = game.status,
                opponentUsername = opponentId?.let { players[it]?.username }
            )
        }
    }

    @Transactional
    fun makeMove(gameId: Long, request: MakeMoveRequest): GameResponse {
        logger.info("Player id={} making move at ({},{}) in game id={}", request.playerId, request.row, request.col, gameId)
        val player = playerService.authenticate(request.playerId, request.password)
        val playerId = requireNotNull(player.id) { "Player id must not be null after authentication" }

        if (request.row !in 0..2 || request.col !in 0..2) {
            logger.warn("Invalid move coordinates ({},{}) from player id={}", request.row, request.col, playerId)
            throw InvalidMoveException("Row and col must be between 0 and 2")
        }

        val game = gameRepository.findById(gameId).orElseThrow { GameNotFoundException(gameId) }

        if (playerId != game.playerXId && playerId != game.playerOId) {
            logger.warn("Player id={} is not part of game id={}", playerId, gameId)
            throw InvalidMoveException("Player is not part of this game")
        }
        if (game.status != GameStatus.IN_PROGRESS) {
            logger.warn("Move rejected: game id={} status is {}", gameId, game.status)
            throw InvalidMoveException("Game is not in progress")
        }
        if (game.nextPlayerId != playerId) {
            logger.warn("Move rejected: it is not player id={}'s turn in game id={}", playerId, gameId)
            throw InvalidMoveException("It is not your turn")
        }

        val index = request.row * 3 + request.col
        if (game.board[index] != '_') {
            logger.warn("Move rejected: cell ({},{}) already occupied in game id={}", request.row, request.col, gameId)
            throw InvalidMoveException("Cell (${request.row}, ${request.col}) is already occupied")
        }

        val symbol = when (playerId) {
            game.playerXId -> 'X'
            game.playerOId -> 'O'
            else -> throw InvalidMoveException("Player is not part of this game")
        }
        val newBoard = game.board.replaceCharAt(index, symbol)

        val (newStatus, winnerId) = when {
            checkWinner(newBoard, symbol) -> GameStatus.FINISHED to playerId
            newBoard.none { it == '_' }  -> GameStatus.FINISHED to null
            else                         -> GameStatus.IN_PROGRESS to null
        }

        val newNextPlayerId = if (newStatus == GameStatus.IN_PROGRESS) {
            if (game.playerXId == playerId) game.playerOId else game.playerXId
        } else null

        val updatedGame = game.copy(
            board = newBoard,
            status = newStatus,
            nextPlayerId = newNextPlayerId,
            winnerId = winnerId
        )

        val savedGame = try {
            gameRepository.save(updatedGame)
        } catch (ex: OptimisticLockingFailureException) {
            logger.warn("Concurrent update conflict for player id={} in game id={}", playerId, gameId)
            throw ConcurrentUpdateException()
        }

        logger.info("Move saved: game id={} status={} winnerId={}", savedGame.id, savedGame.status, savedGame.winnerId)
        return toGameResponse(savedGame, requestingPlayerId = playerId)
    }

    private fun toGameResponse(game: Game, requestingPlayerId: Long?): GameResponse {
        val gameId = requireNotNull(game.id) { "Game id must not be null" }

        val yourSymbol = game.symbolOfPlayer(requestingPlayerId)
        val isYourTurn = requestingPlayerId?.let { it == game.nextPlayerId && yourSymbol != null }
        val nextTurnSymbol = game.symbolOfPlayer(game.nextPlayerId)
        val winnerSymbol = game.symbolOfPlayer(game.winnerId)

        val availableMoves =
            if (isYourTurn == true && game.status == GameStatus.IN_PROGRESS) {
                game.board.availableMoves()
            } else {
                null
            }

        return GameResponse(
            gameId = gameId,
            board = renderBoard(game.board),
            yourSymbol = yourSymbol,
            yourTurn = isYourTurn,
            nextTurn = nextTurnSymbol,
            status = game.status,
            winner = winnerSymbol,
            availableMoves = availableMoves
        )
    }

    private fun Game.symbolOfPlayer(playerId: Long?): String? =
        when (playerId) {
            playerXId -> "X"
            playerOId -> "O"
            else -> null
        }

    private fun String.availableMoves(): List<MoveCoordinates> =
        mapIndexedNotNull { i, c ->
            if (c == '_') MoveCoordinates(row = i / 3, col = i % 3) else null
        }

    private fun renderBoard(board: String): List<String> {
        fun cell(c: Char) = if (c == '_') " " else c.toString()
        fun row(start: Int) = "${cell(board[start])} | ${cell(board[start + 1])} | ${cell(board[start + 2])}"
        val sep = "--+---+--"
        return listOf(row(0), sep, row(3), sep, row(6))
    }

    private fun String.replaceCharAt(index: Int, ch: Char) =
        StringBuilder(this).also { it.setCharAt(index, ch) }.toString()

    private fun checkWinner(board: String, symbol: Char): Boolean {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        return lines.any { (a, b, c) -> board[a] == symbol && board[b] == symbol && board[c] == symbol }
    }
}
