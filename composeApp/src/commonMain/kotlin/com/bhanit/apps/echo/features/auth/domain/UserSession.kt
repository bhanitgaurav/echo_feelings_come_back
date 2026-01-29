package com.bhanit.apps.echo.features.auth.domain

data class UserSession(
    val token: String,
    val userId: String,
    val phone: String,
    val username: String? = null
)
