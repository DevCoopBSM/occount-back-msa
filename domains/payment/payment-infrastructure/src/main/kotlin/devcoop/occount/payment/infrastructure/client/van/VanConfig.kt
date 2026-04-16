package devcoop.occount.payment.infrastructure.client.van

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
@EnableConfigurationProperties(VanProperties::class)
class VanConfig(
    private val vanProperties: VanProperties
) {

    @Bean
    fun vanClient(): RestClient {
        val requestFactory = JdkClientHttpRequestFactory()
        requestFactory.setReadTimeout(Duration.ofSeconds(40))

        return RestClient.builder()
            .baseUrl(vanProperties.url)
            .requestFactory(requestFactory)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
