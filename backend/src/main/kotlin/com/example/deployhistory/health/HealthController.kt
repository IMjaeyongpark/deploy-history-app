package com.example.deployhistory.health

import com.example.deployhistory.common.api.ApiResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
@RequestMapping("/api/health")
class HealthController {
    @GetMapping
    fun health(): ApiResponse<HealthStatus> =
        ApiResponse.success(
            HealthStatus(
                service = "deploy-history-backend",
                status = "UP",
                timestamp = Instant.now(),
            ),
        )
}

data class HealthStatus(
    val service: String,
    val status: String,
    val timestamp: Instant,
)
