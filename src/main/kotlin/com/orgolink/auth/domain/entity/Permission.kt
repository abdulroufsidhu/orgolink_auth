package com.orgolink.auth.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "permissions")
data class Permission(
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) val id: Long? = null,
        @Column(nullable = false, unique = true, length = 50) val name: String,
        @Column(length = 255) val description: String? = null,
        @Column(name = "created_at", nullable = false, updatable = false)
        val createdAt: LocalDateTime = LocalDateTime.now()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Permission) return false
        return name == other.name
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return "Permission(id=$id, name='$name')"
    }
}
