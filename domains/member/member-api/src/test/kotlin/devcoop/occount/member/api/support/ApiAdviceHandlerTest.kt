package devcoop.occount.member.api.support

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException
import devcoop.occount.member.application.exception.InvalidPasswordException
import devcoop.occount.member.application.exception.InvalidPinException
import devcoop.occount.member.application.exception.UserAlreadyExistsException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.usecase.register.MemberRegisterRequest
import kotlin.test.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

@DisplayName("ApiAdviceHandler 단위 테스트")
class ApiAdviceHandlerTest {
    private val apiAdviceHandler = ApiAdviceHandler()

    @Test
    @DisplayName("UserNotFoundException 발생 시 404 NOT_FOUND와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 404 for UserNotFoundException`() {
        val response = apiAdviceHandler.handleBusinessBaseException(UserNotFoundException())

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("존재하지 않는 유저입니다.", response.body?.message)
    }

    @Test
    @DisplayName("InvalidPasswordException 발생 시 401 UNAUTHORIZED와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 401 for InvalidPasswordException`() {
        val response = apiAdviceHandler.handleBusinessBaseException(InvalidPasswordException())

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("비밀번호가 일치하지 않습니다.", response.body?.message)
    }

    @Test
    @DisplayName("InvalidPinException 발생 시 401 UNAUTHORIZED와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 401 for InvalidPinException`() {
        val response = apiAdviceHandler.handleBusinessBaseException(InvalidPinException())

        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
        assertEquals("핀번호가 틀렸습니다.", response.body?.message)
    }

    @Test
    @DisplayName("UserAlreadyExistsException 발생 시 409 CONFLICT와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 409 for UserAlreadyExistsException`() {
        val response = apiAdviceHandler.handleBusinessBaseException(UserAlreadyExistsException())

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("이미 존재하는 유저입니다.", response.body?.message)
    }

    @Test
    @DisplayName("유효성 검증 실패 시 400 BAD_REQUEST와 필드별 에러 메시지 맵을 반환한다")
    fun `handleValidationException returns 400 with field error messages`() {
        val bindingResult = BeanPropertyBindingResult(
            MemberRegisterRequest(
                userCiNumber = "CI123",
                username = "홍길동",
                userPhone = null,
                birthDate = null,
                userEmail = "invalid-email",
                password = "password1234",
                pin = "1234",
            ),
            "request",
        ).apply {
            addError(FieldError("request", "userEmail", "올바른 이메일 형식이어야 합니다."))
        }

        val exception = MethodArgumentNotValidException(
            MethodParameter(
                ApiAdviceHandlerTest::class.java.getDeclaredMethod("sample", MemberRegisterRequest::class.java),
                0,
            ),
            bindingResult,
        )

        val response = apiAdviceHandler.handleValidationException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        // FieldError의 프로퍼티명(userEmail)은 응답에서 snake_case(user_email)로 변환된다
        assertEquals("올바른 이메일 형식이어야 합니다.", response.body?.get("user_email"))
    }

    @Test
    @DisplayName("ErrorMessage별로 resolveStatus가 올바른 HttpStatus로 매핑한다")
    fun `resolveStatus maps every error message branch`() {
        val cases: List<Pair<ErrorMessage, HttpStatus>> = listOf(
            ErrorMessage.INVALID_TOKEN to HttpStatus.UNAUTHORIZED,
            ErrorMessage.EXPIRED_TOKEN to HttpStatus.UNAUTHORIZED,
            ErrorMessage.INVALID_PASSWORD to HttpStatus.UNAUTHORIZED,
            ErrorMessage.INVALID_PIN to HttpStatus.UNAUTHORIZED,
            ErrorMessage.OTP_EXPIRED to HttpStatus.UNAUTHORIZED,
            ErrorMessage.OTP_MISMATCH to HttpStatus.UNAUTHORIZED,
            ErrorMessage.OTP_LOCKED to HttpStatus.UNAUTHORIZED,
            ErrorMessage.EMAIL_NOT_VERIFIED to HttpStatus.UNAUTHORIZED,
            ErrorMessage.OTP_RATE_LIMITED to HttpStatus.TOO_MANY_REQUESTS,
            ErrorMessage.IDENTITY_VERIFICATION_FAILED to HttpStatus.BAD_GATEWAY,
            ErrorMessage.IDENTITY_NOT_VERIFIED to HttpStatus.BAD_REQUEST,
            ErrorMessage.USER_NOT_FOUND to HttpStatus.NOT_FOUND,
            ErrorMessage.ITEM_NOT_FOUND to HttpStatus.NOT_FOUND,
            ErrorMessage.ITEM_NOT_SYNCHRONIZED to HttpStatus.NOT_FOUND,
            ErrorMessage.PAYMENT_LOG_NOT_FOUND to HttpStatus.NOT_FOUND,
            ErrorMessage.OTP_NOT_FOUND to HttpStatus.NOT_FOUND,
            ErrorMessage.USER_ALREADY_EXISTS to HttpStatus.CONFLICT,
            ErrorMessage.TRANSACTION_IN_PROGRESS to HttpStatus.CONFLICT,
            ErrorMessage.PAYMENT_TIMEOUT to HttpStatus.REQUEST_TIMEOUT,
            ErrorMessage.PAYMENT_LOG_SAVE_FAILED to HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorMessage.CHARGE_LOG_SAVE_FAILED to HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorMessage.ACCESS_DENIED to HttpStatus.BAD_REQUEST,
        )

        assertAll(
            cases.map { (errorMessage, expectedStatus) ->
                {
                    val exception = object : BusinessBaseException(errorMessage) {}
                    assertEquals(expectedStatus, apiAdviceHandler.handleBusinessBaseException(exception).statusCode)
                }
            },
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun sample(request: MemberRegisterRequest) = Unit
}
