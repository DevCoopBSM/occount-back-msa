package devcoop.occount.warmup

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.startup-warmup")
class StartupWarmupProperties {
    var enabled: Boolean = true

    var jpaEnabled: Boolean = true
    var jpaRepeat: Int = 10

    var servletEnabled: Boolean = true
    var servletRepeat: Int = 10
    var servletTimeout: Duration = Duration.ofSeconds(10)
    var servletPath: String? = null
    var servletEndpoints: List<WarmupEndpoint> = emptyList()

    var businessEnabled: Boolean = true
    var businessRepeat: Int = 10
}

data class WarmupEndpoint(
    var path: String = "",
    var method: String = "GET",
    var body: String? = null,
    var contentType: String = "application/json",
)
