package com.orgolink.auth.repository

import com.orgolink.auth.domain.entity.Token
import java.time.LocalDateTime
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface TokenRepository : JpaRepository<Token, Long> {

    fun findByTokenValue(tokenValue: String): Optional<Token>

    @Query(
            "SELECT t FROM Token t WHERE t.tokenValue = :tokenValue AND t.isRevoked = false AND t.expiresAt > :now"
    )
    fun findValidToken(
            tokenValue: String,
            now: LocalDateTime = LocalDateTime.now()
    ): Optional<Token>

    @Query(
            "SELECT t FROM Token t WHERE t.userId = :userId AND t.isRevoked = false AND t.expiresAt > :now"
    )
    fun findValidTokensByUserId(userId: Long, now: LocalDateTime = LocalDateTime.now()): List<Token>

    @Modifying
    @Query(
            "UPDATE Token t SET t.isRevoked = true, t.revokedAt = :now WHERE t.userId = :userId AND t.isRevoked = false"
    )
    fun revokeAllUserTokens(userId: Long, now: LocalDateTime = LocalDateTime.now())

    @Modifying
    @Query("DELETE FROM Token t WHERE t.expiresAt < :threshold")
    fun deleteExpiredTokens(threshold: LocalDateTime = LocalDateTime.now().minusDays(7))
}
