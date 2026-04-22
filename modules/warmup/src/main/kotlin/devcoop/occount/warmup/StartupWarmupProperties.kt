package devcoop.occount.warmup

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("app.startup-warmup")
class StartupWarmupProperties {
    var enabled: Boolean = true
    var jpaEnabled: Boolean = true
    var servletEnabled: Boolean = true
    var servletPath: String? = null
    var servletTimeout: Duration = Duration.ofSeconds(3)
}
