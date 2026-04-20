package devcoop.occount.member.api.auth

import devcoop.occount.member.application.usecase.login.KioskLoginRequest
import devcoop.occount.member.application.usecase.login.LoginUserUseCase
import devcoop.occount.member.application.usecase.login.MemberLoginRequest
import devcoop.occount.member.application.usecase.register.MemberRegisterRequest
import devcoop.occount.member.application.usecase.register.RegisterUserUseCase
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletResponse

@DisplayName("AuthController 단위 테스트")
class AuthControllerTest {

    private lateinit var loginUserUseCase: LoginUserUseCase
    private lateinit var registerUserUseCase: RegisterUserUseCase
    private lateinit var authController: AuthController

    @BeforeEach
    fun setUp() {
        loginUserUseCase = mock(LoginUserUseCase::class.java)
        registerUserUseCase = mock(RegisterUserUseCase::class.java)
        authController = AuthController(loginUserUseCase, registerUserUseCase)
    }

    @Test
    @DisplayName("회원가입 요청이 성공하면 201 Created를 반환한다")
    fun `register returns 201 Created on success`() {
        val request = MemberRegisterRequest(
            userCiNumber = "CI123",
            userName = "홍길동",
            userPhone = null,
            userEmail = "test@test.com",
            password = "password1234",
        )
        doNothing().`when`(registerUserUseCase).register(request)

        val response = authController.register(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        verify(registerUserUseCase).register(request)
    }

    @Test
    @DisplayName("멤버 로그인 성공 시 Authorization 헤더에 Bearer 토큰이 설정된다")
    fun `memberLogin sets Authorization header with Bearer token`() {
        val request = MemberLoginRequest("test@test.com", "password1234")
        val mockResponse = MockHttpServletResponse()

        `when`(loginUserUseCase.login(request)).thenReturn("generatedAccessToken")

        authController.login(request, mockResponse)

        assertEquals("Bearer generatedAccessToken", mockResponse.getHeader(HttpHeaders.AUTHORIZATION))
        verify(loginUserUseCase).login(request)
    }

    @Test
    @DisplayName("키오스크 로그인 성공 시 Authorization 헤더에 Bearer 토큰이 설정된다")
    fun `kioskLogin sets Authorization header with Bearer token`() {
        val request = KioskLoginRequest("BARCODE123", "123456")
        val mockResponse = MockHttpServletResponse()

        `when`(loginUserUseCase.login(request)).thenReturn("generatedKioskToken")

        authController.login(request, mockResponse)

        assertEquals("Bearer generatedKioskToken", mockResponse.getHeader(HttpHeaders.AUTHORIZATION))
        verify(loginUserUseCase).login(request)
    }
}
