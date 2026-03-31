package com.koval.tiktaktoegame.exception

class ConcurrentUpdateException : RuntimeException("Operation was rejected due to a concurrent update. Please retry.")
