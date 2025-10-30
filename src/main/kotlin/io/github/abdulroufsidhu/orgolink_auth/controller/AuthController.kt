package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.LoginOrCreateUserRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.services.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController("/api")
class AuthController(private val userService: UserService) {

    @GetMapping("/")
    suspend fun securedThankYou(request: HttpServletRequest): ResponseEntity<String> {
        println("inside secured request")
        return ResponseEntity.ok(request.session.id)
    }

    @PostMapping("/auth/register")
    @Operation(summary = "Register new user")
    suspend fun register(@Valid @RequestBody user: LoginOrCreateUserRequestDTO) = userService.createUser(user)

    @PostMapping("/auth/login")
    @Operation(summary = "Login to get JWT token")
    suspend fun login(
        @Valid @RequestBody requeestDto: LoginOrCreateUserRequestDTO
    ): ResponseEntity<ValidResponseData<String>> = userService.login(requeestDto)

    @PostMapping("/auth/logout")
    @Operation(
        summary = "Logout and revoke token",
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    suspend fun logout(request: HttpServletRequest): ResponseEntity<ValidResponseData<Nothing>> =
        userService.logout(request)

    @GetMapping("/auth/verify")
    suspend fun verify(
        request: HttpServletRequest,
        @AuthenticationPrincipal userDetails: OrgoUserPrincipal?
    ): ResponseEntity<ValidResponseData<Nothing>> = userService.verify(request, userDetails)

}

@Profile("test")
@RestController
class TestAuthController(private val userService: UserService) {

    @DeleteMapping("/auth/delete")
    suspend fun delete(
        request: HttpServletRequest,
        @AuthenticationPrincipal userDetails: OrgoUserPrincipal?
    ) = userService.delete(request, userDetails)
}
