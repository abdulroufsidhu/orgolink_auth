package io.github.abdulroufsidhu.orgolink_auth

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication
@EnableJpaAuditing
@EnableWebSecurity
class OrgolinkAuthApplication

fun main(args: Array<String>) {
	runApplication<OrgolinkAuthApplication>(*args)
}
