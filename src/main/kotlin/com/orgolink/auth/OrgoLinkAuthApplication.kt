package com.orgolink.auth

import com.orgolink.auth.config.AuthProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AuthProperties::class)
class OrgoLinkAuthApplication

fun main(args: Array<String>) {
    runApplication<OrgoLinkAuthApplication>(*args)
}
