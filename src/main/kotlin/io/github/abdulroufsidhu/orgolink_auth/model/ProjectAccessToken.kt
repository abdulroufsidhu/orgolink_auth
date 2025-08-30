package io.github.abdulroufsidhu.orgolink_auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "project_access_tokens")
class ProjectAccessToken(
    @Column(nullable = false, unique = true, length = 500) 
    var token: String? = null,
    
    @Column(nullable = false) 
    var expiresAt: Date? = null,
    
    @Column(nullable = false) 
    var isRevoked: Boolean = false,
    
    @Column(name = "project_id", nullable = false) 
    var projectId: UUID? = null,
    
    @Column(name = "user_id", nullable = false) 
    var userId: UUID? = null,
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: ProjectRole = ProjectRole.USER,
    
    @Column(length = 255) 
    var description: String? = null,
) : BaseTable()
