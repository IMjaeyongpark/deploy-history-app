package com.example.deployhistory.auth

import com.example.deployhistory.common.api.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/auth/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> =
        try {
            ResponseEntity.ok(ApiResponse.success(authService.login(request)))
        } catch (error: AuthenticationException) {
            ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse(success = false, error = "Invalid username or password"))
        }

    @GetMapping("/me")
    fun me(authentication: Authentication): ApiResponse<CurrentUserResponse> =
        ApiResponse.success(
            CurrentUserResponse(
                username = authentication.name,
                authorities = authentication.authorities.map { it.authority },
            ),
        )
}

