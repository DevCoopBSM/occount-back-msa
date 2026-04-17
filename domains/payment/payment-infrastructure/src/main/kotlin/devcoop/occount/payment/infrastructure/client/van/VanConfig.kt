package devcoop.occount.payment.infrastructure.client.van

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(VanProperties::class)
class VanConfig
