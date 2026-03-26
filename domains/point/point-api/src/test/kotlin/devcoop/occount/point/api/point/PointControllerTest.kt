package devcoop.occount.point.api.point

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.point.application.point.PointAmountRequest
import devcoop.occount.point.application.point.PointBalanceResponse
import devcoop.occount.point.application.point.PointService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest

class PointControllerTest {
    @Test
    fun `authenticated balance query uses authenticated user id header`() {
        val pointService = mock(PointService::class.java)
        val controller = PointController(pointService)

        `when`(pointService.getBalance(4L)).thenReturn(PointBalanceResponse(balance = 800))

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "4")
        }

        val actual = controller.getBalance(httpRequest)

        assertEquals(PointBalanceResponse(balance = 800), actual)
        verify(pointService).getBalance(4L)
    }

    @Test
    fun `charge delegates to point service`() {
        val pointService = mock(PointService::class.java)
        val controller = PointController(pointService)
        val request = PointAmountRequest(amount = 500)
        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "3")
        }

        `when`(pointService.charge(3L, 500)).thenReturn(PointBalanceResponse(balance = 500))

        val actual = controller.charge(request, httpRequest)

        assertEquals(PointBalanceResponse(balance = 500), actual)
        verify(pointService).charge(3L, 500)
    }
}
