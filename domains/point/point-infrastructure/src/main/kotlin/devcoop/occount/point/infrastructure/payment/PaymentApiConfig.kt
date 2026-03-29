package devcoop.occount.point.infrastructure.payment

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(PaymentApiProperties::class)
class PaymentApiConfig(
    private val paymentApiProperties: PaymentApiProperties,
) {
    @Bean("paymentApiRestClient")
    fun paymentApiRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl(paymentApiProperties.url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
