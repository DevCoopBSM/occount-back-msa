package devcoop.occount.payment.infrastructure.member

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(MemberReadApiProperties::class)
class MemberReadApiConfig(
    private val memberReadApiProperties: MemberReadApiProperties,
) {
    @Bean
    fun memberReadApiRestClient(): RestClient {
        return RestClient.builder()
            .baseUrl(memberReadApiProperties.url)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .build()
    }
}
