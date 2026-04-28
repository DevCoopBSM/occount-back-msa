package devcoop.occount.warmup

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.startup-warmup")
class StartupWarmupProperties {
    var enabled: Boolean = true

    var jpaEnabled: Boolean = true
    var jpaRepeat: Int = 10

    var httpEnabled: Boolean = true
    var httpRepeat: Int = 10
    var httpTimeout: Duration = Duration.ofSeconds(10)
    var httpPath: String? = null
    var httpEndpoints: List<WarmupEndpoint> = emptyList()

    var businessEnabled: Boolean = true
    var businessRepeat: Int = 10
}

data class WarmupEndpoint(
    var path: String = "",
    var method: String = "GET",
    var body: String? = null,
    var contentType: String = "application/json",
    var headers: Map<String, String> = emptyMap(),
)