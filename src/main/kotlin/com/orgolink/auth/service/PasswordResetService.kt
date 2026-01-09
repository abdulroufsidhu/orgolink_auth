package com.orgolink.auth.service

import com.orgolink.auth.config.AuthProperties
import com.orgolink.auth.domain.dto.ForgotPasswordRequest
import com.orgolink.auth.domain.dto.ForgotPasswordResponse
import com.orgolink.auth.domain.dto.ResetPasswordRequest
import com.orgolink.auth.domain.entity.PasswordResetToken
import com.orgolink.auth.repository.PasswordResetTokenRepository
import com.orgolink.auth.repository.UserRepository
import org.springframework.core.env.Environment
import java.time.LocalDateTime
import java.util.*
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PasswordResetService(
        private val userRepository: UserRepository,
        private val passwordResetTokenRepository: PasswordResetTokenRepository,
        private val passwordEncoder: PasswordEncoder,
        private val authProperties: AuthProperties,
        private val environment: Environment,
) {

    @Transactional
    fun initiatePasswordReset(request: ForgotPasswordRequest): ForgotPasswordResponse {
        val user =
                userRepository.findActiveByEmail(request.email).orElseThrow {
                    IllegalArgumentException("User not found with email: ${request.email}")
                }

        // Generate unique token
        val tokenValue = UUID.randomUUID().toString()
        val expiresAt = LocalDateTime.now().plusSeconds(authProperties.passResetTimeout / 1000)

        val resetToken =
                PasswordResetToken(userId = user.id!!, token = tokenValue, expiresAt = expiresAt)

        passwordResetTokenRepository.save(resetToken)

        val sendToken = environment.activeProfiles.contains("test")

        return ForgotPasswordResponse(
                message = "Password reset link generated. Check your email.",
                resetToken = if (sendToken) tokenValue else null
        )
    }

    @Transactional
    fun resetPassword(token: String, request: ResetPasswordRequest) {
        val resetToken =
                passwordResetTokenRepository.findValidByToken(token).orElseThrow {
                    IllegalArgumentException("Invalid or expired reset token")
                }

        if (!resetToken.isValid()) {
            throw IllegalArgumentException("Invalid or expired reset token")
        }

        val user =
                userRepository.findById(resetToken.userId).orElseThrow {
                    IllegalArgumentException("User not found")
                }

        // Update password
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)

        // Mark token as used
        resetToken.markAsUsed()
        passwordResetTokenRepository.save(resetToken)
    }

    fun validateResetToken(token: String): Boolean {
        val resetToken = passwordResetTokenRepository.findByToken(token).orElse(null)
        return resetToken?.isValid() ?: false
    }
}
