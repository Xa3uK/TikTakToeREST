package com.koval.tiktaktoegame

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class TikTakToeGameApplication

fun main(args: Array<String>) {
    runApplication<TikTakToeGameApplication>(*args)
}
