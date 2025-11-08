package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.model.Project
import io.github.abdulroufsidhu.orgolink_auth.services.ProjectService
import io.github.abdulroufsidhu.orgolink_auth.services.ProjectTokenService
import io.github.abdulroufsidhu.orgolink_auth.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import kotlinx.coroutines.runBlocking
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/public")
@Tag(name = "Public API", description = "Public endpoints that don't require authentication")
class PublicApiController(
    private val projectService: ProjectService,
    private val projectTokenService: ProjectTokenService,
    private val userService: UserService,
) {

    @GetMapping("/projects")
    @Operation(
        summary = "Get all public projects",
        description = "Retrieves all publicly available projects"
    )
    fun getPublicProjects(): ResponseEntity<ValidResponseData<List<ProjectResponseDTO>>> =
        runBlocking {
            projectService.getPublicProjects()
        }

    @GetMapping("/projects/{projectKey}")
    @Operation(
        summary = "Get public project by key",
        description = "Retrieves a public project by its key"
    )
    fun getPublicProject(
        @Parameter(description = "Project key") @PathVariable projectKey: String
    ): ResponseEntity<ValidResponseData<ProjectResponseDTO>> = runBlocking {
        val response = projectService.getProjectByKey(projectKey)

        if (response.statusCode == HttpStatus.OK) {
            val project = response.body?.data
            if (project?.isPublic == true) {
                return@runBlocking response
            }
        }

        return@runBlocking ResponseEntity.notFound().build()
    }

    @PostMapping(
        "/projects/{projectKey}/validate-token",
        consumes = [MediaType.MULTIPART_FORM_DATA_VALUE, MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    @Operation(
        summary = "Validate project access token",
        description = "Validates a project access token and returns project information if valid"
    )
    fun validateProjectToken(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @RequestBody tokenRequest: TokenValidationRequest
    ): ResponseEntity<ValidResponseData<ProjectTokenValidationResponse>> = runBlocking {
        val projectToken = projectTokenService.validateProjectToken(tokenRequest.token)

        val project: Project? = projectService.findById(projectToken?.projectId)
        val user = userService.findById(projectToken?.userId)

        if (projectToken != null && project?.projectKey == projectKey) {
            val response =
                ProjectTokenValidationResponse(
                    valid = true,
                    projectId = projectToken.projectId,
                    projectKey = project?.projectKey,
                    projectName = project?.name,
                    userId = projectToken.userId,
                    username = user?.username,
                    role = projectToken.role,
                    expiresAt = projectToken.expiresAt
                )

            return@runBlocking ResponseEntity.ok(
                ValidResponseData(
                    message = "Token is valid",
                    data = response
                )
            )
        }

        return@runBlocking ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(
                ValidResponseData(
                    message = "Invalid or expired token",
                    data = ProjectTokenValidationResponse(valid = false)
                )
            )
    }

    @GetMapping("/health")
    @Operation(summary = "Health check", description = "Simple health check endpoint")
    fun healthCheck(): ResponseEntity<ValidResponseData<String>> = runBlocking {
        ResponseEntity.ok(ValidResponseData(message = "Service is running", data = "OK"))
    }
}

data class TokenValidationRequest(val token: String)

data class ProjectTokenValidationResponse(
    val valid: Boolean,
    val projectId: java.util.UUID? = null,
    val projectKey: String? = null,
    val projectName: String? = null,
    val userId: java.util.UUID? = null,
    val username: String? = null,
    val role: io.github.abdulroufsidhu.orgolink_auth.model.ProjectRole? = null,
    val expiresAt: java.util.Date? = null
)
