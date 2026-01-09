package com.orgolink.auth.controller

import com.orgolink.auth.domain.dto.*
import com.orgolink.auth.service.AuthService
import com.orgolink.auth.service.PasswordResetService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class AuthController(
        private val authService: AuthService,
        private val passwordResetService: PasswordResetService
) {

    @PostMapping("/register")
    fun register(
            @RequestBody request: RegisterRequest
    ): ResponseEntity<ApiResponse<RegisterResponse>> {
        return try {
            val response = authService.register(request)
            ResponseEntity.ok(ApiResponse.success("User registered successfully", response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Registration failed"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"))
        }
    }

    @PostMapping("/login")
    fun login(@RequestBody request: LoginRequest): ResponseEntity<ApiResponse<LoginResponse>> {
        return try {
            val response = authService.login(request)
            ResponseEntity.ok(ApiResponse.success("Login successful", response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Login failed"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"))
        }
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(
            @RequestBody request: ForgotPasswordRequest
    ): ResponseEntity<ApiResponse<ForgotPasswordResponse>> {
        return try {
            val response = passwordResetService.initiatePasswordReset(request)
            ResponseEntity.ok(ApiResponse.success(response.message, response))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.message ?: "Failed to initiate password reset"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"))
        }
    }

    @PostMapping("/reset-password/{token}")
    fun resetPassword(
            @PathVariable token: String,
            @RequestBody request: ResetPasswordRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            passwordResetService.resetPassword(token, request)
            ResponseEntity.ok(ApiResponse.success("Password reset successful"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.message ?: "Password reset failed"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"))
        }
    }
}
