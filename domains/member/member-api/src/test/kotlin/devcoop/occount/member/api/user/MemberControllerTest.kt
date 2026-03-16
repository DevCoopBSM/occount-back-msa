package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.member.application.user.UserPreOrderInfoResponse
import devcoop.occount.member.application.user.UserQueryService
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest

class MemberControllerTest {
    @Test
    fun `find user info delegates with authenticated user id`() {
        val userQueryService = mock(UserQueryService::class.java)
        val controller = MemberController(userQueryService)
        val expected = UserPreOrderInfoResponse(
            username = "Tester",
        )

        `when`(userQueryService.findPreOrderInfo(7L)).thenReturn(expected)

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "7")
        }

        val actual = controller.findUserInfo(httpRequest)

        assertSame(expected, actual)
        verify(userQueryService).findPreOrderInfo(7L)
    }

    @Test
    fun `find payment info delegates with requested user id`() {
        val userQueryService = mock(UserQueryService::class.java)
        val controller = MemberController(userQueryService)
        val expected = UserPaymentInfoResponse(
            userId = 11L,
            email = "member@example.com",
        )

        `when`(userQueryService.findPaymentInfo(11L)).thenReturn(expected)

        val actual = controller.findPaymentInfo(11L)

        assertSame(expected, actual)
        verify(userQueryService).findPaymentInfo(11L)
    }
}
