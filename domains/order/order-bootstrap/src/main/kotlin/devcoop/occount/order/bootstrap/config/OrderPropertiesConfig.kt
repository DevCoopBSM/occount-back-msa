package devcoop.occount.order.bootstrap.config

import devcoop.occount.order.application.config.OrderTimeoutConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(OrderProperties::class)
class OrderPropertiesConfig {
    @Bean
    fun orderTimeoutConfig(orderProperties: OrderProperties): OrderTimeoutConfig {
        return OrderTimeoutConfig(
            timeoutSeconds = orderProperties.timeoutSeconds,
            asyncTimeoutBufferMillis = orderProperties.asyncTimeoutBufferMillis,
        )
    }
}
