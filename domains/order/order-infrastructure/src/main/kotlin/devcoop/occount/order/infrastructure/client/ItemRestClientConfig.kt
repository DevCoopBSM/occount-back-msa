package devcoop.occount.order.infrastructure.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.web.client.RestClient

@Configuration
class ItemRestClientConfig {
    @Bean
    fun itemRestClient(
        @Value("\${services.item.base-url}") baseUrl: String,
    ): RestClient {
        val factory = SimpleClientHttpRequestFactory().apply {
            setConnectTimeout(2_000)
            setReadTimeout(5_000)
        }
        return RestClient.builder()
            .baseUrl(baseUrl)
            .requestFactory(factory)
            .build()
    }
}
