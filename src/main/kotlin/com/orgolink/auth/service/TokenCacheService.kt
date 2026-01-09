package com.orgolink.auth.service

import com.orgolink.auth.config.AuthProperties
import java.util.concurrent.TimeUnit
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Service

@Service
class TokenCacheService(
        private val redisTemplate: RedisTemplate<String, String>,
        private val authProperties: AuthProperties
) {

    private val TOKEN_PREFIX = "auth:token:"

    fun cacheToken(tokenValue: String, username: String) {
        val key = TOKEN_PREFIX + tokenValue
        val ttl = authProperties.jwt.expirationMs
        redisTemplate.opsForValue().set(key, username, ttl, TimeUnit.MILLISECONDS)
    }

    fun getTokenFromCache(tokenValue: String): String? {
        val key = TOKEN_PREFIX + tokenValue
        return redisTemplate.opsForValue().get(key)
    }

    fun invalidateToken(tokenValue: String) {
        val key = TOKEN_PREFIX + tokenValue
        redisTemplate.delete(key)
    }

    fun isTokenInCache(tokenValue: String): Boolean {
        val key = TOKEN_PREFIX + tokenValue
        return redisTemplate.hasKey(key)
    }
}
