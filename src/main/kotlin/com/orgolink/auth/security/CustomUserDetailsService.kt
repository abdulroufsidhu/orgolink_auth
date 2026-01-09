package com.orgolink.auth.security

import com.orgolink.auth.repository.UserRepository
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {

    override fun loadUserByUsername(username: String): UserDetails {
        val user =
                userRepository.findActiveByUsername(username).orElseThrow {
                    UsernameNotFoundException("User not found: $username")
                }

        val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_${it.name}") }

        return User(
                user.username,
                user.passwordHash,
                user.isActive(),
                true, // accountNonExpired
                true, // credentialsNonExpired
                !user.isDeleted, // accountNonLocked
                authorities
        )
    }
}
