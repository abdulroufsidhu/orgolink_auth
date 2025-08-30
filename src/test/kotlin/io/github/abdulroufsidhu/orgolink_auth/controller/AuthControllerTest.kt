package io.github.abdulroufsidhu.orgolink_auth.controller

import io.github.abdulroufsidhu.orgolink_auth.dto.ValidResponseData
import io.github.abdulroufsidhu.orgolink_auth.dto.requestdto.LoginOrCreateUserRequestDTO
import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUser
import io.github.abdulroufsidhu.orgolink_auth.model.OrgoUserPrincipal
import io.github.abdulroufsidhu.orgolink_auth.services.UserService
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpSession
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import java.util.*

@ExtendWith(MockitoExtension::class)
class AuthControllerTest {

    @Mock
    private lateinit var userService: UserService

    @Mock
    private lateinit var httpServletRequest: HttpServletRequest

    @Mock
    private lateinit var httpSession: HttpSession

    @Mock
    private lateinit var orgoUserPrincipal: OrgoUserPrincipal

    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp() {
        authController = AuthController(userService)
    }

    // Test for securedThankYou endpoint
    @Test
    fun `securedThankYou should return session id`() {
        val sessionId = "test-session-id"
        `when`(httpServletRequest.session).thenReturn(httpSession)
        `when`(httpSession.id).thenReturn(sessionId)

        val result = authController.securedThankYou(httpServletRequest)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals(sessionId, result.body)
        verify(httpServletRequest).session
        verify(httpSession).id
    }

    // Test for register endpoint - Success case
    @Test
    fun `register should return success response when valid user data provided`() {
        val requestDto = LoginOrCreateUserRequestDTO(
            username = "testuser",
            password = "Password123!"
        )
        val responseData = ValidResponseData(
            message = "User registered successfully",
            data = "jwt-token-here"
        )
        val expectedResponse = ResponseEntity.ok(responseData)

        `when`(userService.createUser(requestDto)).thenReturn(expectedResponse)

        val result = authController.register(requestDto)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("User registered successfully", result.body?.message)
        assertEquals("jwt-token-here", result.body?.data)
        verify(userService).createUser(requestDto)
    }

    // Test for register endpoint - Exception handling
    @Test
    fun `register should handle service exceptions`() {
        val requestDto = LoginOrCreateUserRequestDTO(
            username = "testuser",
            password = "Password123!"
        )

        `when`(userService.createUser(requestDto))
            .thenThrow(RuntimeException("Username already exists"))

        assertThrows(RuntimeException::class.java) {
            authController.register(requestDto)
        }

        verify(userService).createUser(requestDto)
    }


    // Test for login endpoint - Success case
    @Test
    fun `login should return JWT token when valid credentials provided`() {
        val requestDto = LoginOrCreateUserRequestDTO(
            username = "testuser",
            password = "Password123!"
        )
        val responseData = ValidResponseData(
            message = "Login successful",
            data = "jwt-token-here"
        )
        val expectedResponse = ResponseEntity.ok(responseData)

        `when`(userService.login(requestDto)).thenReturn(expectedResponse)

        val result = authController.login(requestDto)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Login successful", result.body?.message)
        assertEquals("jwt-token-here", result.body?.data)
        verify(userService).login(requestDto)
    }

    // Test for login endpoint - Invalid credentials
    @Test
    fun `login should handle authentication failure`() {
        val requestDto = LoginOrCreateUserRequestDTO(
            username = "testuser",
            password = "wrongpassword"
        )

        `when`(userService.login(requestDto))
            .thenThrow(RuntimeException("Invalid credentials"))

        assertThrows(RuntimeException::class.java) {
            authController.login(requestDto)
        }

        verify(userService).login(requestDto)
    }


    // Test for logout endpoint - Success case
    @Test
    fun `logout should return success response when valid token provided`() {
        val responseData = ValidResponseData<Nothing>(
            message = "Logged out successfully",
            data = null
        )
        val expectedResponse = ResponseEntity.ok(responseData)

        `when`(userService.logout(httpServletRequest))
            .thenReturn(expectedResponse)

        val result = authController.logout(httpServletRequest)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Logged out successfully", result.body?.message)
        assertNull(result.body?.data)
        verify(userService).logout(httpServletRequest)
    }

