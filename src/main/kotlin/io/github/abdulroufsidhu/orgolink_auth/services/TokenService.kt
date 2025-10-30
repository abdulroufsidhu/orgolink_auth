package io.github.abdulroufsidhu.orgolink_auth.services

import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.model.UserAccessToken
import io.github.abdulroufsidhu.orgolink_auth.repo.TokenRepo
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.lang3.time.DateUtils
import org.hibernate.type.descriptor.DateTimeUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.token.Sha512DigestUtils
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCrypt
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.encrypt.TextEncryptor
import org.springframework.stereotype.Service
import java.security.Key
import java.util.Calendar
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.log

@Service
class TokenService(
    private val tokenRepo: TokenRepo,
    @Value("\${jwt.secret}") private val secretKey: String,
    @Value("\${jwt.expiration}") private val jwtExpiration: Long,
) {

    private val textHasher = BCryptPasswordEncoder(12)

    suspend fun generateToken(userDetails: UserDetails): String = withContext(Dispatchers.Default) {
        val now = Date()
        val expirationDate = Date(now.time + jwtExpiration)

        val token =
            Jwts.builder()
                .setSubject(userDetails.username)
                .setIssuedAt(now)
                .setExpiration(expirationDate)
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact()

        if (userDetails is OrgoUserPrincipal) {
            withContext(Dispatchers.IO) {
                userDetails.id?.let { userId ->
                    tokenRepo.save(
                        UserAccessToken(
                            token = Sha512DigestUtils.shaHex(token),
                            expiresAt = expirationDate,
                            userId = userId,
                        )
                    )
                }
            }
        }

        return@withContext token
    }

    suspend fun isTokenValid(token: String, userDetails: UserDetails): Boolean = withContext(Dispatchers.IO) {
        val t = Sha512DigestUtils.shaHex(token)
        println("Encrypted Token: $t");
        val username = extractUsername(token)
        return@withContext username == userDetails.username &&
                !isTokenExpired(token) &&
                tokenRepo.existsByTokenAndIsRevokedFalse(t)
    }

    suspend fun extractUsername(token: String): String = withContext(Dispatchers.IO) {
        return@withContext extractClaim(token) { obj: Claims -> obj.subject }
    }

    suspend fun revokeAllUserTokens(userId: UUID) = withContext(Dispatchers.IO) {
        val validUserTokens = tokenRepo.findByUserIdAndIsRevokedFalse(userId)
        validUserTokens.forEach { token ->
            token.isRevoked = true
            tokenRepo.save(token)
        }
    }

    private suspend fun isTokenExpired(token: String): Boolean = withContext(Dispatchers.Default) {
        extractExpiration(token).before(Date())
    }

    private suspend fun extractExpiration(token: String): Date = withContext(Dispatchers.Default) {
        extractClaim(token) { obj: Claims -> obj.expiration }
    }

    private suspend fun <T> extractClaim(token: String, claimsResolver: (Claims) -> T): T =
        withContext(Dispatchers.Default) {
            val claims = extractAllClaims(token)
            claimsResolver(claims)
        }

    private suspend fun extractAllClaims(token: String): Claims = withContext(Dispatchers.Default) {
        Jwts.parserBuilder().setSigningKey(getSignInKey()).build().parseClaimsJws(token).body
    }

    private suspend fun getSignInKey(): Key = withContext(Dispatchers.Default) {
        val keyBytes = Decoders.BASE64.decode(secretKey)
        Keys.hmacShaKeyFor(keyBytes)
    }
}
