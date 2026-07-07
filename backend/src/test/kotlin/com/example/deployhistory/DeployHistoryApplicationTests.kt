package com.example.deployhistory

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "app.security.auth.password=test-password",
        "app.security.jwt.secret=test-jwt-secret-for-deploy-history-ci",
    ],
)
class DeployHistoryApplicationTests {
    @Test
    fun contextLoads() {
    }
}
