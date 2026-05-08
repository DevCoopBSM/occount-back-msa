package devcoop.occount.payment.infrastructure.watchdog

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(AlertProperties::class, StuckPaymentAlertProperties::class)
@ConditionalOnProperty(prefix = "alerts", name = ["webhook-url"])
class StuckPaymentWatchdogConfig
