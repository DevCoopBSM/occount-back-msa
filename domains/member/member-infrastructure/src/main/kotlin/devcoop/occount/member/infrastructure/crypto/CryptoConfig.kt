package devcoop.occount.member.infrastructure.crypto

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
class CryptoConfig(
    @param:Value("\${app.encryption.secret-key}")
    private val secretKey: String,
) {
    @PostConstruct
    fun configureConverter() {
        CryptoConverter.configure(CryptoHelper(secretKey))
    }
}
