package devcoop.occount.item.infrastructure.toss

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient


@Configuration
class TossClientConfig {

    @Bean
    fun tossClient(
        @Value("\${toss.api.url}") baseUrl: String
    ): TossClient {
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build()

        val factory = HttpServiceProxyFactory.builder()
            .exchangeAdapter(RestClientAdapter.create(restClient))
            .build()

        return factory.createClient<TossClient>()
    }
}
