package devcoop.occount.payment.infrastructure.point

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(PointApiProperties::class)
class PointApiConfig(
    private val pointApiProperties: PointApiProperties,
) {
    @Bean
    fun pointApiRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl(pointApiProperties.url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
