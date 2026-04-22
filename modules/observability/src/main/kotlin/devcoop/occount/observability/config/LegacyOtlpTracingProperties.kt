package devcoop.occount.observability.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("management.otlp.tracing")
class LegacyOtlpTracingProperties {
    var endpoint: String? = null
    var connectTimeout: Duration? = null
    var timeout: Duration? = null
    var compression: String? = null
    var headers: Map<String, String> = emptyMap()
}
