package com.example.deployhistory.auth

import com.example.deployhistory.security.SecurityProperties
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val jwtEncoder: JwtEncoder,
    private val securityProperties: SecurityProperties,
) {
    fun login(request: LoginRequest): LoginResponse {
        val authentication = authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password),
        )

        return issueToken(authentication)
    }

    private fun issueToken(authentication: Authentication): LoginResponse {
        val now = Instant.now()
        val expiresAt = now.plusSeconds(securityProperties.jwt.accessTokenTtlSeconds)
        val scope = authentication.authorities.joinToString(" ") { it.authority.removePrefix("ROLE_") }
        val claims = JwtClaimsSet.builder()
            .issuer(securityProperties.jwt.issuer)
            .issuedAt(now)
            .expiresAt(expiresAt)
            .subject(authentication.name)
            .claim("scope", scope)
            .build()
        val headers = JwsHeader.with(MacAlgorithm.HS256).build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims)).tokenValue

        return LoginResponse(
            accessToken = token,
            expiresIn = securityProperties.jwt.accessTokenTtlSeconds,
        )
    }
}

