package com.orgolink.auth.controller

import com.orgolink.auth.domain.dto.ApiResponse
import com.orgolink.auth.domain.dto.UserResponse
import com.orgolink.auth.service.UserService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class UserController(private val userService: UserService) {

    @GetMapping("/user")
    fun getAuthenticatedUser(
            @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ApiResponse<UserResponse>> {
        return try {
            val token =
                    extractToken(authHeader)
                            ?: return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Invalid authorization header"))

            val user = userService.getAuthenticatedUser(token)
            ResponseEntity.ok(ApiResponse.success("User retrieved successfully", user))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(ApiResponse.error(e.message ?: "Failed to get user"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"))
        }
    }

    @DeleteMapping("/delete")
    fun deleteOwnAccount(
            @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            val token =
                    extractToken(authHeader)
                            ?: return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Invalid authorization header"))

            userService.deleteOwnAccount(token)
            ResponseEntity.ok(ApiResponse.success("Account deleted successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.message ?: "Failed to delete account"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"))
        }
    }

    @DeleteMapping("/delete/{uid}")
    fun deleteUserByUid(
            @PathVariable uid: Long,
            @RequestHeader("Authorization") authHeader: String
    ): ResponseEntity<ApiResponse<Nothing>> {
        return try {
            val token =
                    extractToken(authHeader)
                            ?: return ResponseEntity.badRequest()
                                    .body(ApiResponse.error("Invalid authorization header"))

            userService.deleteUserByUid(uid, token)
            ResponseEntity.ok(ApiResponse.success("User deleted successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.message ?: "Failed to delete user"))
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body(ApiResponse.error("Internal server error"))
        }
    }

    private fun extractToken(authHeader: String): String? {
        return if (authHeader.startsWith("Bearer ")) {
            authHeader.substring(7)
        } else {
            null
        }
    }
}
