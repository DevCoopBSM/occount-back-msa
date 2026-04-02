package devcoop.occount.member.infrastructure.client.wallet

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import org.springframework.web.client.support.RestClientAdapter
import org.springframework.web.service.invoker.HttpServiceProxyFactory
import org.springframework.web.service.invoker.createClient

@Configuration
class WalletClientConfig {

    @Bean
    fun walletClient(
        @Value("\${internal.wallet.api.base-url}") baseUrl: String,
    ): WalletClient {
        val restClient = RestClient.builder()
            .baseUrl(baseUrl)
            .build()

        val factory = HttpServiceProxyFactory.builder()
            .exchangeAdapter(RestClientAdapter.create(restClient))
            .build()

        return factory.createClient<WalletClient>()
    }
}
