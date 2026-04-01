package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.member.application.query.UserPreOrderInfoResponse
import devcoop.occount.member.application.query.UserPreOrderInfoQueryService
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest

class MemberControllerTest {
    @Test
    fun `find user info delegates with authenticated user id`() {
        val userPreOrderInfoQueryService = mock(UserPreOrderInfoQueryService::class.java)
        val controller = MemberController(userPreOrderInfoQueryService)
        val expected = UserPreOrderInfoResponse(
            username = "Tester",
        )

        `when`(userPreOrderInfoQueryService.findPreOrderInfo(7L)).thenReturn(expected)

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "7")
        }

        val actual = controller.findUserInfo(httpRequest)

        assertSame(expected, actual)
        verify(userPreOrderInfoQueryService).findPreOrderInfo(7L)
    }
}
