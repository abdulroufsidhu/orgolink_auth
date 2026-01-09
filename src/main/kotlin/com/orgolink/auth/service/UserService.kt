package com.orgolink.auth.service

import com.orgolink.auth.config.AuthProperties
import com.orgolink.auth.domain.dto.UserResponse
import com.orgolink.auth.repository.UserRepository
import com.orgolink.auth.security.JwtUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
        private val userRepository: UserRepository,
        private val authProperties: AuthProperties,
        private val jwtUtil: JwtUtil,
        private val tokenService: TokenService
) {

    fun getAuthenticatedUser(tokenValue: String): UserResponse {
        val userId =
                jwtUtil.getUserIdFromToken(tokenValue)
                        ?: throw IllegalArgumentException("Invalid token")

        val user =
                userRepository.findActiveById(userId).orElseThrow {
                    IllegalArgumentException("User not found")
                }

        return UserResponse.from(user)
    }

    @Transactional
    fun deleteOwnAccount(tokenValue: String) {
        val userId =
                jwtUtil.getUserIdFromToken(tokenValue)
                        ?: throw IllegalArgumentException("Invalid token")

        val user =
                userRepository.findActiveById(userId).orElseThrow {
                    IllegalArgumentException("User not found")
                }

        deleteUser(user, userId)

        // Invalidate all user's tokens
        tokenService.invalidateToken(tokenValue)
    }

    @Transactional
    fun deleteUserByUid(uid: Long, tokenValue: String) {
        val currentUserId =
                jwtUtil.getUserIdFromToken(tokenValue)
                        ?: throw IllegalArgumentException("Invalid token")

        val userToDelete =
                userRepository.findActiveById(uid).orElseThrow {
                    IllegalArgumentException("User not found")
                }

        deleteUser(userToDelete, currentUserId)
    }

    private fun deleteUser(user: com.orgolink.auth.domain.entity.User, deletedBy: Long) {
        val deleteScheme = authProperties.deleteScheme.lowercase()

        when (deleteScheme) {
            "soft" -> {
                user.softDelete(deletedBy)
                userRepository.save(user)
            }
            "hard" -> {
                userRepository.delete(user)
            }
            else -> {
                throw IllegalStateException("Invalid delete scheme: $deleteScheme")
            }
        }
    }
}
