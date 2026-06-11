package devcoop.occount.gateway.api.presentation.ratelimit

import devcoop.occount.gateway.api.presentation.RateLimitPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpMethod

class RateLimitPolicyTest {
    private val policy = RateLimitPolicy()

    @Test
    fun `login is limited to 10 per minute`() {
        val spec = policy.resolveLimit(HttpMethod.POST, "/api/v3/auth/login")
        assertEquals("auth-login", spec?.id)
        assertEquals(10L, spec?.capacity)
    }

    @Test
    fun `kiosk login is not limited at gateway (handled per-barcode in member service)`() {
        assertNull(policy.resolveLimit(HttpMethod.POST, "/api/v3/auth/kiosk/login"))
    }

    @Test
    fun `identity verify is limited for external api cost`() {
        val spec = policy.resolveLimit(HttpMethod.POST, "/api/v3/auth/identity/verify")
        assertEquals("auth-identity-verify", spec?.id)
        assertEquals(5L, spec?.capacity)
    }

    @Test
    fun `send otp is limited`() {
        assertEquals("auth-send-otp", policy.resolveLimit(HttpMethod.POST, "/api/v3/auth/email/send-otp")?.id)
    }

    @Test
    fun `verify otp is limited`() {
        assertEquals("auth-verify-otp", policy.resolveLimit(HttpMethod.POST, "/api/v3/auth/email/verify-otp")?.id)
    }

    @Test
    fun `wrong method is not limited`() {
        assertNull(policy.resolveLimit(HttpMethod.GET, "/api/v3/auth/login"))
    }

    @Test
    fun `unlisted path is not limited`() {
        assertNull(policy.resolveLimit(HttpMethod.POST, "/api/v3/items"))
    }

    @Test
    fun `null method is not limited`() {
        assertNull(policy.resolveLimit(null, "/api/v3/auth/login"))
    }
}
