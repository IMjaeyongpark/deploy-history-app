package com.example.deployhistory.auth

import jakarta.validation.constraints.NotBlank

data class LoginRequest(
    @field:NotBlank
    val username: String = "",
    @field:NotBlank
    val password: String = "",
)

data class LoginResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)

data class CurrentUserResponse(
    val username: String,
    val authorities: List<String>,
)

