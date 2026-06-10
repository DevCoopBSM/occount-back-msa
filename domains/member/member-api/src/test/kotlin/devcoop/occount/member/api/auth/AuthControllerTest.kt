package devcoop.occount.member.api.auth

import devcoop.occount.member.api.support.FakeEventPublisher
import devcoop.occount.member.api.support.FakeEmailOtpRepository
import devcoop.occount.member.api.support.FakePasswordEncoder
import devcoop.occount.member.api.support.FakeTokenGenerator
import devcoop.occount.member.api.support.FakeUserRepository
import devcoop.occount.member.api.support.mockMvc
import devcoop.occount.member.api.support.userFixture
import devcoop.occount.member.api.support.FakeIdentityVerificationClient
import devcoop.occount.member.api.support.testSendEmailOtpUseCase
import devcoop.occount.member.api.support.testVerifyEmailOtpUseCase
import devcoop.occount.member.api.support.testVerifyIdentityUseCase
import devcoop.occount.member.api.support.testChangePasswordUseCase
import devcoop.occount.member.api.support.passwordResetOtp
import devcoop.occount.member.api.support.verifiedEmailOtp
import devcoop.occount.member.application.exception.IdentityVerificationFailedException
import devcoop.occount.member.application.usecase.identity.VerifyIdentityUseCase
import devcoop.occount.member.application.usecase.login.LoginUserUseCase
import devcoop.occount.member.application.usecase.register.RegisterUserUseCase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@DisplayName("AuthController 웹 테스트")
class AuthControllerTest {
    @Test
    @DisplayName("이메일 OTP 발송 요청이 성공하면 204 No Content를 반환한다")
    fun `sendEmailOtp returns 204 on success`() {
        val emailOtpRepository = FakeEmailOtpRepository()

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/email/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "test@test.com"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("이메일 OTP 발송 요청이 유효하지 않으면 400을 반환한다")
    fun `sendEmailOtp returns 400 when email is invalid`() {
        val emailOtpRepository = FakeEmailOtpRepository()

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/email/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "not-an-email"}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.email").value("올바른 이메일 형식이어야 합니다."))
    }

