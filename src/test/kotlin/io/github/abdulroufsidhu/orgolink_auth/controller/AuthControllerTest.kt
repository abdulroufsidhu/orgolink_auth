package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.LoginOrCreateUserRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.services.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.*
import org.springframework.test.context.ActiveProfiles
import org.junit.jupiter.api.Assertions.*
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.core.ParameterizedTypeReference
import org.springframework.test.context.TestPropertySource

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = [
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
])
class AuthControllerTest {

//    @LocalServerPort
    private var port: Int = 8080

    @Autowired
    private lateinit var restTemplate: TestRestTemplate

    private fun getBaseUrl() = "http://localhost:$port"

    // Test for securedThankYou endpoint
    @Test
    fun `securedThankYou should return session id`() {
        val url = getBaseUrl() + "/";
        println(url)
        val response = restTemplate.getForEntity(url, String::class.java)
        
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
    }

    fun loginUser() : ValidResponseData<*>? {
        val requestDto = LoginOrCreateUserRequestDTO(
            username = "testuser",
            password = "Password123!"
        )
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(requestDto, headers)

        val response: ResponseEntity<ValidResponseData<*>> =
            restTemplate.exchange(
                getBaseUrl() + "/auth/login",
                HttpMethod.POST,
                HttpEntity(request),
                object : ParameterizedTypeReference<ValidResponseData<*>>() {}
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        return response.body
    }

    fun verify() {
        loginUser()?.data?.let {

            val headers = HttpHeaders()
            headers.contentType = MediaType.APPLICATION_JSON
            headers["Authentication"] = "Bearer $it"
            val request = HttpEntity(headers.toMap())

            val response: ResponseEntity<ValidResponseData<Nothing>> =
                restTemplate.exchange(
                    getBaseUrl() + "/auth/verify",
                    HttpMethod.GET,
                    HttpEntity(request),
                    object : ParameterizedTypeReference<ValidResponseData<Nothing>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
        }

    }




    // Test for register endpoint - Success case
    @Test
    fun `register`() {
        createUser()
    }

    // Test for login endpoint - Success case
    @Test
    fun `login`() {
        loginUser()
    }

    // Test for login endpoint - Invalid credentials
    @Test
    fun `verify logged in user`() {
        verify()
    }

}
