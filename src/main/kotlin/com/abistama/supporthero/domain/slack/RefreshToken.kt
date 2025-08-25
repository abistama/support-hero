package com.abistama.supporthero.domain.slack

data class RefreshToken(val clientId: String, val clientSecret: String, val refreshToken: String)
