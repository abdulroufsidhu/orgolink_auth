package com.orgolink.auth.repository

import com.orgolink.auth.domain.entity.Permission
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PermissionRepository : JpaRepository<Permission, Long> {
    fun findByName(name: String): Optional<Permission>
    fun existsByName(name: String): Boolean
}
