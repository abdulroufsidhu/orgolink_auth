package io.github.abdulroufsidhu.orgolink_auth.filters

import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.model.ProjectRole
import io.github.abdulroufsidhu.orgolink_auth.repo.UserRepo
import io.github.abdulroufsidhu.orgolink_auth.services.OrgoUserDetailsService
import io.github.abdulroufsidhu.orgolink_auth.services.ProjectTokenService
import io.github.abdulroufsidhu.orgolink_auth.services.UserService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.runBlocking
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

@Component
class ProjectTokenAuthenticationFilter(
    private val projectTokenService: ProjectTokenService,
    private val userDetailsService: OrgoUserDetailsService,
    private val userRepo: UserRepo,
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) = runBlocking {
        val authHeader = request.getHeader("Authorization")

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return@runBlocking
        }

        val token = authHeader.substring(7)

        // Check if this is a project access token
        val projectToken = projectTokenService.validateProjectToken(token)

        if (projectToken != null && SecurityContextHolder.getContext().authentication == null) {
            try {
                val user = projectToken.userId?.let { userRepo.findById(it).orElse(null) }
                val userDetails = userDetailsService.loadUserByUsername(user?.username)

                if (userDetails is OrgoUserPrincipal) {
                    // Create authorities based on project role
                    val authorities = mutableListOf<SimpleGrantedAuthority>()
                    authorities.add(SimpleGrantedAuthority("ROLE_USER"))
                    authorities.add(SimpleGrantedAuthority("ROLE_PROJECT_${projectToken.role.name}"))
                    authorities.add(SimpleGrantedAuthority("PROJECT_${projectToken.projectId}"))

                    // Create enhanced user principal with project context
                    val enhancedUserPrincipal =
                        projectToken.projectId?.let { ProjectUserPrincipal(userDetails, it, projectToken.role) }

                    val authToken =
                        UsernamePasswordAuthenticationToken(
                            enhancedUserPrincipal,
                            null,
                            authorities
                        )
                    authToken.details = WebAuthenticationDetailsSource().buildDetails(request)
                    SecurityContextHolder.getContext().authentication = authToken
                }
            } catch (e: Exception) {
                // Log error but don't block the filter chain
                logger.error("Error processing project token: ${e.message}")
            }
        }

        filterChain.doFilter(request, response)
    }
}

/** Enhanced user principal that includes project context */
class ProjectUserPrincipal(
    private val userPrincipal: OrgoUserPrincipal,
    val projectId: UUID,
    val projectRole: ProjectRole
) : OrgoUserPrincipal(userPrincipal.getUser()) {

    fun getOrgoUser(): io.github.abdulroufsidhu.orgolink_auth.model.OrgoUser {
        return userPrincipal.getUser()
    }

    override fun getAuthorities():
            MutableCollection<out org.springframework.security.core.GrantedAuthority> {
        val authorities = mutableListOf<SimpleGrantedAuthority>()
        authorities.add(SimpleGrantedAuthority("ROLE_USER"))
        authorities.add(SimpleGrantedAuthority("ROLE_PROJECT_${projectRole.name}"))
        authorities.add(SimpleGrantedAuthority("PROJECT_$projectId"))
        return authorities
    }
}
