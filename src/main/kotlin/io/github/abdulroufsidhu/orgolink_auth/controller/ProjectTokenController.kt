package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.GenerateProjectTokenRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectTokenResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.services.ProjectTokenService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import kotlinx.coroutines.runBlocking
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/projects")
@Tag(name = "Project Token Management", description = "APIs for managing project access tokens")
@SecurityRequirement(name = "bearerAuth")
class ProjectTokenController(private val projectTokenService: ProjectTokenService) {

    @PostMapping(
        "/{projectKey}/tokens",
    )
    @Operation(
        summary = "Generate project access token",
        description =
            "Generates a new access token for the project (requires OWNER or ADMIN role)"
    )
    fun generateProjectToken(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @Valid @RequestBody requestDTO: GenerateProjectTokenRequestDTO,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<ProjectTokenResponseDTO>> =
        runBlocking {
            projectTokenService.generateProjectToken(
                projectKey,
                requestDTO,
                userPrincipal
            )
        }

    @GetMapping("/{projectKey}/tokens")
    @Operation(
        summary = "Get project tokens",
        description = "Retrieves all active tokens for the project (requires OWNER or ADMIN role)"
    )
    fun getProjectTokens(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<List<ProjectTokenResponseDTO>>> =
        runBlocking { projectTokenService.getProjectTokens(projectKey, userPrincipal) }

    @DeleteMapping("/{projectKey}/tokens/{tokenId}")
    @Operation(
        summary = "Revoke project token",
        description = "Revokes a specific project access token (requires OWNER or ADMIN role)"
    )
    fun revokeProjectToken(
        @Parameter(description = "Project key") @PathVariable projectKey: String,
        @Parameter(description = "Token ID") @PathVariable tokenId: UUID,
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> =
        runBlocking { projectTokenService.revokeProjectToken(projectKey, tokenId, userPrincipal) }

    @GetMapping("/tokens/my")
    @Operation(
        summary = "Get user's project tokens",
        description = "Retrieves all active project tokens created by the authenticated user"
    )
    fun getUserProjectTokens(
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<List<ProjectTokenResponseDTO>>> =
        runBlocking { projectTokenService.getUserProjectTokens(userPrincipal) }

    @DeleteMapping("/tokens/my")
    @Operation(
        summary = "Revoke all user project tokens",
        description = "Revokes all project tokens created by the authenticated user"
    )
    fun revokeAllUserProjectTokens(
        @AuthenticationPrincipal userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> =
        runBlocking { projectTokenService.revokeAllUserProjectTokens(userPrincipal) }
}
