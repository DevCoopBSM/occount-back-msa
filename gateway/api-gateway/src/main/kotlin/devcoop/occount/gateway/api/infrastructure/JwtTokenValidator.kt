package devcoop.occount.gateway.api.infrastructure

import org.springframework.stereotype.Component

@Component
class JwtTokenValidator(
    private val jwtProperties: JwtProperties,
) {
    fun validateCredential(credential: String?) {
        if (credential.isNullOrBlank() || !credential.startsWith(jwtProperties.prefix)) {
            throw InvalidTokenException()
        }
    }

    fun stripPrefix(credential: String): String {
        val token = credential.removePrefix(jwtProperties.prefix).trim()
        if (token.isBlank()) {
            throw InvalidTokenException()
        }
        return token
    }
}