    @Test
    @DisplayName("이메일 OTP 인증 요청이 성공하면 204 No Content를 반환한다")
    fun `verifyEmailOtp returns 204 on success`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                "test@test.com" to verifiedEmailOtp("test@test.com", otpCode = "654321").copy(verified = false),
            ),
        )

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/email/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "test@test.com", "otp_code": "654321"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("이메일 OTP 인증 요청이 유효하지 않으면 400을 반환한다")
    fun `verifyEmailOtp returns 400 when request is invalid`() {
        val emailOtpRepository = FakeEmailOtpRepository()

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/email/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "not-an-email", "otp_code": "12"}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.email").value("올바른 이메일 형식이어야 합니다."))
            .andExpect(jsonPath("$.otpCode").value("인증번호는 6자리여야 합니다."))
    }
    @Test
    @DisplayName("회원가입 요청이 성공하면 201 Created를 반환한다")
    fun `register returns 201 Created on success`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                "test@test.com" to verifiedEmailOtp("test@test.com"),
            ),
        )

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userCiNumber": "CI123",
                      "username": "홍길동",
                      "userPhone": null,
                      "userEmail": "test@test.com",
                      "password": "password1234",
                      "pin": "123456"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isCreated)
    }

    @Test
    @DisplayName("회원가입 요청이 유효하지 않으면 400과 필드 에러를 반환한다")
    fun `register returns 400 when request is invalid`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                "test@test.com" to verifiedEmailOtp("test@test.com"),
            ),
        )

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userCiNumber": "CI123",
                      "username": "홍길동",
                      "userPhone": null,
                      "userEmail": "invalid-email",
                      "password": "short",
                      "pin": "123456"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.userEmail").value("올바른 이메일 형식이어야 합니다."))
            .andExpect(jsonPath("$.password").value("비밀번호는 최소 8자 이상 16자 이하여야 합니다."))
    }

    @Test
    @DisplayName("멤버 로그인 성공 시 Authorization 헤더에 Bearer 토큰이 설정된다")
    fun `memberLogin sets Authorization header with Bearer token`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                "test@test.com" to verifiedEmailOtp("test@test.com"),
            ),
        )

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture())),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "test@test.com",
                      "password": "password1234"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer access-1-ROLE_USER"))
    }

    @Test
    @DisplayName("키오스크 로그인 성공 시 Authorization 헤더에 Bearer 토큰이 설정된다")
    fun `kioskLogin sets Authorization header with Bearer token`() {
        val emailOtpRepository = FakeEmailOtpRepository()

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture())),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/kiosk/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userBarcode": "BARCODE123",
                      "userPin": "123456"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer kiosk-1-ROLE_USER"))
    }

    @Test
    @DisplayName("멤버 로그인 비밀번호가 틀리면 401을 반환한다")
    fun `memberLogin returns 401 when password is invalid`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                "test@test.com" to verifiedEmailOtp("test@test.com"),
            ),
        )

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture())),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "email": "test@test.com",
                      "password": "wrong-password"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    @DisplayName("본인인증 검증 성공 시 200과 사용자 정보를 반환한다")
    fun `verifyIdentity returns 200 with user info on success`() {
        val emailOtpRepository = FakeEmailOtpRepository()
        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/identity/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"identity_verification_id": "test-verification-id"}"""),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.userCiNumber").value("CI_TEST_123"))
            .andExpect(jsonPath("$.username").value("홍길동"))
            .andExpect(jsonPath("$.userPhone").value("01012345678"))
            .andExpect(jsonPath("$.birthDate").value("2000-01-15"))
    }

    @Test
    @DisplayName("본인인증 검증 실패 시 502를 반환한다")
    fun `verifyIdentity returns 502 when verification fails`() {
        val emailOtpRepository = FakeEmailOtpRepository()
        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = VerifyIdentityUseCase(
                    identityVerificationClient = object : devcoop.occount.member.application.output.IdentityVerificationClient {
                        override fun verify(identityVerificationId: String) = throw IdentityVerificationFailedException()
                    },
                ),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/identity/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"identity_verification_id": "invalid-id"}"""),
        ).andExpect(status().isBadGateway)
    }

    @Test
    @DisplayName("키오스크 로그인 비밀번호가 틀리면 401을 반환한다")
    fun `kioskLogin returns 401 when pin is invalid`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                "test@test.com" to verifiedEmailOtp("test@test.com"),
            ),
        )

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture())),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/kiosk/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "userBarcode": "BARCODE123",
                      "userPin": "wrong-pin"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("핀번호가 틀렸습니다."))
    }

    @Test
    @DisplayName("비밀번호 변경용 OTP 코드와 함께 요청이 성공하면 204 No Content를 반환한다")
    fun `changePassword returns 204 No Content on success`() {
        val emailOtpRepository = FakeEmailOtpRepository(
            initialOtpsByEmail = mapOf(
                "test@test.com" to passwordResetOtp("test@test.com", otpCode = "123456"),
            ),
        )

        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = emailOtpRepository,
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(emailOtpRepository),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(emailOtpRepository),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(email = "test@test.com"))),
                    emailOtpRepository = emailOtpRepository,
                ),
            ),
        )

        mockMvc.perform(
            post("/auth/password/change")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "test@test.com", "otpCode": "123456", "newPassword": "newPassword1234"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    @DisplayName("비밀번호 변경 요청의 새 비밀번호가 형식에 맞지 않으면 400과 필드 에러를 반환한다")
    fun `changePassword returns 400 when new password is invalid`() {
        val mockMvc = mockMvc(
            AuthController(
                loginUserUseCase = LoginUserUseCase(
                    userRepository = FakeUserRepository(),
                    tokenGenerator = FakeTokenGenerator(),
                    passwordEncoder = FakePasswordEncoder(),
                ),
                registerUserUseCase = RegisterUserUseCase(
                    userRepository = FakeUserRepository(),
                    eventPublisher = FakeEventPublisher(),
                    passwordEncoder = FakePasswordEncoder(),
                    emailOtpRepository = FakeEmailOtpRepository(),
                ),
                sendEmailOtpUseCase = testSendEmailOtpUseCase(FakeEmailOtpRepository()),
                verifyEmailOtpUseCase = testVerifyEmailOtpUseCase(FakeEmailOtpRepository()),
                verifyIdentityUseCase = testVerifyIdentityUseCase(),
                changePasswordUseCase = testChangePasswordUseCase(),
            ),
        )

        mockMvc.perform(
            post("/auth/password/change")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"email": "test@test.com", "otpCode": "123456", "newPassword": "short"}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.newPassword").value("비밀번호는 최소 8자 이상 16자 이하여야 합니다."))
    }
}
