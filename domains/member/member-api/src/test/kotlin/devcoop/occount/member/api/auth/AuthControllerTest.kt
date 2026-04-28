package devcoop.occount.member.api.auth

import devcoop.occount.member.api.support.FakeEventPublisher
import devcoop.occount.member.api.support.FakePasswordEncoder
import devcoop.occount.member.api.support.FakeTokenGenerator
import devcoop.occount.member.api.support.FakeUserRepository
import devcoop.occount.member.api.support.mockMvc
import devcoop.occount.member.api.support.userFixture
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
    @DisplayName("회원가입 요청이 성공하면 201 Created를 반환한다")
    fun `register returns 201 Created on success`() {
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
                    defaultPin = "000000",
                ),
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
                      "password": "password1234"
                    }
                    """.trimIndent(),
                ),
        ).andExpect(status().isCreated)
    }

    @Test
    @DisplayName("회원가입 요청이 유효하지 않으면 400과 필드 에러를 반환한다")
    fun `register returns 400 when request is invalid`() {
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
                    defaultPin = "000000",
                ),
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
                      "password": "short"
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
                    defaultPin = "000000",
                ),
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
        ).andExpect(status().isCreated)
            .andExpect(header().string(HttpHeaders.AUTHORIZATION, "Bearer access-1-ROLE_USER"))
    }

    @Test
    @DisplayName("멤버 로그인 비밀번호가 틀리면 401을 반환한다")
    fun `memberLogin returns 401 when password is invalid`() {
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
                    defaultPin = "000000",
                ),
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
    @DisplayName("키오스크 로그인 비밀번호가 틀리면 401을 반환한다")
    fun `kioskLogin returns 401 when pin is invalid`() {
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
                    defaultPin = "000000",
                ),
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
}
