package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.TestUtils
import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.AddUserToProjectRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.CreateProjectRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectUserResponseDTO
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

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.AUTO_CONFIGURED)
@TestPropertySource(
    properties = [
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
    ]
)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class ProjectControllerTest {

    @LocalServerPort
    private var port: Int = 0

    private lateinit var restTemplate: TestRestTemplate

    @BeforeEach
    fun setUp() {
        this.restTemplate =
            TestRestTemplate(RestTemplateBuilder().rootUri("http://localhost:$port"))
    }

    companion object {
        fun createTestProject(suffix: String = System.currentTimeMillis().toString()) =
            CreateProjectRequestDTO(
                name = "Test Project $suffix",
                description = "A test project for integration testing",
                projectKey = "TEST_PROJECT_$suffix",
                isPublic = false
            )

        fun createPublicTestProject(suffix: String = System.currentTimeMillis().toString()) =
            CreateProjectRequestDTO(
                name = "Public Test Project $suffix",
                description = "A public test project",
                projectKey = "PUBLIC_TEST_$suffix",
                isPublic = true
            )

        fun createProjectAndReturnKey(
            token: String?,
            testProject: CreateProjectRequestDTO,
            restTemplate: TestRestTemplate
        ): String {
            val headers = TestUtils.getAuthHeader(token)
            val createRequest = HttpEntity(testProject, headers)
            val response = restTemplate.exchange(
                "/api/projects",
                HttpMethod.POST,
                createRequest,
                object : ParameterizedTypeReference<ValidResponseData<ProjectResponseDTO>>() {}
            )
            return response.body?.data?.projectKey ?: testProject.projectKey!!
        }
    }

    private fun createProjectAndReturnKey(
        token: String?,
        testProject: CreateProjectRequestDTO
    ): String {
        val headers = TestUtils.getAuthHeader(token)
        val createRequest = HttpEntity(testProject, headers)
        val response = restTemplate.exchange(
            "/api/projects",
            HttpMethod.POST,
            createRequest,
            object : ParameterizedTypeReference<ValidResponseData<ProjectResponseDTO>>() {}
        )
        return response.body?.data?.projectKey ?: testProject.projectKey!!
    }

    @Test
    @Order(1)
    fun `should create project successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = createTestProject()
            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(testProject, headers)

            val response: ResponseEntity<ValidResponseData<ProjectResponseDTO>> =
                restTemplate.exchange(
                    "/api/projects",
                    HttpMethod.POST,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<ProjectResponseDTO>>() {}
                )

            assertEquals(HttpStatus.CREATED, response.statusCode)
            assertNotNull(response.body?.data)
            assertEquals(testProject.name, response.body?.data?.name)
            assertEquals(testProject.projectKey, response.body?.data?.projectKey)
            assertEquals(testProject.isPublic, response.body?.data?.isPublic)
            assertEquals(ProjectRole.OWNER, response.body?.data?.userRole)
        }
    }

    @Test
    @Order(2)
    fun `should get user projects successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = createTestProject()
            createProjectAndReturnKey(token, testProject)

            val headers = TestUtils.getAuthHeader(token)
            val getRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<List<ProjectResponseDTO>>> =
                restTemplate.exchange(
                    "/api/projects",
                    HttpMethod.GET,
                    getRequest,
                    object :
                        ParameterizedTypeReference<ValidResponseData<List<ProjectResponseDTO>>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertTrue(response.body?.data!!.isNotEmpty())
            assertTrue(response.body?.data!!.any { it.projectKey == testProject.projectKey })
        }
    }

    @Test
    @Order(3)
    fun `should get public projects successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val publicTestProject = createPublicTestProject()
            createProjectAndReturnKey(token, publicTestProject)

            val headers = TestUtils.getAuthHeader(token)
            val getRequest = HttpEntity(null, headers)

            // Get public projects (no auth needed for public endpoint)
            val response: ResponseEntity<ValidResponseData<List<ProjectResponseDTO>>> =
                restTemplate.exchange(
                    "/api/projects/public",
                    HttpMethod.GET,
                    getRequest,
                    object :
                        ParameterizedTypeReference<ValidResponseData<List<ProjectResponseDTO>>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertTrue(response.body?.data!!.all { it.isPublic })
        }
    }

    @Test
    @Order(4)
    fun `should get project by key successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = createTestProject()
            val projectKey = createProjectAndReturnKey(token, testProject)

            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(null, headers)

            val response: ResponseEntity<ValidResponseData<ProjectResponseDTO>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey",
                    HttpMethod.GET,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<ProjectResponseDTO>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertEquals(projectKey, response.body?.data?.projectKey)
        }
    }

    @Test
    @Order(5)
    fun `should update project successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = createTestProject()
            val projectKey = createProjectAndReturnKey(token, testProject)

            val updatedProject = CreateProjectRequestDTO(
                name = "Updated Test Project",
                description = "Updated description",
                projectKey = projectKey,
                isPublic = true
            )

            val headers = TestUtils.getAuthHeader(token)
            val updateRequest = HttpEntity(updatedProject, headers)
            val response: ResponseEntity<ValidResponseData<ProjectResponseDTO>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey",
                    HttpMethod.PUT,
                    updateRequest,
                    object : ParameterizedTypeReference<ValidResponseData<ProjectResponseDTO>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertEquals(updatedProject.name, response.body?.data?.name)
            assertEquals(updatedProject.description, response.body?.data?.description)
            assertEquals(updatedProject.isPublic, response.body?.data?.isPublic)
        }
    }

    @Test
    @Order(6)
    fun `should add user to project successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = createTestProject()
            val projectKey = createProjectAndReturnKey(token, testProject)

            val addUserRequest = AddUserToProjectRequestDTO(
                username = "testuser",
                role = ProjectRole.USER
            )

            val headers = TestUtils.getAuthHeader(token)
            val addUserRequestEntity = HttpEntity(addUserRequest, headers)
            val response: ResponseEntity<ValidResponseData<ProjectUserResponseDTO>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/users",
                    HttpMethod.POST,
                    addUserRequestEntity,
                    object :
                        ParameterizedTypeReference<ValidResponseData<ProjectUserResponseDTO>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertEquals(addUserRequest.username, response.body?.data?.username)
            assertEquals(addUserRequest.role, response.body?.data?.role)
        }
    }

    @Test
    @Order(7)
    fun `should get project users successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = createTestProject()
            val projectKey = createProjectAndReturnKey(token, testProject)

            val headers = TestUtils.getAuthHeader(token)
            val getUsersRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<List<ProjectUserResponseDTO>>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/users",
                    HttpMethod.GET,
                    getUsersRequest,
                    object :
                        ParameterizedTypeReference<ValidResponseData<List<ProjectUserResponseDTO>>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
            assertNotNull(response.body?.data)
            assertTrue(response.body?.data!!.isNotEmpty())
            // Creator should be in the project with OWNER role
            assertTrue(response.body?.data!!.any { it.role == ProjectRole.OWNER })
        }
    }

    @Test
    @Order(8)
    fun `should remove user from project successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = createTestProject()
            val projectKey = createProjectAndReturnKey(token, testProject)

            val addUserRequest = AddUserToProjectRequestDTO(
                username = "testuser",
                role = ProjectRole.USER
            )

            val headers = TestUtils.getAuthHeader(token)

            // First add user to project
            val addUserRequestEntity = HttpEntity(addUserRequest, headers)
            restTemplate.exchange(
                "/api/projects/$projectKey/users",
                HttpMethod.POST,
                addUserRequestEntity,
                object : ParameterizedTypeReference<ValidResponseData<ProjectUserResponseDTO>>() {}
            )

            // Then remove user from project
            val removeUserRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<Nothing>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey/users/${addUserRequest.username}",
                    HttpMethod.DELETE,
                    removeUserRequest,
                    object : ParameterizedTypeReference<ValidResponseData<Nothing>>() {}
                )

            // cannot remove last owner of the project
            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    @Test
    @Order(9)
    fun `should delete project successfully`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val testProject = createTestProject()
            val projectKey = createProjectAndReturnKey(token, testProject)

            val headers = TestUtils.getAuthHeader(token)
            val deleteRequest = HttpEntity(null, headers)
            val response: ResponseEntity<ValidResponseData<Nothing>> =
                restTemplate.exchange(
                    "/api/projects/$projectKey",
                    HttpMethod.DELETE,
                    deleteRequest,
                    object : ParameterizedTypeReference<ValidResponseData<Nothing>>() {}
                )

            assertEquals(HttpStatus.OK, response.statusCode)
        }
    }

    @Test
    @Order(10)
    fun `should fail to create project with invalid data`() {
        TestUtils.authenticatedTest(restTemplate) { token ->
            val invalidProject = CreateProjectRequestDTO(
                name = "", // Invalid: blank name
                description = "Test description",
                projectKey = "ab", // Invalid: too short
                isPublic = false
            )

            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(invalidProject, headers)

            val response: ResponseEntity<ValidResponseData<*>> =
                restTemplate.exchange(
                    "/api/projects",
                    HttpMethod.POST,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<*>>() {}
                )

            assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        }
    }

    @Test
    @Order(11)
    fun `should fail to get project with non-existent key`() {
        TestUtils.authenticatedTest(restTemplate) {token ->
            val headers = TestUtils.getAuthHeader(token)
            val request = HttpEntity(null, headers)

            val response: ResponseEntity<ValidResponseData<*>> =
                restTemplate.exchange(
                    "/api/projects/NON_EXISTENT_PROJECT",
                    HttpMethod.GET,
                    request,
                    object : ParameterizedTypeReference<ValidResponseData<*>>() {}
                )

            assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        }
    }
}
