package com.orgolink.auth.repository

import com.orgolink.auth.domain.entity.Role
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface RoleRepository : JpaRepository<Role, Long> {
    fun findByName(name: String): Optional<Role>
    fun existsByName(name: String): Boolean
}
