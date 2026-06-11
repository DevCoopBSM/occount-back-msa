package devcoop.occount.gateway.api.presentation.ratelimit

import devcoop.occount.gateway.api.presentation.ClientIpResolver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import java.net.InetSocketAddress

class ClientIpResolverTest {
    private val resolver = ClientIpResolver()

    @Test
    fun `uses single x-forwarded-for value`() {
        val request = MockServerHttpRequest.post("/api/v3/auth/login")
            .header("X-Forwarded-For", "203.0.113.7")
            .build()
        assertEquals("203.0.113.7", resolver.resolve(request))
    }

    @Test
    fun `uses first ip when x-forwarded-for chains proxies`() {
        val request = MockServerHttpRequest.post("/api/v3/auth/login")
            .header("X-Forwarded-For", "203.0.113.7, 10.0.0.1, 10.0.0.2")
            .build()
        assertEquals("203.0.113.7", resolver.resolve(request))
    }

    @Test
    fun `falls back to remote address when header absent`() {
        val request = MockServerHttpRequest.post("/api/v3/auth/login")
            .remoteAddress(InetSocketAddress("198.51.100.4", 12345))
            .build()
        assertEquals("198.51.100.4", resolver.resolve(request))
    }

    @Test
    fun `returns null when no header and no remote address`() {
        val request = MockServerHttpRequest.post("/api/v3/auth/login").build()
        assertNull(resolver.resolve(request))
    }
}
