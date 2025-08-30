package io.github.abdulroufsidhu.orgolink_auth.dto.responsedto

import io.github.abdulroufsidhu.orgolink_auth.model.Project
import io.github.abdulroufsidhu.orgolink_auth.model.ProjectAccessToken
import io.github.abdulroufsidhu.orgolink_auth.model.ProjectRole
import java.util.Date
import java.util.UUID

data class ProjectTokenResponseDTO(
        val id: UUID?,
        val token: String?,
        val expiresAt: Date?,
        val isRevoked: Boolean,
        val projectId: UUID?,
        val userId: UUID?,
        val role: ProjectRole,
        val description: String?,
        val projectName: String?,
        val projectKey: String?,
        val username: String?,
        val createdAt: Date?,
        val updatedAt: Date?
) {
  companion object {
    fun from(projectToken: ProjectAccessToken, project: Project?, username: String?): ProjectTokenResponseDTO {
      return ProjectTokenResponseDTO(
              id = projectToken.id,
              token = projectToken.token,
              expiresAt = projectToken.expiresAt,
              isRevoked = projectToken.isRevoked,
              projectId = projectToken.projectId,
              userId = projectToken.userId,
              role = projectToken.role,
              description = projectToken.description,
              projectName = project?.name , // projectToken.project?.name,
              projectKey = project?.projectKey , // projectToken.project?.projectKey,
              username = username , // projectToken.user?.username,
              createdAt = projectToken.created_at,
              updatedAt = projectToken.updated_at
      )
    }
  }
}
