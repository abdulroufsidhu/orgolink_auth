package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.AddUserToProjectRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.CreateProjectRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectUserResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.services.ProjectService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.context.annotation.Profile
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Project Management", description = "APIs for managing projects")
@SecurityRequirement(name = "bearerAuth")
class ProjectController(private val projectService: ProjectService) {

    @PostMapping()
    @Operation(
        summary = "Create a new project",
        description = "Creates a new project with the authenticated user as owner"
    )
    fun createProject(
        @Valid @RequestBody requestDTO: CreateProjectRequestDTO,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<ProjectResponseDTO>> = runBlocking {
        projectService.createProject(requestDTO, userPrincipal)
    }

    @GetMapping()
    @Operation(
        summary = "Get user's projects",
        description = "Retrieves all projects where the user is a member"
    )
    fun getUserProjects(
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<List<ProjectResponseDTO>>> = runBlocking{
        projectService.getUserProjects(userPrincipal)
    }

    @GetMapping("/public")
    @Operation(summary = "Get public projects", description = "Retrieves all public projects")
    fun getPublicProjects(): ResponseEntity<ValidResponseData<List<ProjectResponseDTO>>> = runBlocking {
         projectService.getPublicProjects()
    }

    @GetMapping("/{projectKey}")
    @Operation(
        summary = "Get project by key",
        description = "Retrieves a specific project by its key"
    )
    fun getProjectByKey(
        @Parameter(description = "Project key") @PathVariable projectKey: String
    ): ResponseEntity<ValidResponseData<ProjectResponseDTO>> = runBlocking {
         projectService.getProjectByKey(projectKey)
    }

    @PutMapping("/{projectKey}")
    @Operation(
        summary = "Update project",
        description = "Updates project details (requires OWNER or ADMIN role)"
    )
    fun updateProject(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @Valid @RequestBody requestDTO: CreateProjectRequestDTO,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<ProjectResponseDTO>> = runBlocking{
         projectService.updateProject(projectKey, requestDTO, userPrincipal)
    }

    @DeleteMapping("/{projectKey}")
    @Operation(
        summary = "Delete project",
        description = "Soft deletes a project (requires OWNER role)"
    )
    fun deleteProject(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> = runBlocking{
         projectService.deleteProject(projectKey, userPrincipal)
    }

    @PostMapping("/{projectKey}/users")
    @Operation(
        summary = "Add user to project",
        description =
            "Adds a user to the project with specified role (requires OWNER or ADMIN role)"
    )
    fun addUserToProject(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @Valid @RequestBody requestDTO: AddUserToProjectRequestDTO,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<ProjectUserResponseDTO>> = runBlocking {
         projectService.addUserToProject(projectKey, requestDTO, userPrincipal)
    }

    @GetMapping("/{projectKey}/users")
    @Operation(
        summary = "Get project users",
        description = "Retrieves all users associated with the project"
    )
    fun getProjectUsers(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<List<ProjectUserResponseDTO>>> = runBlocking {
        projectService.getProjectUsers(projectKey, userPrincipal)
    }

    @DeleteMapping("/{projectKey}/users/{username}")
    @Operation(
        summary = "Remove user from project",
        description = "Removes a user from the project (requires OWNER or ADMIN role)"
    )
    fun removeUserFromProject(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @Parameter(description = "Username to remove") @PathVariable username: String,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> = runBlocking {
         projectService.removeUserFromProject(projectKey, username, userPrincipal)
    }
}


@Profile("test")
@RestController
class TestProjectController(private val projectService: ProjectService) {

    @DeleteMapping("/api/projects/{projectKey}/permanent")
    fun delete(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ) = runBlocking { projectService.deletePermanent(projectKey, userPrincipal) }
}
