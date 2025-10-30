package io.github.abdulroufsidhu.orgolink_auth.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.Date
import java.util.UUID

@Entity
@Table(name = "user_access_tokens")
class UserAccessToken(
    @Column(nullable = false, unique = true, length = 128)
    var token: String? = null,

    @Column(nullable = false)
    var expiresAt: Date? = null,

    @Column(nullable = false)
    var isRevoked: Boolean = false,

    @Column(nullable = false, name = "user_id")
    var userId: UUID,
) : BaseTable()
