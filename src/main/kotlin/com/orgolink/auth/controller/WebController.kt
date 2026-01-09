package com.orgolink.auth.controller

import com.orgolink.auth.service.PasswordResetService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class WebController(private val passwordResetService: PasswordResetService) {

    @GetMapping("/login")
    fun login(): String {
        return "login"
    }

    @GetMapping("/register")
    fun register(): String {
        return "register"
    }

    @GetMapping("/forgot-password")
    fun forgotPassword(): String {
        return "forgot-password"
    }

    @GetMapping("/new-password/{token}")
    fun newPassword(@PathVariable token: String, model: Model): String {
        val isValid = passwordResetService.validateResetToken(token)
        model.addAttribute("token", token)
        model.addAttribute("isValid", isValid)
        return "new-password"
    }
}
