package io.github.abdulroufsidhu.orgolink_auth.services

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.LoginOrCreateUserRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.exceptions.UsernameAlreadyExists
import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.repo.UserRepo
import io.jsonwebtoken.ExpiredJwtException
import jakarta.servlet.http.HttpServletRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class UserService(
    private val userRep: UserRepo,
    private val passwordEncoder: BCryptPasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val tokenService: TokenService,
    private val userDetailsService: OrgoUserDetailsService,
) {

    suspend fun findById(id: UUID?) = id?.let { userRep.findByIdOrNull(it) }

    suspend fun delete(
        request: HttpServletRequest,
        userDetails: OrgoUserPrincipal?
    ): ResponseEntity<out ValidResponseData<Nothing>?> {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ValidResponseData(
                    message = "Invalid Token",
                    data = null
                ),
            )

        val authHeader = request.getHeader("Authorization")
        val jwt = authHeader?.substring(7)

        val response: ResponseEntity<ValidResponseData<Nothing>> = run {
            val isValid = withContext(Dispatchers.IO) { tokenService.isTokenValid(jwt!!, userDetails) }
            if (isValid) {
                userDetails.id?.let { userRep.deleteById(it) }
                ResponseEntity.ok().body(
                    ValidResponseData(
                        message = "User deleted successfully",
                        data = null
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ValidResponseData(
                        message = "Invalid Token",
                        data = null
                    )
                )
            }
        }


        return response;
    }

    suspend fun createUser(user: LoginOrCreateUserRequestDTO): ResponseEntity<ValidResponseData<String>> =
        withContext(Dispatchers.IO) {
            if (userRep.existsByUsername(user.username))
                throw UsernameAlreadyExists("Username already exists")
            val incommingPassword = user.password
            val savedUser =
                userRep.saveAndFlush(
                    user.asOrgoOrgoUser().apply { password = passwordEncoder.encode(password) }
                )
            login(LoginOrCreateUserRequestDTO(user.username, incommingPassword))
        }

    suspend fun login(requeestDto: LoginOrCreateUserRequestDTO): ResponseEntity<ValidResponseData<String>> {
        val authentication: Authentication =
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(requeestDto.username, requeestDto.password)
            )

        val userDetails = userDetailsService.loadUserByUsername(requeestDto.username)
        val token = tokenService.generateToken(userDetails)

        return ResponseEntity.ok(ValidResponseData(message = "Login successful", data = token))
    }

    suspend fun logout(request: HttpServletRequest): ResponseEntity<ValidResponseData<Nothing>> =
        withContext(Dispatchers.IO) {
            val authHeader = request.getHeader("Authorization")
            val jwt = authHeader?.substring(7)

            jwt?.let {
                val username = tokenService.extractUsername(it)
                val userDetails = userDetailsService.loadUserByUsername(username)
                (userDetails as? OrgoUserPrincipal)?.id?.let { userId: UUID ->
                    tokenService.revokeAllUserTokens(userId)
                }
            }

            ResponseEntity.ok(
                ValidResponseData(
                    message = "Logged out successfully",
                    data = null
                )
            )
        }

    @Throws(ExpiredJwtException::class)
    suspend fun verify(
        request: HttpServletRequest,
        userDetails: UserDetails?
    ): ResponseEntity<ValidResponseData<Nothing>> {
        if (userDetails == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                ValidResponseData(
                    message = "Invalid Token",
                    data = null
                ),
            )

        val authHeader = request.getHeader("Authorization")
        val jwt = authHeader?.substring(7)

        val response: ResponseEntity<ValidResponseData<Nothing>> = run {
            val isValid = withContext(Dispatchers.IO) {
                tokenService.isTokenValid(jwt!!, userDetails)
            }
            if (isValid) {
                ResponseEntity.ok().body(
                    ValidResponseData(
                        message = "Token is valid",
                        data = null
                    )
                )
            } else {
                ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ValidResponseData(
                        message = "Invalid Token",
                        data = null
                    )
                )
            }
        }
        return response;
    }
}
