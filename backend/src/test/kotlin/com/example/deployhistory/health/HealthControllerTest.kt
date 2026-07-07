package com.example.deployhistory.health

import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest(
    properties = [
        "app.security.auth.password=test-password",
        "app.security.jwt.secret=test-jwt-secret-for-deploy-history-ci",
    ],
)
@AutoConfigureMockMvc
class HealthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `returns service health`() {
        mockMvc.get("/api/health")
            .andExpect {
                status { isOk() }
                jsonPath("$.success", equalTo(true))
                jsonPath("$.data.service", equalTo("deploy-history-backend"))
                jsonPath("$.data.status", equalTo("UP"))
            }
    }
}
