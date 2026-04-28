package devcoop.occount.inquiry.api.support

import devcoop.occount.inquiry.application.exception.InquiryAccessDeniedException
import devcoop.occount.inquiry.application.exception.InquiryNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.validation.BindingResult
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException

@DisplayName("inquiry ApiAdviceHandler 단위 테스트")
class ApiAdviceHandlerTest {

    private lateinit var handler: ApiAdviceHandler

    @BeforeEach
    fun setUp() {
        handler = ApiAdviceHandler()
    }

    @Test
    @DisplayName("InquiryNotFoundException 발생 시 404 NOT_FOUND를 반환한다")
    fun `handleBusinessBaseException returns 404 for InquiryNotFoundException`() {
        val response = handler.handleBusinessBaseException(InquiryNotFoundException())

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("문의 정보를 찾을 수 없습니다.", response.body?.message)
    }

    @Test
    @DisplayName("InquiryAccessDeniedException 발생 시 403 FORBIDDEN을 반환한다")
    fun `handleBusinessBaseException returns 403 for InquiryAccessDeniedException`() {
        val response = handler.handleBusinessBaseException(InquiryAccessDeniedException())

        assertEquals(HttpStatus.FORBIDDEN, response.statusCode)
        assertEquals("해당 문의에 접근할 수 없습니다.", response.body?.message)
    }

    @Test
    @DisplayName("유효성 검증 실패 시 400 BAD_REQUEST와 필드별 에러 메시지를 반환한다")
    fun `handleValidationException returns 400 with field errors`() {
        val fieldError = FieldError("request", "title", "제목을 입력해주세요.")
        val bindingResult = mock(BindingResult::class.java)
        `when`(bindingResult.allErrors).thenReturn(listOf(fieldError))

        val exception = mock(MethodArgumentNotValidException::class.java)
        `when`(exception.bindingResult).thenReturn(bindingResult)

        val response = handler.handleValidationException(exception)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("제목을 입력해주세요.", response.body?.get("title"))
    }
}
