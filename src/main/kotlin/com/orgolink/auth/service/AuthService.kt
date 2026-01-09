package com.orgolink.auth.service

import com.orgolink.auth.config.AuthProperties
import com.orgolink.auth.domain.dto.LoginRequest
import com.orgolink.auth.domain.dto.LoginResponse
import com.orgolink.auth.domain.dto.RegisterRequest
import com.orgolink.auth.domain.dto.RegisterResponse
import com.orgolink.auth.domain.dto.UserResponse
import com.orgolink.auth.domain.entity.Token
import com.orgolink.auth.domain.entity.User
import com.orgolink.auth.repository.RoleRepository
import com.orgolink.auth.repository.TokenRepository
import com.orgolink.auth.repository.UserRepository
import com.orgolink.auth.security.JwtUtil
import java.time.LocalDateTime
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
        private val userRepository: UserRepository,
        private val roleRepository: RoleRepository,
        private val tokenRepository: TokenRepository,
        private val passwordEncoder: PasswordEncoder,
        private val jwtUtil: JwtUtil,
        private val tokenCacheService: TokenCacheService,
        private val authProperties: AuthProperties
) {

    @Transactional
    fun register(request: RegisterRequest): RegisterResponse {
        // Check if user already exists
        if (userRepository.existsByUsername(request.username)) {
            throw IllegalArgumentException("Username already exists")
        }

        if (userRepository.existsByEmail(request.email)) {
            throw IllegalArgumentException("Email already exists")
        }

        // Create new user
        val user =
                User(
                        username = request.username,
                        email = request.email,
                        passwordHash = passwordEncoder.encode(request.password),
                        createdBy = request.createdBy
                )

        // Assign default USER role
        val userRole =
                roleRepository.findByName("USER").orElseThrow {
                    IllegalStateException("Default USER role not found")
                }
        user.roles.add(userRole)

        val savedUser = userRepository.save(user)

        // Generate token
        val permissions = getPermissionsByRoles(savedUser.roles.map { it.name })
        val token =
                jwtUtil.generateToken(
                        savedUser.username,
                        savedUser.id!!,
                        savedUser.roles.map { it.name },
                        permissions
                )

        // Store token in database and cache
        saveAndCacheToken(savedUser.id, token)

        return RegisterResponse(token = token, user = UserResponse.from(savedUser))
    }

    @Transactional
    fun login(request: LoginRequest): LoginResponse {
        val user =
                userRepository.findActiveByUsername(request.username).orElseThrow {
                    IllegalArgumentException("Invalid username or password")
                }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw IllegalArgumentException("Invalid username or password")
        }

        // Generate token
        val permissions = getPermissionsByRoles(user.roles.map { it.name })
        val token =
                jwtUtil.generateToken(
                        user.username,
                        user.id!!,
                        user.roles.map { it.name },
                        permissions
                )

        // Store token in database and cache
        saveAndCacheToken(user.id, token)

        return LoginResponse(token = token, user = UserResponse.from(user))
    }

    private fun saveAndCacheToken(userId: Long, tokenValue: String) {
        // Save to database
        val expirationDate = LocalDateTime.now().plusSeconds(authProperties.jwt.expirationMs / 1000)
        val token = Token(userId = userId, tokenValue = tokenValue, expiresAt = expirationDate)
        tokenRepository.save(token)

        // Cache in Redis
        val username = jwtUtil.getUsernameFromToken(tokenValue) ?: ""
        tokenCacheService.cacheToken(tokenValue, username)
    }

    private fun getPermissionsByRoles(roles: List<String>): List<String> {
        return authProperties
                .roles
                .filter { it.name in roles }
                .flatMap { it.permissions }
                .distinct()
    }
}
