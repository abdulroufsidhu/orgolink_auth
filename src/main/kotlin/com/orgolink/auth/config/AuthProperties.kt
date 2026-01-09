package com.orgolink.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "orgolink.auth")
data class AuthProperties(
        var jwt: JwtProperties = JwtProperties(),
        var deleteScheme: String = "soft",
        var passResetTimeout: Long = 3600000,
        var roles: List<RoleConfig> = emptyList()
)

data class JwtProperties(var secret: String = "", var expirationMs: Long = 86400000)

data class RoleConfig(var name: String = "", var permissions: List<String> = emptyList())
