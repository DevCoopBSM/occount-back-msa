package devcoop.occount.member.api.auth

import devcoop.occount.member.api.auth.dto.SendEmailOtpRequest
import devcoop.occount.member.api.auth.dto.VerifyEmailOtpRequest
import devcoop.occount.member.api.auth.dto.VerifyIdentityRequest
import devcoop.occount.member.application.usecase.identity.VerifyIdentityResponse
import devcoop.occount.member.application.usecase.identity.VerifyIdentityUseCase
import devcoop.occount.member.application.usecase.login.KioskLoginRequest
import devcoop.occount.member.application.usecase.login.LoginUserUseCase
import devcoop.occount.member.application.usecase.login.MemberLoginRequest
import devcoop.occount.member.application.usecase.otp.SendEmailOtpUseCase
import devcoop.occount.member.application.usecase.otp.VerifyEmailOtpUseCase
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
    private val sendEmailOtpUseCase: SendEmailOtpUseCase,
    private val verifyEmailOtpUseCase: VerifyEmailOtpUseCase,
    private val verifyIdentityUseCase: VerifyIdentityUseCase,
) {
    @PostMapping("/email/send-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun sendEmailOtp(@Valid @RequestBody request: SendEmailOtpRequest) {
        sendEmailOtpUseCase.send(request.email)
    }

    @PostMapping("/email/verify-otp")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun verifyEmailOtp(@Valid @RequestBody request: VerifyEmailOtpRequest) {
        verifyEmailOtpUseCase.verify(request.email, request.otpCode)
    }

    @PostMapping("/identity/verify")
    @ResponseStatus(HttpStatus.OK)
    fun verifyIdentity(@Valid @RequestBody request: VerifyIdentityRequest): VerifyIdentityResponse {
        return verifyIdentityUseCase.verify(request.identityVerificationId)
    }

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: MemberRegisterRequest): ResponseEntity<Void> {
        registerUserUseCase.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).build()
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(
        @Valid @RequestBody request: MemberLoginRequest,
        response: HttpServletResponse,
    ) {
        val token = loginUserUseCase.login(request)
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    @PostMapping("/kiosk/login")
    @ResponseStatus(HttpStatus.OK)
    fun login(
        @Valid @RequestBody request: KioskLoginRequest,
        response: HttpServletResponse,
    ) {
        val token = loginUserUseCase.login(request)
        response.setHeader(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }
}
