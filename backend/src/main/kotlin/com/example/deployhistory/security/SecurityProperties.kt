package com.example.deployhistory.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.security")
data class SecurityProperties(
    val auth: Auth = Auth(),
    val jwt: Jwt = Jwt(),
) {
    data class Auth(
        val username: String = "admin",
        val password: String = "change-me",
    )

    data class Jwt(
        val issuer: String = "deploy-history-backend",
        val secret: String = "",
        val accessTokenTtlSeconds: Long = 3600,
    )
}
