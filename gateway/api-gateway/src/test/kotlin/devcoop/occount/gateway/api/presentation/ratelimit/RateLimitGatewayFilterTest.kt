package devcoop.occount.gateway.api.presentation.ratelimit

import devcoop.occount.gateway.api.presentation.ClientIpResolver
import devcoop.occount.gateway.api.presentation.RateLimitExceededWriter
import devcoop.occount.gateway.api.presentation.RateLimitGatewayFilter
import devcoop.occount.gateway.api.presentation.RateLimitPolicy
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper

class RateLimitGatewayFilterTest {
    private lateinit var filter: RateLimitGatewayFilter

    @BeforeEach
    fun setUp() {
        // 매 테스트마다 새 버킷 캐시로 시작해 테스트 간 상태 오염을 차단한다.
        filter = RateLimitGatewayFilter(
            RateLimitPolicy(),
            ClientIpResolver(),
            RateLimitExceededWriter(ObjectMapper()),
        )
    }

    @Test
    fun `unlisted path is forwarded without limiting`() {
        val exchange = exchangeFor("/api/v3/items", ip = "1.1.1.1")
        var forwarded = false
        val chain = GatewayFilterChain { forwarded = true; Mono.empty() }

        filter.filter(exchange, chain).block()

        assertTrue(forwarded)
    }

    @Test
    fun `requests within the limit are forwarded`() {
        repeat(5) {
            val exchange = exchangeFor("/api/v3/auth/email/send-otp", ip = "2.2.2.2")
            var forwarded = false
            val chain = GatewayFilterChain { forwarded = true; Mono.empty() }

            filter.filter(exchange, chain).block()

            assertTrue(forwarded, "request #$it within limit should pass")
        }
    }

    @Test
    fun `request over the limit returns 429 with retry-after`() {
        // send-otp 한도는 분당 5 — 6번째 요청은 차단된다.
        repeat(5) {
            val exchange = exchangeFor("/api/v3/auth/email/send-otp", ip = "3.3.3.3")
            filter.filter(exchange, GatewayFilterChain { Mono.empty() }).block()
        }

        val blocked = exchangeFor("/api/v3/auth/email/send-otp", ip = "3.3.3.3")
        var forwarded = false
        filter.filter(blocked, GatewayFilterChain { forwarded = true; Mono.empty() }).block()

        assertFalse(forwarded, "over-limit request must not be forwarded")
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, blocked.response.statusCode)
        assertNotNull(blocked.response.headers.getFirst(HttpHeaders.RETRY_AFTER))
    }

    @Test
    fun `limit is tracked per ip`() {
        // 3.3.3.4 가 한도를 모두 소진해도 다른 IP는 영향받지 않는다.
        repeat(5) {
            filter.filter(exchangeFor("/api/v3/auth/email/send-otp", ip = "3.3.3.4"), GatewayFilterChain { Mono.empty() })
                .block()
        }

        val other = exchangeFor("/api/v3/auth/email/send-otp", ip = "9.9.9.9")
        var forwarded = false
        filter.filter(other, GatewayFilterChain { forwarded = true; Mono.empty() }).block()

        assertTrue(forwarded)
    }

    @Test
    fun `requests without resolvable ip fail open`() {
        // XFF·remoteAddress 모두 없어 IP를 알 수 없으면 한도와 무관하게 통과한다(정상 사용자 보호).
        repeat(20) {
            val request = MockServerHttpRequest.post("/api/v3/auth/email/send-otp").build()
            val exchange = MockServerWebExchange.from(request)
            var forwarded = false

            filter.filter(exchange, GatewayFilterChain { forwarded = true; Mono.empty() }).block()

            assertTrue(forwarded, "request #$it without ip must fail open")
        }
    }

    private fun exchangeFor(path: String, ip: String): MockServerWebExchange {
        val request = MockServerHttpRequest.post(path)
            .header("X-Forwarded-For", ip)
            .build()
        return MockServerWebExchange.from(request)
    }
}
