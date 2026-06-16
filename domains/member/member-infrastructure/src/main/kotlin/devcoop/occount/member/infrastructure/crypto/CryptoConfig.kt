package devcoop.occount.member.infrastructure.crypto

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CryptoConfig(
    @param:Value("\${app.encryption.secret-key}")
    private val secretKey: String,
) {
    @Bean
    fun cryptoHelper(): CryptoHelper {
        return CryptoHelper(secretKey)
    }

    @Bean
    fun cryptoInitializer(cryptoHelper: CryptoHelper): CryptoInitializer {
        return CryptoInitializer(cryptoHelper)
    }
}

class CryptoInitializer(
    private val cryptoHelper: CryptoHelper,
) {

    @PostConstruct
    fun configureCrypto() {
        CryptoConverter.configure(cryptoHelper)
    }
}
