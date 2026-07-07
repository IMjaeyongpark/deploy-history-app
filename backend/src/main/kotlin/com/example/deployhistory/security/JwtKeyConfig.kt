package com.example.deployhistory.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration
class JwtKeyConfig {
    @Bean
    fun jwtSecretKey(securityProperties: SecurityProperties): SecretKey {
        val configuredSecret = securityProperties.jwt.secret.trim()
        val keyBytes = if (configuredSecret.isBlank()) {
            ByteArray(HMAC_SHA256_KEY_SIZE_BYTES).also { SecureRandom().nextBytes(it) }
        } else {
            MessageDigest
                .getInstance("SHA-256")
                .digest(configuredSecret.toByteArray(Charsets.UTF_8))
        }

        return SecretKeySpec(keyBytes, HMAC_SHA256_ALGORITHM)
    }

    private companion object {
        const val HMAC_SHA256_KEY_SIZE_BYTES = 32
        const val HMAC_SHA256_ALGORITHM = "HmacSHA256"
    }
}

