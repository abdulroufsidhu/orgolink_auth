package com.orgolink.auth.domain.dto

import com.orgolink.auth.domain.entity.User
import java.time.LocalDateTime

// ============================================================================
// Authentication DTOs
// ============================================================================

data class LoginRequest(val username: String, val password: String)

data class RegisterRequest(
        val username: String,
        val email: String,
        val password: String,
        val createdBy: Long? = null // Optional: for admin creating users
)

data class LoginResponse(val token: String, val user: UserResponse)

data class RegisterResponse(val token: String, val user: UserResponse)

// ============================================================================
// Generic API Response
// ============================================================================

data class ApiResponse<T>(val status: String, val message: String, val data: T? = null) {
    companion object {
        fun <T> success(message: String = "Success", data: T? = null): ApiResponse<T> {
            return ApiResponse("success", message, data)
        }

        fun <T> error(message: String, data: T? = null): ApiResponse<T> {
            return ApiResponse("error", message, data)
        }
    }
}

// ============================================================================
// User DTOs
// ============================================================================

data class UserResponse(
        val id: Long,
        val username: String,
        val email: String,
        val roles: List<String>,
        val createdAt: LocalDateTime,
        val createdBy: Long? = null
) {
    companion object {
        fun from(user: User): UserResponse {
            return UserResponse(
                    id = user.id ?: 0,
                    username = user.username,
                    email = user.email,
                    roles = user.roles.map { it.name },
                    createdAt = user.createdAt,
                    createdBy = user.createdBy
            )
        }
    }
}

// ============================================================================
// Password Reset DTOs
// ============================================================================

data class ForgotPasswordRequest(val email: String)

data class ForgotPasswordResponse(
        val message: String,
        val resetToken: String? =
                null // For testing/dev, in production this should be sent via email
)

data class ResetPasswordRequest(val newPassword: String)

// ============================================================================
// Verification DTOs
// ============================================================================

data class VerifyRoleRequest(val role: String)

data class VerifyPermissionRequest(val permission: String)
