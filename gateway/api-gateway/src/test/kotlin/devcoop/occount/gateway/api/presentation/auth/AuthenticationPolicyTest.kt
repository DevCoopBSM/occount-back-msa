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
    fun `wallet path requires authentication`() {
        assertEquals(AuthenticationRule.Access.AUTHENTICATED, policy.resolveAccess(HttpMethod.POST, "/api/v3/wallet/charge"))
    }

    @Test
    fun `items list path is public`() {
        assertEquals(AuthenticationRule.Access.PERMIT_ALL, policy.resolveAccess(HttpMethod.GET, "/api/v3/items"))
    }

    @Test
    fun `public item lookup is bypassed`() {
        assertEquals(AuthenticationRule.Access.PERMIT_ALL, policy.resolveAccess(HttpMethod.GET, "/api/v3/items/88012341234"))
    }

    @Test
    fun `without barcode path is public`() {
        assertEquals(
            AuthenticationRule.Access.PERMIT_ALL,
            policy.resolveAccess(HttpMethod.GET, "/api/v3/items/without-barcode"),
        )
    }

    @Test
    fun `item creation requires admin`() {
        assertEquals(AuthenticationRule.Access.ADMIN_ONLY, policy.resolveAccess(HttpMethod.POST, "/api/v3/items"))
    }

    @Test
    fun `order creation allows optional auth`() {
        assertEquals(AuthenticationRule.Access.OPTIONAL_AUTH, policy.resolveAccess(HttpMethod.POST, "/api/v3/orders"))
    }

    @Test
    fun `order cancel allows optional auth`() {
        assertEquals(
            AuthenticationRule.Access.OPTIONAL_AUTH,
            policy.resolveAccess(HttpMethod.POST, "/api/v3/orders/order-1/cancel"),
        )
    }
}
