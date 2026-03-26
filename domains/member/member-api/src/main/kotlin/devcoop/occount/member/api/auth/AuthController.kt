package devcoop.occount.member.api.auth

import devcoop.occount.member.application.usecase.login.KioskLoginRequest
import devcoop.occount.member.application.usecase.login.LoginUserUseCase
import devcoop.occount.member.application.usecase.login.MemberLoginRequest
import devcoop.occount.member.application.usecase.register.MemberRegisterRequest
import devcoop.occount.member.application.usecase.register.RegisterUserUseCase
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
    private val loginUserUseCase: LoginUserUseCase,
    private val registerUserUseCase: RegisterUserUseCase,
) {
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: MemberRegisterRequest): ResponseEntity<Void> {
        registerUserUseCase.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.CREATED)
    fun login(
        @Valid @RequestBody request: MemberLoginRequest,
        response: HttpServletResponse,
    ) {
        val token = loginUserUseCase.login(request)
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    @PostMapping("/kiosk/login")
    @ResponseStatus(HttpStatus.CREATED)
    fun login(
        @Valid @RequestBody request: KioskLoginRequest,
        response: HttpServletResponse,
    ) {
        val token = loginUserUseCase.login(request)
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }
}
