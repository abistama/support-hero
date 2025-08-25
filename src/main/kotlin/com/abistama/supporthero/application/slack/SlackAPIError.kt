package com.abistama.supporthero.application.slack

data class SlackAPIError(
    val message: String,
)

fun Throwable.toSlackAPIError(): SlackAPIError = SlackAPIError(this.message ?: "Unknown error")
