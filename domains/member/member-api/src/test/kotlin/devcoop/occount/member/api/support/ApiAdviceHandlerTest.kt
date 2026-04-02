package devcoop.occount.member.api.support

import devcoop.occount.member.application.exception.PointNotFoundException
import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.UserAlreadyExistsException
import devcoop.occount.member.application.exception.UserNotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

@DisplayName("ApiAdviceHandler 단위 테스트")
class ApiAdviceHandlerTest {

    private lateinit var apiAdviceHandler: ApiAdviceHandler

    @BeforeEach
    fun setUp() {
        apiAdviceHandler = ApiAdviceHandler()
    }

    @Test
    @DisplayName("UserNotFoundException 발생 시 404 NOT_FOUND와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 404 for UserNotFoundException`() {
        val exception = UserNotFoundException()

        val response = apiAdviceHandler.handleBusinessBaseException(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("존재하지 않는 유저입니다.", response.body?.message)
    }

    @Test
    @DisplayName("InvalidPasswordException 발생 시 401 UNAUTHORIZED와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 401 for InvalidPasswordException`() {
        val exception = InvalidPasswordException()

        val response = apiAdviceHandler.handleBusinessBaseException(exception)

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("비밀번호가 일치하지 않습니다.", response.body?.message)
    }

    @Test
    @DisplayName("PointNotFoundException 발생 시 404 NOT_FOUND와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 404 for PointNotFoundException`() {
        val exception = PointNotFoundException()

        val response = apiAdviceHandler.handleBusinessBaseException(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("포인트 정보를 조회할 수 없습니다.", response.body?.message)
    }

    @Test
    @DisplayName("UserAlreadyExistsException 발생 시 409 CONFLICT와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 409 for UserAlreadyExistsException`() {
        val exception = UserAlreadyExistsException()

        val response = apiAdviceHandler.handleBusinessBaseException(exception)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("이미 존재하는 유저입니다.", response.body?.message)
    }

    @Test
    @DisplayName("유효성 검증 실패 시 400 BAD_REQUEST와 필드별 에러 메시지 맵을 반환한다")
    fun `handleValidationException returns 400 with field error messages`() {
        val fieldError = FieldError("request", "userEmail", "올바른 이메일 형식이어야 합니다.")
        val bindingResult = mock(BindingResult::class.java)
        `when`(bindingResult.allErrors).thenReturn(listOf(fieldError))

        val exception = mock(MethodArgumentNotValidException::class.java)
        `when`(exception.bindingResult).thenReturn(bindingResult)

        val response = apiAdviceHandler.handleValidationException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("올바른 이메일 형식이어야 합니다.", response.body?.get("userEmail"))
    }
}
