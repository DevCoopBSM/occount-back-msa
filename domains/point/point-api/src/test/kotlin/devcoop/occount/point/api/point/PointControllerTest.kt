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

        `when`(pointService.getBalance(4L)).thenReturn(PointBalanceResponse(userId = 4L, balance = 800))

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "4")
        }

        val actual = controller.getBalance(httpRequest)

        assertEquals(PointBalanceResponse(userId = 4L, balance = 800), actual)
        verify(pointService).getBalance(4L)
    }

    @Test
    fun `direct balance query uses requested user id`() {
        val pointService = mock(PointService::class.java)
        val controller = PointController(pointService)

        `when`(pointService.getBalance(9L)).thenReturn(PointBalanceResponse(userId = 9L, balance = 300))

        val actual = controller.getBalanceByUserId(9L)

        assertEquals(PointBalanceResponse(userId = 9L, balance = 300), actual)
        verify(pointService).getBalance(9L)
    }

    @Test
    fun `charge delegates to point service`() {
        val pointService = mock(PointService::class.java)
        val controller = PointController(pointService)
        val request = PointAmountRequest(amount = 500)

        `when`(pointService.charge(3L, 500)).thenReturn(PointBalanceResponse(userId = 3L, balance = 500))

        val actual = controller.charge(3L, request)

        assertEquals(PointBalanceResponse(userId = 3L, balance = 500), actual)
        verify(pointService).charge(3L, 500)
    }

    @Test
    fun `deduct delegates to point service`() {
        val pointService = mock(PointService::class.java)
        val controller = PointController(pointService)
        val request = PointAmountRequest(amount = 200)

        `when`(pointService.deduct(3L, 200)).thenReturn(PointBalanceResponse(userId = 3L, balance = 300))

        val actual = controller.deduct(3L, request)

        assertEquals(PointBalanceResponse(userId = 3L, balance = 300), actual)
        verify(pointService).deduct(3L, 200)
    }
}
