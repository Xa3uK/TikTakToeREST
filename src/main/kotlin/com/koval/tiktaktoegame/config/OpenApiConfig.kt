package com.koval.tiktaktoegame.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class OpenApiConfig {

    @Bean
    fun openApi(): OpenAPI = OpenAPI()
        .info(
            Info()
                .title("Tic Tac Toe API")
                .version("v1")
                .description("REST API for playing Tic Tac Toe with real players")
        )
}
