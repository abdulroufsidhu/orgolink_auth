package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.TestUtils
import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.CreateProjectRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.GenerateProjectTokenRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectTokenResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.model.ProjectRole
import org.junit.jupiter.api.Test
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
import java.util.UUID

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.AUTO_CONFIGURED)
@TestPropertySource(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
    ]
)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProjectTokenControllerTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setUp() {
        this.restTemplate =
            TestRestTemplate(RestTemplateBuilder().rootUri("http://localhost:$port"))
    }


    private val tokenRequest = GenerateProjectTokenRequestDTO(
        role = ProjectRole.USER,
        description = "Test token for integration testing",
        expirationDays = 30L
    )

    private val adminTokenRequest = GenerateProjectTokenRequestDTO(
        role = ProjectRole.ADMIN,
        description = "Admin test token",
        expirationDays = 7L
    )

    @Test
    @Order(1)
    fun `should generate project token successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)

            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(tokenRequest, headers)

            val response: ResponseEntity<ValidResponseData<ProjectTokenResponseDTO>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/tokens",
                    HttpMethod.POST,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertNotNull(response.body?.data?.token)
            assertEquals(tokenRequest.role, response.body?.data?.role)
            assertEquals(tokenRequest.description, response.body?.data?.description)
            assertEquals(projectKey, response.body?.data?.projectKey)
            assertFalse(response.body?.data?.isRevoked ?: true)
            assertNotNull(response.body?.data?.expiresAt)
        }
    }

    @Test
    @Order(2)
    fun `should get project tokens successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)
            val headers = TestUtils.getAuthHeader(token)

            // First generate a token
            val tokenRequestEntity = HttpEntity(tokenRequest, headers)
            restTemplate.exchange(
                "/api/projects/$projectKey/tokens",
                HttpMethod.POST,
                tokenRequestEntity,
                object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
            )

            // Then get all tokens for the project
            val getRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<List<ProjectTokenResponseDTO>>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/tokens",
                    HttpMethod.GET,
                    getRequest,
                    object : ParameterizedTypeReference<ValidResponseData<List<ProjectTokenResponseDTO>>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertTrue(response.body?.data!!.isNotEmpty())
            assertTrue(response.body?.data!!.any { it.description == tokenRequest.description })
        }
    }

    @Test
    @Order(3)
    fun `should revoke project token successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)
            val headers = TestUtils.getAuthHeader(token)

            // First generate a token
            val tokenRequestEntity = HttpEntity(tokenRequest, headers)
            val tokenResponse = restTemplate.exchange(
                "/api/projects/$projectKey/tokens",
                HttpMethod.POST,
                tokenRequestEntity,
                object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
            )

            val tokenId = tokenResponse.body?.data?.id
            assertNotNull(tokenId)

            // Then revoke the token
            val revokeRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<Nothing>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/tokens/$tokenId",
                    HttpMethod.DELETE,
                    revokeRequest,
                    object : ParameterizedTypeReference<ValidResponseData<Nothing>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
        }
    }

    @Test
    @Order(4)
    fun `should get user project tokens successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)
            val headers = TestUtils.getAuthHeader(token)

            // First generate a token
            val tokenRequestEntity = HttpEntity(tokenRequest, headers)
            restTemplate.exchange(
                "/api/projects/$projectKey/tokens",
                HttpMethod.POST,
                tokenRequestEntity,
                object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
            )

            // Then get all user's project tokens
            val getRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<List<ProjectTokenResponseDTO>>> =
                restTemplate.exchange(
                    "/api/projects/tokens/my",
                    HttpMethod.GET,
                    getRequest,
                    object : ParameterizedTypeReference<ValidResponseData<List<ProjectTokenResponseDTO>>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertTrue(response.body?.data!!.isNotEmpty())
            assertTrue(response.body?.data!!.any { it.projectKey == projectKey })
        }
    }

    @Test
    @Order(5)
    fun `should revoke all user project tokens successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)
            val headers = TestUtils.getAuthHeader(token)

            // First generate multiple tokens
            val tokenRequestEntity1 = HttpEntity(tokenRequest, headers)
            restTemplate.exchange(
                "/api/projects/$projectKey/tokens",
                HttpMethod.POST,
                tokenRequestEntity1,
                object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
            )

            val tokenRequestEntity2 = HttpEntity(adminTokenRequest, headers)
            restTemplate.exchange(
                "/api/projects/$projectKey/tokens",
                HttpMethod.POST,
                tokenRequestEntity2,
                object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
            )

            // Then revoke all user tokens
            val revokeRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<Nothing>> =
                restTemplate.exchange(
                    "/api/projects/tokens/my",
                    HttpMethod.DELETE,
                    revokeRequest,
                    object : ParameterizedTypeReference<ValidResponseData<Nothing>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
        }
    }

    @Test
    @Order(6)
    fun `should generate admin token successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)
            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(adminTokenRequest, headers)

            val response: ResponseEntity<ValidResponseData<ProjectTokenResponseDTO>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/tokens",
                    HttpMethod.POST,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertEquals(ProjectRole.ADMIN, response.body?.data?.role)
            assertEquals(adminTokenRequest.description, response.body?.data?.description)
        }
    }

    @Test
    @Order(7)
    fun `should fail to generate token with invalid role`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)

            val invalidTokenRequest = GenerateProjectTokenRequestDTO(
                role = null, // Invalid: null role
                description = "Invalid token",
                expirationDays = 30L
            )

            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(invalidTokenRequest, headers)

            val response: ResponseEntity<ValidResponseData<*>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/tokens",
                    HttpMethod.POST,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<*>>() {}
                )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    @Test
    @Order(8)
    fun `should fail to generate token for non-existent project`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(tokenRequest, headers)

            val response: ResponseEntity<ValidResponseData<ProjectTokenResponseDTO>> =
                restTemplate.exchange(
                    "/api/projects/NON_EXISTENT_PROJECT/tokens",
                    HttpMethod.POST,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
                )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }
    }

    @Test
    @Order(9)
    fun `should fail to revoke non-existent token`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)
            val nonExistentTokenId = UUID.randomUUID()
            val headers = TestUtils.getAuthHeader(token)

            val revokeRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<Nothing>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/tokens/$nonExistentTokenId",
                    HttpMethod.DELETE,
                    revokeRequest,
                    object : ParameterizedTypeReference<ValidResponseData<Nothing>>() {}
                )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }
    }

    @Test
    @Order(10)
    fun `should generate token with custom expiration days`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = ProjectControllerTest.createTestProject()
            val projectKey = ProjectControllerTest.createProjectAndReturnKey(token, testProject, restTemplate)

            val customTokenRequest = GenerateProjectTokenRequestDTO(
                role = ProjectRole.USER,
                description = "Custom expiration token",
                expirationDays = 90L // 3 months
            )

            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(customTokenRequest, headers)

            val response: ResponseEntity<ValidResponseData<ProjectTokenResponseDTO>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/tokens",
                    HttpMethod.POST,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<ProjectTokenResponseDTO>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertNotNull(response.body?.data?.expiresAt)
            assertEquals(customTokenRequest.description, response.body?.data?.description)
        }
    }

    @Test
    @Order(11)
    fun `should fail to get tokens for non-existent project`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val headers = TestUtils.getAuthHeader(token)
            val getRequest = HttpEntity(null, headers)

            val response: ResponseEntity<ValidResponseData<List<ProjectTokenResponseDTO>>> =
                restTemplate.exchange(
                    "/api/projects/NON_EXISTENT_PROJECT/tokens",
                    HttpMethod.GET,
                    getRequest,
                    object : ParameterizedTypeReference<ValidResponseData<List<ProjectTokenResponseDTO>>>() {}
                )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }
    }
}
