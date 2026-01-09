package com.orgolink.auth.repository

import com.orgolink.auth.domain.entity.PasswordResetToken
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, Long> {

    fun findByToken(token: String): Optional<PasswordResetToken>

    @Query("SELECT p FROM PasswordResetToken p WHERE p.token = :token AND p.isUsed = false")
    fun findValidByToken(token: String): Optional<PasswordResetToken>

    @Query(
            "SELECT p FROM PasswordResetToken p WHERE p.userId = :userId AND p.isUsed = false ORDER BY p.createdAt DESC"
    )
    fun findActiveTokensByUserId(userId: Long): List<PasswordResetToken>
}
