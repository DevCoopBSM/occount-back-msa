package devcoop.occount.warmup

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.mock.env.MockEnvironment

class ServletWarmupTargetTest {
    @Test
    fun `defaultEndpoint uses context path and actuator base path by default`() {
        val environment = MockEnvironment()
            .withProperty("server.servlet.context-path", "/api/v3")
            .withProperty("management.endpoints.web.base-path", "/actuator")

        val endpoint = ServletWarmupTargets.defaultEndpoint(environment, StartupWarmupProperties())

        assertEquals("/api/v3/actuator/health/ping", endpoint.path)
        assertEquals("GET", endpoint.method)
    }

    @Test
    fun `resolveBase returns scheme and port only`() {
        val base = ServletWarmupTargets.resolveBase(8080, MockEnvironment())
        assertEquals("http://127.0.0.1:8080", base)
    }

    @Test
    fun `resolveBase uses https when ssl is enabled`() {
        val environment = MockEnvironment()
            .withProperty("server.ssl.enabled", "true")

        val base = ServletWarmupTargets.resolveBase(8443, environment)
        assertEquals("https://127.0.0.1:8443", base)
    }

    @Test
    fun `defaultEndpoint honors explicit warmup path`() {
        val properties = StartupWarmupProperties().apply {
            servletPath = "/internal/warmup"
        }

        val endpoint = ServletWarmupTargets.defaultEndpoint(MockEnvironment(), properties)

        assertEquals("/internal/warmup", endpoint.path)
    }
}
