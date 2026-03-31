package com.koval.tiktaktoegame.api.exception

import com.koval.tiktaktoegame.domain.exception.AuthenticationException
import com.koval.tiktaktoegame.domain.exception.ConcurrentUpdateException
import com.koval.tiktaktoegame.domain.exception.GameNotFoundException
import com.koval.tiktaktoegame.domain.exception.InvalidMoveException
import com.koval.tiktaktoegame.domain.exception.PlayerNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(PlayerNotFoundException::class, GameNotFoundException::class)
    fun handleNotFound(ex: RuntimeException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse(ex.message!!))

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuth(ex: AuthenticationException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ErrorResponse(ex.message!!))

    @ExceptionHandler(InvalidMoveException::class)
    fun handleInvalidMove(ex: InvalidMoveException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message!!))

    @ExceptionHandler(ConcurrentUpdateException::class)
    fun handleConcurrent(ex: ConcurrentUpdateException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse(ex.message!!))

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ErrorResponse(ex.message!!))
}

data class ErrorResponse(val error: String)
