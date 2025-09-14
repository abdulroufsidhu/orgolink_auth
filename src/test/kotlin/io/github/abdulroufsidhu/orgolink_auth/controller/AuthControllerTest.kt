package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.TestUtils
import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.core.ParameterizedTypeReference
import org.springframework.core.annotation.Order
import org.springframework.test.context.TestPropertySource

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.AUTO_CONFIGURED)
@TestPropertySource(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
    ]
)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class AuthControllerTest(
) {
    @LocalServerPort
    private var port: Int = 0

    private lateinit var restTemplate: TestRestTemplate

    private lateinit var token: String

    @BeforeEach
    fun setUp() {
        // Reconfigure restTemplate with the right port
        this.restTemplate =
            TestRestTemplate(RestTemplateBuilder().rootUri("http://localhost:$port"))

    }

    fun loginUser() {
        TestUtils.authenticatedTest(restTemplate) {
        }
    }

    fun verify() {
        TestUtils.authenticatedTest(restTemplate) { token ->

            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(null, headers)

            val response: ResponseEntity<ValidResponseData<Nothing>> =
                restTemplate.exchange(
                    "/auth/verify",
                    HttpMethod.GET,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<Nothing>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
        }

    }

    // Test for register endpoint - Success case
    @Test
    @Order(1)
    fun `register`() {
        TestUtils.authenticatedTest(restTemplate) {}
    }


    // Test for login endpoint - Success case
    @Test
    @Order(2)
    fun `login`() {
        loginUser()
    }

    // Test for login endpoint - Invalid credentials
    @Test
    @Order(3)
    fun `verify logged in user`() {
        verify()
    }

    @Test
    @Order(4)
    fun `securedThankYou should return session id`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(null, headers)

            val response: ResponseEntity<String> =
                restTemplate.exchange(
                    "/",
                    HttpMethod.GET,
                    request,
                    String::class.java
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body)
        }
    }

}
