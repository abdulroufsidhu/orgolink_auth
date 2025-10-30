package io.github.abdulroufsidhu.orgolink_auth.services

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.AddUserToProjectRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.CreateProjectRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.dto.responsedto.ProjectUserResponseDTO
import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.model.Project
import io.github.abdulroufsidhu.orgolink_auth.model.ProjectRole
import io.github.abdulroufsidhu.orgolink_auth.model.ProjectUser
import io.github.abdulroufsidhu.orgolink_auth.repo.ProjectRepo
import io.github.abdulroufsidhu.orgolink_auth.repo.ProjectUserRepo
import io.github.abdulroufsidhu.orgolink_auth.repo.UserRepo
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
@Transactional
class ProjectService(
    private val projectRepo: ProjectRepo,
    private val projectUserRepo: ProjectUserRepo,
    private val userRepo: UserRepo
) {

    fun createProject(
        requestDTO: CreateProjectRequestDTO,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<ProjectResponseDTO>> {
        try {
            // Check if project key already exists
            if (projectRepo.existsByProjectKey(requestDTO.projectKey!!)) {
                return ResponseEntity.badRequest()
                    .body(ValidResponseData(message = "Project key already exists", data = null))
            }

            // Create project
            val project = projectRepo.save(requestDTO.toProject())

            // Add creator as owner
            val projectUser =
                ProjectUser(
                    userId = userPrincipal.id!!,
                    projectId = project.id!!,
                    role = ProjectRole.OWNER,
                    isActive = true
                )
            projectUserRepo.save(projectUser)

            return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                    ValidResponseData(
                        message = "Project created successfully",
                        data = ProjectResponseDTO.from(project, ProjectRole.OWNER)
                    )
                )
        } catch (e: DataIntegrityViolationException) {
            return ResponseEntity.badRequest()
                .body(ValidResponseData(message = "Project key already exists", data = null))
        } catch (e: Exception) {
            return ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to create project: ${e.message}",
                        data = null
                    )
                )
        }
    }

    fun getProjectByKey(projectKey: String): ResponseEntity<ValidResponseData<ProjectResponseDTO>> {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            ValidResponseData(message = "Project found", data = ProjectResponseDTO.from(project))
        )
    }

    fun getUserProjects(
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<List<ProjectResponseDTO>>> {
        val projects = projectRepo.findProjectsByUserId(userPrincipal.id!!)
        val projectDTOs =
            projects.map { project ->
                val userRole =
                    projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)?.role
                ProjectResponseDTO.from(project, userRole)
            }

        return ResponseEntity.ok(
            ValidResponseData(message = "User projects retrieved successfully", data = projectDTOs)
        )
    }

    fun getPublicProjects(): ResponseEntity<ValidResponseData<List<ProjectResponseDTO>>> {
        val projects = projectRepo.findByIsPublicTrueAndIsActiveTrue()
        val projectDTOs = projects.map { ProjectResponseDTO.from(it) }

        return ResponseEntity.ok(
            ValidResponseData(
                message = "Public projects retrieved successfully",
                data = projectDTOs
            )
        )
    }

    fun addUserToProject(
        projectKey: String,
        requestDTO: AddUserToProjectRequestDTO,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<ProjectUserResponseDTO>> {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return ResponseEntity.notFound().build()

        // Check if current user has permission to add users (OWNER or ADMIN)
        val currentUserRole =
            projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (currentUserRole?.role !in listOf(ProjectRole.OWNER, ProjectRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    ValidResponseData(
                        message = "Insufficient permissions to add users to this project",
                        data = null
                    )
                )
        }

        // Find user to add
        val userToAdd =
            userRepo.findByUsername(requestDTO.username)
                ?: return ResponseEntity.badRequest()
                    .body(ValidResponseData(message = "User not found", data = null))

        // Check if user is already in project
        if (projectUserRepo.existsByUserIdAndProjectIdAndIsActiveTrue(
                userToAdd.id!!,
                project.id!!
            )
        ) {
            return ResponseEntity.badRequest()
                .body(
                    ValidResponseData(
                        message = "User is already a member of this project",
                        data = null
                    )
                )
        }

        // Only owners can add other owners
        if (requestDTO.role == ProjectRole.OWNER && currentUserRole?.role != ProjectRole.OWNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ValidResponseData(message = "Only owners can add other owners", data = null))
        }

        try {
            val projectUser =
                ProjectUser(
                    userId = userToAdd.id!!,
                    projectId = project.id!!,
                    role = requestDTO.role!!,
                    isActive = true
                )
            val savedProjectUser = projectUserRepo.save(projectUser)
            val user = savedProjectUser.userId?.let { userRepo.findById(it).orElse(null) }
            val project = savedProjectUser.projectId?.let { projectRepo.findById(it).orElse(null) }

            return ResponseEntity.ok(
                ValidResponseData(
                    message = "User added to project successfully",
                    data = ProjectUserResponseDTO.from(savedProjectUser, project, user?.username)
                )
            )
        } catch (e: Exception) {
            return ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to add user to project: ${e.message}",
                        data = null
                    )
                )
        }
    }

    fun getProjectUsers(
        projectKey: String,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<List<ProjectUserResponseDTO>>> {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return ResponseEntity.notFound().build()

        // Check if user has access to this project
        val userRole = projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (userRole == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ValidResponseData(message = "Access denied to this project", data = null))
        }

        val projectUsers = projectUserRepo.findByProjectIdAndIsActiveTrue(project.id!!)
        val projectUserDTOs = projectUsers.map {

            val user = it.userId?.let { uid -> userRepo.findById(uid).orElse(null) }
            val project = it.projectId?.let { pid -> projectRepo.findById(pid).orElse(null) }
            ProjectUserResponseDTO.from(it, project, user?.username)
        }

        return ResponseEntity.ok(
            ValidResponseData(
                message = "Project users retrieved successfully",
                data = projectUserDTOs
            )
        )
    }

    fun removeUserFromProject(
        projectKey: String,
        username: String,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return ResponseEntity.notFound().build()

        // Check if current user has permission to remove users (OWNER or ADMIN)
        val currentUserRole =
            projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (currentUserRole?.role !in listOf(ProjectRole.OWNER, ProjectRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    ValidResponseData(
                        message =
                            "Insufficient permissions to remove users from this project",
                        data = null
                    )
                )
        }

        // Find user to remove
        val userToRemove =
            userRepo.findByUsername(username)
                ?: return ResponseEntity.badRequest()
                    .body(ValidResponseData(message = "User not found", data = null))

        val projectUser =
            projectUserRepo.findActiveProjectUser(userToRemove.id!!, project.id!!)
                ?: return ResponseEntity.badRequest()
                    .body(
                        ValidResponseData(
                            message = "User is not a member of this project",
                            data = null
                        )
                    )

        // Only owners can remove other owners
        if (projectUser.role == ProjectRole.OWNER && currentUserRole?.role != ProjectRole.OWNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    ValidResponseData(
                        message = "Only owners can remove other owners",
                        data = null
                    )
                )
        }

        // Cannot remove the last owner
        if (projectUser.role == ProjectRole.OWNER) {
            val ownerCount =
                projectUserRepo.countActiveUsersByProjectIdAndRole(project.id!!, ProjectRole.OWNER)
            if (ownerCount <= 1) {
                return ResponseEntity.badRequest()
                    .body(
                        ValidResponseData(
                            message = "Cannot remove the last owner of the project",
                            data = null
                        )
                    )
            }
        }

        try {
            projectUser.isActive = false
            projectUserRepo.save(projectUser)

            return ResponseEntity.ok(
                ValidResponseData(message = "User removed from project successfully", data = null)
            )
        } catch (e: Exception) {
            return ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to remove user from project: ${e.message}",
                        data = null
                    )
                )
        }
    }

    fun updateProject(
        projectKey: String,
        requestDTO: CreateProjectRequestDTO,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<ProjectResponseDTO>> {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return ResponseEntity.notFound().build()

        // Check if user has permission to update (OWNER or ADMIN)
        val userRole = projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (userRole?.role !in listOf(ProjectRole.OWNER, ProjectRole.ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(
                    ValidResponseData(
                        message = "Insufficient permissions to update this project",
                        data = null
                    )
                )
        }

        // Check if new project key already exists (if changed)
        if (requestDTO.projectKey != project.projectKey &&
            projectRepo.existsByProjectKey(requestDTO.projectKey!!)
        ) {
            return ResponseEntity.badRequest()
                .body(ValidResponseData(message = "Project key already exists", data = null))
        }

        try {
            project.name = requestDTO.name
            project.description = requestDTO.description
            project.projectKey = requestDTO.projectKey
            project.isPublic = requestDTO.isPublic

            val updatedProject = projectRepo.save(project)

            return ResponseEntity.ok(
                ValidResponseData(
                    message = "Project updated successfully",
                    data = ProjectResponseDTO.from(updatedProject, userRole?.role)
                )
            )
        } catch (e: Exception) {
            return ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to update project: ${e.message}",
                        data = null
                    )
                )
        }
    }

    fun deletePermanent(
        projectKey: String,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return ResponseEntity.notFound().build()

        // Check if user is owner
        val userRole = projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (userRole?.role != ProjectRole.OWNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ValidResponseData(message = "Only owners can delete projects", data = null))
        }

        try {
            project.id?.let {
                projectRepo.deleteById(it)
            }

            return ResponseEntity.ok(
                ValidResponseData(message = "Project deleted successfully", data = null)
            )
        } catch (e: Exception) {
            return ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to delete project: ${e.message}",
                        data = null
                    )
                )
        }
    }

    fun deleteProject(
        projectKey: String,
        userPrincipal: OrgoUserPrincipal
    ): ResponseEntity<ValidResponseData<Nothing>> {
        val project =
            projectRepo.findByProjectKey(projectKey) ?: return ResponseEntity.notFound().build()

        // Check if user is owner
        val userRole = projectUserRepo.findActiveProjectUser(userPrincipal.id!!, project.id!!)
        if (userRole?.role != ProjectRole.OWNER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ValidResponseData(message = "Only owners can delete projects", data = null))
        }

        try {
            project.isActive = false
            projectRepo.save(project)

            return ResponseEntity.ok(
                ValidResponseData(message = "Project deleted successfully", data = null)
            )
        } catch (e: Exception) {
            return ResponseEntity.internalServerError()
                .body(
                    ValidResponseData(
                        message = "Failed to delete project: ${e.message}",
                        data = null
                    )
                )
        }
    }

    fun findById(projectId: UUID?): Project? {
        return projectId?.let { projectRepo.findByIdOrNull(it) }
    }
}
