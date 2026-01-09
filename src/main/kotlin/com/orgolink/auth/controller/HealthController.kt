package com.orgolink.auth.controller

import java.time.LocalDateTime
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class HealthController {

    data class HealthResponse(
            val status: String,
            val timestamp: LocalDateTime,
            val service: String
    )

    @GetMapping("/healthy")
    fun healthCheck(): ResponseEntity<HealthResponse> {
        return ResponseEntity.ok(
                HealthResponse(
                        status = "UP",
                        timestamp = LocalDateTime.now(),
                        service = "orgolink-auth"
                )
        )
    }
}
