package devcoop.occount.payment.infrastructure.watchdog

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("alerts.payment-stuck")
data class StuckPaymentAlertProperties(
    val threshold: Duration = Duration.ofMinutes(1),
    val cooldown: Duration = Duration.ofHours(1),
    val scanLimit: Int = 100,
)
