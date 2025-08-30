package io.github.abdulroufsidhu.orgolink_auth

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.LoginOrCreateUserRequestDTO
import org.junit.jupiter.api.Assertions.assertEquals
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

object Utils {

    private var port: Int = 8080
    private fun getBaseUrl() = "http://localhost:$port"

    fun createUser(restTemplate: TestRestTemplate, onSuccess: (String?) -> Unit){
        val requestDto = LoginOrCreateUserRequestDTO(
            username = "testuser",
            password = "Password123!"
        )
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(requestDto, headers)

        val response: ResponseEntity<ValidResponseData<String>> =
            restTemplate.exchange(
                getBaseUrl() + "/auth/register",
                HttpMethod.POST,
                HttpEntity(request),
                object : ParameterizedTypeReference<ValidResponseData<String>>() {}
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        onSuccess(response.body?.message)

    }
}