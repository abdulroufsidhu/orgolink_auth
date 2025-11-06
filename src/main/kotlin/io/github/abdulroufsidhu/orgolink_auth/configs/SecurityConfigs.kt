package io.github.abdulroufsidhu.orgolink_auth.configs

import io.github.abdulroufsidhu.orgolink_auth.filters.JWTAuthenticationFilter
import io.github.abdulroufsidhu.orgolink_auth.filters.ProjectTokenAuthenticationFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
@EnableWebSecurity
class SecurityConfigs(
    private val jwtAuthFilter: JWTAuthenticationFilter,
    private val projectTokenAuthFilter: ProjectTokenAuthenticationFilter
) {

    @Bean
    @Throws(Exception::class)
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { customizer -> customizer.disable() }
            .cors {}
            .authorizeHttpRequests { requests ->
                requests.requestMatchers(
                    "/public/**",
                    "/api/auth/**",
                    "/api/projects/public"
                )
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .sessionManagement { customizer ->
                customizer.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterBefore(projectTokenAuthFilter, JWTAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun authenticationManager(authConfig: AuthenticationConfiguration) =
        authConfig.authenticationManager

    @Bean
    fun authenticationProvider(userDetailsService: UserDetailsService): AuthenticationProvider {
        val provider = DaoAuthenticationProvider(userDetailsService)
        provider.setPasswordEncoder(passwordEncoder())
        return provider
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.setAllowedOriginPatterns(mutableListOf("http://localhost:*", "http://127.0.0.1:*"))
        config.setAllowedMethods(mutableListOf<String?>("GET","POST","PUT", "PATCH", "DELETE"))
        config.setAllowedHeaders(mutableListOf<String?>("*"))
        config.setAllowCredentials(true)

        val source: UrlBasedCorsConfigurationSource = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }

    @Bean
    fun passwordEncoder() = BCryptPasswordEncoder(12)
}
