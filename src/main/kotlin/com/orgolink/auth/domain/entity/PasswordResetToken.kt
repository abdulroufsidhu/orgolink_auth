package com.orgolink.auth.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "password_reset_tokens")
data class PasswordResetToken(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
        @Column(name = "user_id", nullable = false) val userId: Long,
        @Column(nullable = false, unique = true) val token: String,
        @Column(name = "expires_at", nullable = false) val expiresAt: LocalDateTime,
        @Column(name = "created_at", nullable = false, updatable = false)
        val createdAt: LocalDateTime = LocalDateTime.now(),
        @Column(name = "used_at") var usedAt: LocalDateTime? = null,
        @Column(name = "is_used", nullable = false) var isUsed: Boolean = false,
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "user_id", insertable = false, updatable = false)
        val user: User? = null
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isValid(): Boolean = !isUsed && !isExpired()

    fun markAsUsed() {
        isUsed = true
        usedAt = LocalDateTime.now()
    }
}
