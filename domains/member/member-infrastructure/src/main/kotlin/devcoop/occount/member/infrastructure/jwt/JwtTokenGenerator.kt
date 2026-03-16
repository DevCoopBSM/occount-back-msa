package devcoop.occount.member.infrastructure.jwt

import devcoop.occount.member.application.auth.TokenGenerator
import io.jsonwebtoken.Jwts
import org.springframework.stereotype.Component
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtTokenGenerator(
    private val jwtSecretKey: SecretKey,
    private val jwtProperties: JwtProperties,
) : TokenGenerator {
    override fun createAccessToken(userId: Long, role: String): String {
        val now = Date()
        return Jwts.builder()
            .signWith(jwtSecretKey)
            .subject(userId.toString())
            .claim("role", role)
            .claim("tokenType", "ACCESS_TOKEN")
            .issuedAt(now)
            .expiration(Date(now.time + jwtProperties.accessExpirationTime))
            .compact()
    }

    override fun createKioskToken(userId: Long, role: String): String {
        val now = Date()
        return Jwts.builder()
            .signWith(jwtSecretKey)
            .subject(userId.toString())
            .claim("role", role)
            .claim("tokenType", "KIOSK_TOKEN")
            .issuedAt(now)
            .expiration(Date(now.time + jwtProperties.kioskExpirationTime))
            .compact()
    }
}
