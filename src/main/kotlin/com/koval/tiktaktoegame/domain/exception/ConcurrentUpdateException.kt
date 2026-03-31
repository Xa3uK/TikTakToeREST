package com.koval.tiktaktoegame.domain.exception

class ConcurrentUpdateException : RuntimeException("Operation was rejected due to a concurrent update. Please retry.")
