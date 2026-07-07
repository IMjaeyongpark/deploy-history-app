package com.example.deployhistory.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.hamcrest.Matchers.blankOrNullString
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(
    properties = [
        "app.security.auth.password=test-password",
        "app.security.jwt.secret=test-jwt-secret-for-deploy-history-ci",
    ],
)
@AutoConfigureMockMvc
class AuthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `login returns hmac sha256 jwt access token`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("admin", "test-password"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.success", equalTo(true))
            jsonPath("$.data.tokenType", equalTo("Bearer"))
            jsonPath("$.data.accessToken", not(blankOrNullString()))
            jsonPath("$.data.accessToken", org.hamcrest.Matchers.startsWith("eyJhbGciOiJIUzI1NiJ9"))
            jsonPath("$.data.expiresIn", equalTo(3600))
        }
    }

    @Test
    fun `login rejects invalid password`() {
        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("admin", "wrong-password"))
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.success", equalTo(false))
            jsonPath("$.error", equalTo("Invalid username or password"))
        }
    }

    @Test
    fun `protected endpoint rejects missing bearer token`() {
        mockMvc.get("/api/me")
            .andExpect {
                status { isUnauthorized() }
            }
    }

    @Test
    fun `protected endpoint accepts valid bearer token`() {
        val token = loginAndExtractAccessToken()

        mockMvc.get("/api/me") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.success", equalTo(true))
            jsonPath("$.data.username", equalTo("admin"))
        }
    }

    private fun loginAndExtractAccessToken(): String {
        val result = mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(LoginRequest("admin", "test-password"))
        }.andReturn()

        val responseBody = result.response.contentAsString
        val responseJson: JsonNode = objectMapper.readTree(responseBody)

        return responseJson.get("data").get("accessToken").asText()
    }
}
