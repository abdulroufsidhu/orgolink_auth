package com.orgolink.auth.security

import com.orgolink.auth.config.AuthProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.security.Keys
import java.util.*
import javax.crypto.SecretKey
import org.springframework.stereotype.Component

@Component
class JwtUtil(private val authProperties: AuthProperties) {

    private val secretKey: SecretKey by lazy {
        Keys.hmacShaKeyFor(authProperties.jwt.secret.toByteArray())
    }

    fun generateToken(
            username: String,
            userId: Long,
            roles: List<String>,
            permissions: List<String>
    ): String {
        val now = Date()
        val expiration = Date(now.time + authProperties.jwt.expirationMs)

        return Jwts.builder()
                .setSubject(username)
                .claim("userId", userId)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .setIssuedAt(now)
                .setExpiration(expiration)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact()
    }

    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parser().verifyWith(secretKey).build().parseClaimsJws(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getUsernameFromToken(token: String): String? {
        return try {
            val claims = getClaims(token)
            claims.subject
        } catch (e: Exception) {
            null
        }
    }

    fun getUserIdFromToken(token: String): Long? {
        return try {
            val claims = getClaims(token)
            claims["userId"].toString().toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }

    fun getRolesFromToken(token: String): List<String> {
        return try {
            val claims = getClaims(token)
            @Suppress("UNCHECKED_CAST") (claims["roles"] as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getPermissionsFromToken(token: String): List<String> {
        return try {
            val claims = getClaims(token)
            @Suppress("UNCHECKED_CAST") (claims["permissions"] as? List<String>) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getExpirationFromToken(token: String): Date? {
        return try {
            val claims = getClaims(token)
            claims.expiration
        } catch (e: Exception) {
            null
        }
    }

    fun isTokenExpired(token: String): Boolean {
        val expiration = getExpirationFromToken(token) ?: return true
        return expiration.before(Date())
    }

    private fun getClaims(token: String): Claims {
        return Jwts.parser().verifyWith(secretKey).build().parseClaimsJws(token).body
    }
}
