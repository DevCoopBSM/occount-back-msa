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
    private val cryptoHelper = CryptoHelper(secretKey)
    private val sensitiveInformationHasher = SensitiveInformationHasher(secretKey)

    @Bean
    fun cryptoHelper(): CryptoHelper = cryptoHelper

    @Bean
    fun sensitiveInformationHasher(): SensitiveInformationHasher = sensitiveInformationHasher

    @PostConstruct
    fun configureCrypto() {
        CryptoConverter.configure(cryptoHelper)
        SensitiveInformationHash.configure(sensitiveInformationHasher)
    }
}
