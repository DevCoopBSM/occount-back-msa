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
    fun sensitiveInformationHasher(): SensitiveInformationHasher {
        return SensitiveInformationHasher(secretKey)
    }

    @Bean
    fun cryptoInitializer(
        cryptoHelper: CryptoHelper,
        sensitiveInformationHasher: SensitiveInformationHasher,
    ): CryptoInitializer {
        return CryptoInitializer(cryptoHelper, sensitiveInformationHasher)
    }
}

class CryptoInitializer(
    private val cryptoHelper: CryptoHelper,
    private val sensitiveInformationHasher: SensitiveInformationHasher,
) {

    @PostConstruct
    fun configureCrypto() {
        CryptoConverter.configure(cryptoHelper)
        SensitiveInformationHash.configure(sensitiveInformationHasher)
    }
}