    // Test for logout endpoint - Exception handling
    @Test
    fun `logout should handle service exceptions`() {
        `when`(userService.logout(httpServletRequest))
            .thenThrow(RuntimeException("Logout failed"))

        assertThrows(RuntimeException::class.java) {
            authController.logout(httpServletRequest)
        }

        verify(userService).logout(httpServletRequest)
    }

    // Test for verify endpoint - Valid token
    @Test
    fun `verify should return success when valid token provided`() {
        val responseData = ValidResponseData<Nothing>(
            message = "Token is valid",
            data = null
        )
        val expectedResponse = ResponseEntity.ok(responseData)

        `when`(userService.verify(httpServletRequest, orgoUserPrincipal))
            .thenReturn(expectedResponse)

        val result = authController.verify(httpServletRequest, orgoUserPrincipal)

        assertEquals(HttpStatus.OK, result.statusCode)
        assertEquals("Token is valid", result.body?.message)
        assertNull(result.body?.data)
        verify(userService).verify(httpServletRequest, orgoUserPrincipal)
    }

    // Test for verify endpoint - Invalid token
    @Test
    fun `verify should return unauthorized when invalid token provided`() {
        val responseData = ValidResponseData<Nothing>(
            message = "Invalid Token",
            data = null
        )
        val expectedResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseData)

        `when`(userService.verify(httpServletRequest, orgoUserPrincipal))
            .thenReturn(expectedResponse)

        val result = authController.verify(httpServletRequest, orgoUserPrincipal)

        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
        assertEquals("Invalid Token", result.body?.message)
        assertNull(result.body?.data)
        verify(userService).verify(httpServletRequest, orgoUserPrincipal)
    }

    // Test for verify endpoint - No authentication principal
    @Test
    fun `verify should return unauthorized when no user principal provided`() {
        val responseData = ValidResponseData<Nothing>(
            message = "Invalid Token",
            data = null
        )
        val expectedResponse = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(responseData)

        `when`(userService.verify(httpServletRequest, null))
            .thenReturn(expectedResponse)

        val result = authController.verify(httpServletRequest, null)

        assertEquals(HttpStatus.UNAUTHORIZED, result.statusCode)
        assertEquals("Invalid Token", result.body?.message)
        verify(userService).verify(httpServletRequest, null)
    }

    // Test for verify endpoint - Exception handling
    @Test
    fun `verify should handle service exceptions`() {
        `when`(userService.verify(httpServletRequest, orgoUserPrincipal))
            .thenThrow(RuntimeException("Token expired"))

        assertThrows(RuntimeException::class.java) {
            authController.verify(httpServletRequest, orgoUserPrincipal)
        }

        verify(userService).verify(httpServletRequest, orgoUserPrincipal)
    }

    // Integration test for register -> login flow
    @Test
    fun `register and login flow should work correctly`() {
        val registerDto = LoginOrCreateUserRequestDTO(
            username = "newuser",
            password = "Password123!"
        )
        val loginDto = LoginOrCreateUserRequestDTO(
            username = "newuser",
            password = "Password123!"
        )

        val registerResponse = ValidResponseData(
            message = "User registered successfully",
            data = "jwt-token-register"
        )
        val loginResponse = ValidResponseData(
            message = "Login successful",
            data = "jwt-token-login"
        )

        `when`(userService.createUser(registerDto))
            .thenReturn(ResponseEntity.ok(registerResponse))
        `when`(userService.login(loginDto))
            .thenReturn(ResponseEntity.ok(loginResponse))

        // Test register
        val registerResult = authController.register(registerDto)
        assertEquals(HttpStatus.OK, registerResult.statusCode)
        assertEquals("User registered successfully", registerResult.body?.message)

        // Test login
        val loginResult = authController.login(loginDto)
        assertEquals(HttpStatus.OK, loginResult.statusCode)
        assertEquals("Login successful", loginResult.body?.message)

        verify(userService).createUser(registerDto)
        verify(userService).login(loginDto)
    }
}
