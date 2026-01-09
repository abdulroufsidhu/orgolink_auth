package com.orgolink.auth.controller

import com.orgolink.auth.service.AuthorizationService
import com.orgolink.auth.service.TokenService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api")
class VerificationController(
        private val tokenService: TokenService,
        private val authorizationService: AuthorizationService
) {

    @GetMapping("/verify")
    fun verifyToken(@RequestHeader("Authorization") authHeader: String): ResponseEntity<Void> {
        val token = extractToken(authHeader)

        return if (token != null && tokenService.verifyToken(token)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }
    }

    @GetMapping("/verify_role")
    fun verifyRole(
            @RequestHeader("Authorization") authHeader: String,
            @RequestParam("role") role: String
    ): ResponseEntity<Void> {
        val token = extractToken(authHeader)

        return if (token != null && authorizationService.verifyRole(token, role)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
        }
    }

    @GetMapping("/verify_permission")
    fun verifyPermission(
            @RequestHeader("Authorization") authHeader: String,
            @RequestParam("permission") permission: String
    ): ResponseEntity<Void> {
        val token = extractToken(authHeader)

        return if (token != null && authorizationService.verifyPermission(token, permission)) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.status(HttpStatus.FORBIDDEN).build()
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
