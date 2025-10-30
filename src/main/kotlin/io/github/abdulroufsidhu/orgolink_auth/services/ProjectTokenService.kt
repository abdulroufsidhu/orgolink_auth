package io.github.abdulroufsidhu.orgolink_auth.services

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.GenerateProjectTokenRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectTokenResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.model.ProjectAccessToken
import io.github.abdulroufsidhu.orgolink_auth.model.ProjectRole
import io.github.abdulroufsidhu.orgolink_auth.repo.ProjectAccessTokenRepo
import io.github.abdulroufsidhu.orgolink_auth.repo.ProjectRepo
import io.github.abdulroufsidhu.orgolink_auth.repo.ProjectUserRepo
import io.github.abdulroufsidhu.orgolink_auth.repo.UserRepo
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.Key
import java.util.Date
import java.util.UUID

@Service
@Transactional
class ProjectTokenService(
    private val projectRepo: ProjectRepo,
    private val projectUserRepo: ProjectUserRepo,
    private val projectAccessTokenRepo: ProjectAccessTokenRepo,
    private val userRepo: UserRepo,
    @Value("\${jwt.secret}") private val secretKey: String
) {

    suspend fun generateProjectToken(
        projectKey: String,
        requestDTO: GenerateProjectTokenRequestDTO,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<ProjectTokenResponseDTO>> = withContext(Dispatchers.IO) {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return@withContext ResponseEntity.notFound().build()

        // Check if user has permission to generate tokens (OWNER or ADMIN)
        val userRole = projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (userRole?.role !in listOf(ProjectRole.OWNER, ProjectRole.ADMIN)) {
            return@withContext ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    ValidResponseData(
                        message =
                            "Insufficient permissions to generate tokens for this project",
                        data = null
                    )
                )
        }

        // Only owners can generate owner tokens
        if (requestDTO.role == ProjectRole.OWNER && userRole?.role != ProjectRole.OWNER) {
            return@withContext ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    ValidResponseData(
                        message = "Only owners can generate owner tokens",
                        data = null
                    )
                )
        }

        try {
            val now = Date()
            val expirationDate =
                Date(now.time + (requestDTO.expirationDays ?: 30) * 24 * 60 * 60 * 1000)

            val token =
                generateSecureToken(
                    userPrincipal.id!!,
                    project.id!!,
                    requestDTO.role!!,
                    expirationDate
                )

            val projectAccessToken =
                ProjectAccessToken(
                    token = token,
                    expiresAt = expirationDate,
                    projectId = project.id!!,
                    userId = userPrincipal.id!!,
                    role = requestDTO.role!!,
                    description = requestDTO.description,
                    isRevoked = false
                )

            val savedToken = projectAccessTokenRepo.save(projectAccessToken)

            val project = projectRepo.findById(savedToken.projectId!!).orElse(null)
            val user = userRepo.findById(savedToken.userId!!).orElse(null)

            return@withContext ResponseEntity.ok(
                ValidResponseData(
                    message = "Project access token generated successfully",
                    data = ProjectTokenResponseDTO.from(savedToken, project, user.username)
                )
            )
        } catch (e: Exception) {
            return@withContext ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to generate project token: ${e.message}",
                        data = null
                    )
                )
        }
    }

    suspend fun getProjectTokens(
        projectKey: String,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<List<ProjectTokenResponseDTO>>> = withContext(Dispatchers.IO) {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return@withContext ResponseEntity.notFound().build()

        // Check if user has permission to view tokens (OWNER or ADMIN)
        val userRole = projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (userRole?.role !in listOf(ProjectRole.OWNER, ProjectRole.ADMIN)) {
            return@withContext ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    ValidResponseData(
                        message = "Insufficient permissions to view tokens for this project",
                        data = null
                    )
                )
        }

        val tokens = projectAccessTokenRepo.findByProjectIdAndIsRevokedFalse(project.id!!)
        val tokenDTOs =
            tokens.map {
                val user = userRepo.findById(it.userId!!).orElse(null)
                ProjectTokenResponseDTO.from(it, project, user.username)
            }

        return@withContext ResponseEntity.ok(
            ValidResponseData(message = "Project tokens retrieved successfully", data = tokenDTOs)
        )
    }

    suspend fun revokeProjectToken(
        projectKey: String,
        tokenId: UUID,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> = withContext(Dispatchers.IO) {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return@withContext ResponseEntity.notFound().build()

        // Check if user has permission to revoke tokens (OWNER or ADMIN)
        val userRole = projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (userRole?.role !in listOf(ProjectRole.OWNER, ProjectRole.ADMIN)) {
            return@withContext ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    ValidResponseData(
                        message =
                            "Insufficient permissions to revoke tokens for this project",
                        data = null
                    )
                )
        }

        val token =
            projectAccessTokenRepo.findById(tokenId).orElse(null)
                ?: return@withContext ResponseEntity.notFound().build()

        if (token.projectId != project.id) {
            return@withContext ResponseEntity.badRequest()
                .body(
                    ValidResponseData(
                        message = "Token does not belong to this project",
                        data = null
                    )
                )
        }

        try {
            token.isRevoked = true
            projectAccessTokenRepo.save(token)

            return@withContext ResponseEntity.ok(
                ValidResponseData(message = "Project token revoked successfully", data = null)
            )
        } catch (e: Exception) {
            return@withContext ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to revoke project token: ${e.message}",
                        data = null
                    )
                )
        }
    }

    suspend fun validateProjectToken(token: String): ProjectAccessToken? = withContext(Dispatchers.IO) {
        return@withContext try {
            val projectToken = projectAccessTokenRepo.findValidToken(token)
            if (projectToken != null && isTokenValid(projectToken)) {
                projectToken
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getUserProjectTokens(
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<List<ProjectTokenResponseDTO>>> = withContext(Dispatchers.IO) {
        val tokens = projectAccessTokenRepo.findValidTokensByUserId(userPrincipal.id!!)
        val tokenDTOs = tokens.map {
            val project = projectRepo.findById(it.projectId!!).orElse(null)
            val user = userRepo.findById(it.userId!!).orElse(null)
            ProjectTokenResponseDTO.from(it, project, user.username)
        }

        return@withContext ResponseEntity.ok(
            ValidResponseData(
                message = "User project tokens retrieved successfully",
                data = tokenDTOs
            )
        )
    }

    suspend fun revokeAllUserProjectTokens(
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> = withContext(Dispatchers.IO) {
        try {
            val tokens = projectAccessTokenRepo.findByUserIdAndIsRevokedFalse(userPrincipal.id!!)
            tokens.forEach { token ->
                token.isRevoked = true
                projectAccessTokenRepo.save(token)
            }

            return@withContext ResponseEntity.ok(
                ValidResponseData(
                    message = "All user project tokens revoked successfully",
                    data = null
                )
            )
        } catch (e: Exception) {
            return@withContext ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to revoke user project tokens: ${e.message}",
                        data = null
                    )
                )
        }
    }

    private suspend fun generateSecureToken(
        userId: UUID,
        projectId: UUID,
        role: ProjectRole,
        expirationDate: Date
    ): String = withContext(Dispatchers.Default) {
        val claims =
            mapOf(
                "userId" to userId.toString(),
                "projectId" to projectId.toString(),
                "role" to role.name,
                "type" to "project_access"
            )

        return@withContext Jwts.builder()
            .setClaims(claims)
            .setSubject("project_access")
            .setIssuedAt(Date())
            .setExpiration(expirationDate)
            .signWith(getSignInKey(), SignatureAlgorithm.HS256)
            .compact()
    }

    suspend private fun isTokenValid(projectToken: ProjectAccessToken): Boolean =
        withContext(Dispatchers.Default) { !projectToken.isRevoked && projectToken.expiresAt?.after(Date()) == true }

    private suspend fun getSignInKey(): Key = withContext(Dispatchers.Default) {
        val keyBytes = Decoders.BASE64.decode(secretKey)
        return@withContext Keys.hmacShaKeyFor(keyBytes)
    }

    suspend fun extractClaimsFromToken(token: String): Claims? = withContext(Dispatchers.Default) {
        return@withContext try {
            Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).body
        } catch (e: Exception) {
            null
        }
    }
}
