package devcoop.occount.gateway.api.infrastructure

import devcoop.occount.gateway.api.application.AuthenticatedUser
import devcoop.occount.gateway.api.application.TokenAuthenticator
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import javax.crypto.SecretKey

@Component
class JwtTokenAuthenticator(
    private val jwtSecretKey: SecretKey,
    private val jwtTokenValidator: JwtTokenValidator,
) : TokenAuthenticator {
    override fun authenticate(authorizationHeader: String?): AuthenticatedUser {
        jwtTokenValidator.validateCredential(authorizationHeader)
        val token = jwtTokenValidator.stripPrefix(authorizationHeader!!)
        val claims = getTokenBody(token)
        return buildAuthPrincipal(claims)
    }

    private fun buildAuthPrincipal(claims: Claims): AuthenticatedUser {
        val userId = claims.subject?.toLongOrNull() ?: throw InvalidTokenException()
        val role = claims["role"]?.toString()?.takeIf { it.isNotBlank() } ?: throw InvalidTokenException()
        return AuthenticatedUser(userId, role)
    }

    private fun getTokenBody(token: String): Claims {
        return try {
            Jwts.parser()
                .verifyWith(jwtSecretKey)
                .build()
                .parseSignedClaims(token)
                .payload
        } catch (_: ExpiredJwtException) {
            throw ExpiredTokenException()
        } catch (_: IllegalArgumentException) {
            throw InvalidTokenException()
        } catch (_: JwtException) {
            throw InvalidTokenException()
        }
    }
}
