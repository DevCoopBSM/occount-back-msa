package devcoop.occount.payment.infrastructure.client.pg

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(PgProperties::class)
class PgConfig(
    private val pgProperties: PgProperties
) {

    @Bean
    fun pgClient(): RestClient {
        val requestFactory = JdkClientHttpRequestFactory()
        requestFactory.setReadTimeout(Duration.ofSeconds(40))

        return RestClient.builder()
            .baseUrl(pgProperties.url)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
