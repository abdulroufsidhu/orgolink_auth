package com.orgolink.auth.service

import com.orgolink.auth.config.AuthProperties
import com.orgolink.auth.security.JwtUtil
import org.springframework.stereotype.Service

@Service
class AuthorizationService(
        private val authProperties: AuthProperties,
        private val jwtUtil: JwtUtil
) {

    fun verifyRole(tokenValue: String, roleNameParam: String): Boolean {
        if (!jwtUtil.validateToken(tokenValue)) {
            return false
        }

        val roles = jwtUtil.getRolesFromToken(tokenValue)
        return roles.contains(roleNameParam)
    }

    fun verifyPermission(tokenValue: String, permissionParam: String): Boolean {
        if (!jwtUtil.validateToken(tokenValue)) {
            return false
        }

        val permissions = jwtUtil.getPermissionsFromToken(tokenValue)
        return permissions.contains(permissionParam)
    }
}
