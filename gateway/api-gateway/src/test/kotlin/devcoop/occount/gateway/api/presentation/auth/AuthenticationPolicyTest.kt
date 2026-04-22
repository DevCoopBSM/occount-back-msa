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

    @Test
    fun `order stream allows optional auth`() {
        assertEquals(
            AuthenticationRule.Access.OPTIONAL_AUTH,
            policy.resolveAccess(HttpMethod.GET, "/api/v3/orders/order-1/stream"),
        )
    }

    @Test
    fun `ari pick list path is public`() {
        assertEquals(AuthenticationRule.Access.PERMIT_ALL, policy.resolveAccess(HttpMethod.GET, "/api/v3/ari-pick"))
    }

    @Test
    fun `ari pick detail path is public`() {
        assertEquals(AuthenticationRule.Access.PERMIT_ALL, policy.resolveAccess(HttpMethod.GET, "/api/v3/ari-pick/1"))
    }

    @Test
    fun `ari pick stats path is public`() {
        assertEquals(AuthenticationRule.Access.PERMIT_ALL, policy.resolveAccess(HttpMethod.GET, "/api/v3/ari-pick/stats"))
    }

    @Test
    fun `ari pick foods search requires authentication`() {
        assertEquals(AuthenticationRule.Access.AUTHENTICATED, policy.resolveAccess(HttpMethod.GET, "/api/v3/ari-pick/foods"))
    }

    @Test
    fun `ari pick creation requires authentication`() {
        assertEquals(AuthenticationRule.Access.AUTHENTICATED, policy.resolveAccess(HttpMethod.POST, "/api/v3/ari-pick"))
    }

    @Test
    fun `ari pick user delete requires authentication`() {
        assertEquals(AuthenticationRule.Access.AUTHENTICATED, policy.resolveAccess(HttpMethod.DELETE, "/api/v3/ari-pick/1"))
    }

    @Test
    fun `ari pick like requires authentication`() {
        assertEquals(AuthenticationRule.Access.AUTHENTICATED, policy.resolveAccess(HttpMethod.POST, "/api/v3/ari-pick/1/like"))
    }

    @Test
    fun `ari pick approve requires admin`() {
        assertEquals(AuthenticationRule.Access.ADMIN_ONLY, policy.resolveAccess(HttpMethod.PATCH, "/api/v3/ari-pick/1/approve"))
    }

    @Test
    fun `ari pick reject requires admin`() {
        assertEquals(AuthenticationRule.Access.ADMIN_ONLY, policy.resolveAccess(HttpMethod.PATCH, "/api/v3/ari-pick/1/reject"))
    }

    @Test
    fun `ari pick pending requires admin`() {
        assertEquals(AuthenticationRule.Access.ADMIN_ONLY, policy.resolveAccess(HttpMethod.PATCH, "/api/v3/ari-pick/1/pending"))
    }

    @Test
    fun `ari pick admin delete requires admin`() {
        assertEquals(AuthenticationRule.Access.ADMIN_ONLY, policy.resolveAccess(HttpMethod.DELETE, "/api/v3/ari-pick/1/admin"))
    }

    @Test
    fun `ari pick blocked keywords list requires admin`() {
        assertEquals(
            AuthenticationRule.Access.ADMIN_ONLY,
            policy.resolveAccess(HttpMethod.GET, "/api/v3/ari-pick/blocked-keywords"),
        )
    }

    @Test
    fun `ari pick blocked keywords create requires admin`() {
        assertEquals(
            AuthenticationRule.Access.ADMIN_ONLY,
            policy.resolveAccess(HttpMethod.POST, "/api/v3/ari-pick/blocked-keywords"),
        )
    }

    @Test
    fun `ari pick blocked keywords delete requires admin`() {
        assertEquals(
            AuthenticationRule.Access.ADMIN_ONLY,
            policy.resolveAccess(HttpMethod.DELETE, "/api/v3/ari-pick/blocked-keywords/1"),
        )
    }
}
