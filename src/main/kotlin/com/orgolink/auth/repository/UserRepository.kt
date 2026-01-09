package com.orgolink.auth.repository

import com.orgolink.auth.domain.entity.User
import java.util.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByUsernameAndIsDeleted(username: String, isDeleted: Boolean = false): Optional<User>

    fun findByEmailAndIsDeleted(email: String, isDeleted: Boolean = false): Optional<User>

    fun findByIdAndIsDeleted(id: Long, isDeleted: Boolean = false): Optional<User>

    @Query("SELECT u FROM User u WHERE u.isDeleted = false") fun findAllActive(): List<User>

    fun existsByUsernameAndIsDeleted(username: String, isDeleted: Boolean = false): Boolean

    fun existsByEmailAndIsDeleted(email: String, isDeleted: Boolean = false): Boolean
    fun findActiveByUsername(username: String): Optional<User>
    fun existsByUsername(username: String): Boolean
    fun existsByEmail(email: String): Boolean
    fun findActiveByEmail(email: String) : Optional<User>
    fun findActiveById(userId: Long) : Optional<User>
}
