package devcoop.occount.member.api.auth

import devcoop.occount.member.application.auth.KioskLoginRequest
import devcoop.occount.member.application.auth.AuthCommandService
import devcoop.occount.member.application.auth.MemberLoginRequest
import devcoop.occount.member.application.auth.MemberRegisterRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth")
class AuthController(
    private val authCommandService: AuthCommandService,
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: MemberRegisterRequest): ResponseEntity<Void> {
        authCommandService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    fun login(
        @Valid @RequestBody request: MemberLoginRequest,
        response: HttpServletResponse,
    ) {
        val token = authCommandService.login(request)
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    @PostMapping("/kiosk/login")
    @ResponseStatus(HttpStatus.CREATED)
    fun login(
        @Valid @RequestBody request: KioskLoginRequest,
        response: HttpServletResponse,
    ) {
        val token = authCommandService.login(request)
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }
}
