package com.bhanit.apps.echo.data.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.util.Date
import java.util.UUID

class JwtService(
    private val issuer: String,
    private val audience: String,
    private val secret: String
) {
    private val algorithm = Algorithm.HMAC256(secret)

    // Validity: 365 days (long lived for demo)
    private val validityInMs = 365L * 24 * 60 * 60 * 1000

    fun generateToken(userId: UUID, tokenVersion: Int): String {
        return JWT.create()
            .withSubject("Authentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("id", userId.toString())
            .withClaim("token_version", tokenVersion)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }

    fun generateAdminToken(adminId: UUID, role: String): String {
        return JWT.create()
            .withSubject("AdminAuthentication")
            .withIssuer(issuer)
            .withAudience(audience)
            .withClaim("id", adminId.toString())
            .withClaim("role", role)
            .withExpiresAt(Date(System.currentTimeMillis() + validityInMs))
            .sign(algorithm)
    }

    fun getVerifier() = JWT.require(algorithm)
        .withAudience(audience)
        .withIssuer(issuer)
        .build()
}
