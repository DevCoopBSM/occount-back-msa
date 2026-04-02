package devcoop.occount.payment.api.support

import devcoop.occount.payment.application.exception.WalletNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

@DisplayName("Payment ApiAdviceHandler 단위 테스트")
class ApiAdviceHandlerTest {

    private lateinit var apiAdviceHandler: ApiAdviceHandler

    @BeforeEach
    fun setUp() {
        apiAdviceHandler = ApiAdviceHandler()
    }

    @Test
    @DisplayName("WalletNotFoundException 발생 시 404 NOT_FOUND와 에러 메시지를 반환한다")
    fun `handleBusinessBaseException returns 404 for WalletNotFoundException`() {
        val exception = WalletNotFoundException()

        val response = apiAdviceHandler.handleBusinessBaseException(exception)

        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        assertEquals("포인트 정보를 조회할 수 없습니다.", response.body?.message)
    }
}
