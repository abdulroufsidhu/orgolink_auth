package com.orgolink.auth.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "tokens")
data class Token(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
        @Column(name = "user_id", nullable = false) val userId: Long,
        @Column(name = "token_value", nullable = false, unique = true, columnDefinition = "TEXT")
        val tokenValue: String,
        @Column(name = "token_type", nullable = false, length = 20) val tokenType: String = "JWT",
        @Column(name = "expires_at", nullable = false) val expiresAt: LocalDateTime,
        @Column(name = "created_at", nullable = false, updatable = false)
        val createdAt: LocalDateTime = LocalDateTime.now(),
        @Column(name = "revoked_at") var revokedAt: LocalDateTime? = null,
        @Column(name = "is_revoked", nullable = false) var isRevoked: Boolean = false,
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", insertable = false, updatable = false)
        val user: User? = null
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isValid(): Boolean = !isRevoked && !isExpired()

    fun revoke() {
        isRevoked = true
        revokedAt = LocalDateTime.now()
    }
}
