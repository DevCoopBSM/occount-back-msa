package devcoop.occount.warmup.servlet

import devcoop.occount.warmup.StartupWarmupConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.context.annotation.Import
import org.springframework.test.context.TestPropertySource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.atomic.AtomicInteger

@ExtendWith(OutputCaptureExtension::class)
@SpringBootTest(
    classes = [ServletStartupWarmupIntegrationTest.TestApp::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
@TestPropertySource(properties = [
    "app.startup-warmup.http-repeat=3",
    "app.startup-warmup.http-endpoints[0].path=/servlet-warmup-probe",
    "app.startup-warmup.http-endpoints[0].method=GET",
    "app.startup-warmup.http-endpoints[0].headers.X-Warmup-Test=servlet",
])
class ServletStartupWarmupIntegrationTest {

    @Test
    fun `servlet warmup invokes configured endpoint and logs completion`(output: CapturedOutput) {
        assertTrue(
            CallCounter.value.get() >= 3,
            "Expected at least 3 hits to /servlet-warmup-probe, got ${CallCounter.value.get()}",
        )
        assertTrue(
            output.all.contains("Servlet startup warmup completed"),
            "Expected 'Servlet startup warmup completed' in logs",
        )
        assertEquals("servlet", LastHeader.value, "Expected custom header to be propagated")
    }

    @SpringBootApplication
    @Import(StartupWarmupConfiguration::class, ServletProbeController::class)
    @EnableConfigurationProperties
    open class TestApp

    @RestController
    open class ServletProbeController {
        @GetMapping("/servlet-warmup-probe")
        fun probe(@RequestHeader(value = "X-Warmup-Test", required = false) header: String?): String {
            CallCounter.value.incrementAndGet()
            if (header != null) LastHeader.value = header
            return "ok"
        }
    }

    object CallCounter {
        val value = AtomicInteger(0)
    }

    object LastHeader {
        @Volatile
        var value: String? = null
    }
}
