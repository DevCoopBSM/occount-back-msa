package devcoop.occount.warmup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class ServletWarmupTargetTest {
    @Test
    fun `resolve uses context path and actuator base path by default`() {
        val environment = MockEnvironment()
            .withProperty("server.servlet.context-path", "/api/v3")
            .withProperty("management.endpoints.web.base-path", "/actuator")

        val target = ServletWarmupTarget.resolve(
            port = 8080,
            environment = environment,
            properties = StartupWarmupProperties(),
        )

        assertEquals("http://127.0.0.1:8080/api/v3/actuator/health/ping", target.toString())
    }

    @Test
    fun `resolve honors explicit warmup path`() {
        val properties = StartupWarmupProperties().apply {
            servletPath = "/internal/warmup"
        }

        val target = ServletWarmupTarget.resolve(
            port = 8081,
            environment = MockEnvironment(),
            properties = properties,
        )

        assertEquals("http://127.0.0.1:8081/internal/warmup", target.toString())
    }
}
