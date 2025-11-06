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

object TestUtils {

    val  userRequest: LoginOrCreateUserRequestDTO = LoginOrCreateUserRequestDTO(
        username = "testuser",
        password = "Password123!"
    );

    fun getAuthHeader(token: String?): HttpHeaders = HttpHeaders().apply {
        contentType = MediaType.APPLICATION_JSON
        this["Authorization"] = "Bearer $token"
    }

    fun createUser(restTemplate: TestRestTemplate, user: LoginOrCreateUserRequestDTO = userRequest) : ValidResponseData<String>? {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        val request = HttpEntity(user, headers)

        val response: ResponseEntity<ValidResponseData<*>> =
            restTemplate.exchange(
                 "/api/auth/register",
                HttpMethod.POST,
                request,
                object : ParameterizedTypeReference<ValidResponseData<*>>() {}
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        return ValidResponseData(data = response.body?.data.toString(), message = response.body?.message)
    }

    fun deleteUser(restTemplate: TestRestTemplate,  token: String): ValidResponseData<*>? {

        val headers = getAuthHeader(token)
        val request = HttpEntity(null, headers)

        val response: ResponseEntity<ValidResponseData<*>> =
            restTemplate.exchange(
                "/api/auth/delete",
                HttpMethod.DELETE,
                request,
                object : ParameterizedTypeReference<ValidResponseData<*>>() {}
            )

        assertEquals(HttpStatus.OK, response.statusCode)
        return response.body
    }

    fun authenticatedTest(restTemplate: TestRestTemplate, onSuccess: (token: String?) -> Unit){
        val response = createUser(restTemplate)
        onSuccess(response?.data)
        deleteUser(restTemplate, response?.data ?: "")
    }
}