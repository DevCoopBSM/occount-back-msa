package devcoop.occount.gateway.api.infrastructure

import io.jsonwebtoken.security.Keys
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.Base64
import javax.crypto.SecretKey

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtPropertiesConfig {
    @Bean
    fun jwtSecretKey(jwtProperties: JwtProperties): SecretKey {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtProperties.secretKey))
    }
}
