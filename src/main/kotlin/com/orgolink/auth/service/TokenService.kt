package com.orgolink.auth.service

import com.orgolink.auth.repository.TokenRepository
import com.orgolink.auth.security.JwtUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TokenService(
        private val tokenRepository: TokenRepository,
        private val tokenCacheService: TokenCacheService,
        private val jwtUtil: JwtUtil
) {

    fun verifyToken(tokenValue: String): Boolean {
        // First check if token is valid JWT
        if (!jwtUtil.validateToken(tokenValue)) {
            return false
        }

        // Check if token is expired
        if (jwtUtil.isTokenExpired(tokenValue)) {
            return false
        }

        // Check Redis cache first (fast path)
        if (tokenCacheService.isTokenInCache(tokenValue)) {
            return true
        }

        // Fall back to database check
        val token = tokenRepository.findByTokenValue(tokenValue).orElse(null) ?: return false

        return token.isValid()
    }

    @Transactional
    fun invalidateToken(tokenValue: String) {
        // Remove from cache
        tokenCacheService.invalidateToken(tokenValue)

        // Revoke in database
        tokenRepository.findByTokenValue(tokenValue).ifPresent { token ->
            token.revoke()
            tokenRepository.save(token)
        }
    }
}
