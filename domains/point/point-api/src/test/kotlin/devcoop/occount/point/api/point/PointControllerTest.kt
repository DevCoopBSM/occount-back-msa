package devcoop.occount.point.api.point

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.point.api.dto.request.ChargePointCommand
import devcoop.occount.point.application.query.balance.PointBalanceResponse
import devcoop.occount.point.application.query.balance.GetPointBalanceQueryService
import devcoop.occount.point.application.usecase.charge.ChargePointRequest
import devcoop.occount.point.application.usecase.charge.ChargePointUseCase
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest

class PointControllerTest {
    @Test
    @DisplayName("인증된 사용자 헤더로 잔액 조회를 위임한다")
    fun `authenticated balance query uses authenticated user id header`() {
        val getPointBalanceQueryService = mock(GetPointBalanceQueryService::class.java)
        val chargePointUseCase = mock(ChargePointUseCase::class.java)
        val controller = PointController(getPointBalanceQueryService, chargePointUseCase)

        `when`(getPointBalanceQueryService.getBalance(4L)).thenReturn(PointBalanceResponse(balance = 800))

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "4")
        }

        val actual = controller.getBalance(httpRequest)

        assertEquals(PointBalanceResponse(balance = 800), actual)
        verify(getPointBalanceQueryService).getBalance(4L)
    }

    @Test
    @DisplayName("충전 요청을 포인트 서비스에 위임한다")
    fun `charge delegates to point service`() {
        val getPointBalanceQueryService = mock(GetPointBalanceQueryService::class.java)
        val chargePointUseCase = mock(ChargePointUseCase::class.java)
        val controller = PointController(getPointBalanceQueryService, chargePointUseCase)
        val request = ChargePointCommand(amount = 500)
        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "3")
        }
        val chargeRequest = ChargePointRequest(userId = 3L, amount = 500)

        `when`(chargePointUseCase.charge(chargeRequest)).thenReturn(PointBalanceResponse(balance = 500))

        val actual = controller.charge(request, httpRequest)

        assertEquals(PointBalanceResponse(balance = 500), actual)
        verify(chargePointUseCase).charge(chargeRequest)
    }
}
