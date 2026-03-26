package devcoop.occount.gateway.api.presentation.auth

import devcoop.occount.gateway.api.presentation.AuthenticationPolicy
import devcoop.occount.gateway.api.presentation.AuthenticationRule
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod

class AuthenticationPolicyTest {
    private val policy = AuthenticationPolicy()

    @Test
    fun `auth path is bypassed`() {
        assertEquals(AuthenticationRule.Access.PERMIT_ALL, policy.resolveAccess(HttpMethod.POST, "/api/v3/auth/login"))
    }

    @Test
    fun `payment path requires authentication`() {
        assertEquals(AuthenticationRule.Access.AUTHENTICATED, policy.resolveAccess(HttpMethod.POST, "/api/v3/payments/execute"))
    }

    @Test
    fun `items management path requires admin access`() {
        assertEquals(AuthenticationRule.Access.ADMIN_ONLY, policy.resolveAccess(HttpMethod.GET, "/api/v3/items"))
    }

    @Test
    fun `public item lookup is bypassed`() {
        assertEquals(AuthenticationRule.Access.PERMIT_ALL, policy.resolveAccess(HttpMethod.GET, "/api/v3/items/88012341234"))
    }

    @Test
    fun `without barcode path requires authentication`() {
        assertEquals(
            AuthenticationRule.Access.AUTHENTICATED,
            policy.resolveAccess(HttpMethod.GET, "/api/v3/items/without-barcode"),
        )
    }
}
