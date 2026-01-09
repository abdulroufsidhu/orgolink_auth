package com.orgolink.auth.domain.entity

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,
    
    @Column(nullable = false, unique = true, length = 50)
    var username: String,
    
    @Column(nullable = false, unique = true, length = 100)
    var email: String,
    
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,
    
    // Audit fields
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
    
    @Column(name = "created_by")
    var createdBy: Long? = null,
    
    @Column(name = "updated_by")
    var updatedBy: Long? = null,
    
    // Soft delete support
    @Column(name = "deleted_at")
    var deletedAt: LocalDateTime? = null,
    
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,
    
    // Relationships
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    var roles: MutableSet<Role> = mutableSetOf()
) {
    @PreUpdate
    fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
    
    fun softDelete(deletedBy: Long? = null) {
        isDeleted = true
        deletedAt = LocalDateTime.now()
        updatedBy = deletedBy
    }
    
    fun isActive(): Boolean = !isDeleted
}
