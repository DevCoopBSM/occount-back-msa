package devcoop.occount.payment.infrastructure.client.pg.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(PgProperties::class)
class PgConfig
