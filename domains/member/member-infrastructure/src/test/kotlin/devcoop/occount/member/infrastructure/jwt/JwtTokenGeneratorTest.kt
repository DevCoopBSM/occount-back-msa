package devcoop.occount.member.infrastructure.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.Base64
import javax.crypto.SecretKey

@DisplayName("JwtTokenGenerator 단위 테스트")
class JwtTokenGeneratorTest {

    private lateinit var secretKey: SecretKey
    private lateinit var jwtProperties: JwtProperties
    private lateinit var jwtTokenGenerator: JwtTokenGenerator

    @BeforeEach
    fun setUp() {
        // 256비트(32바이트) 테스트용 시크릿 키 생성
        val rawKey = "test-secret-key-for-hs256-32byte".toByteArray(Charsets.UTF_8)
        secretKey = Keys.hmacShaKeyFor(rawKey)
        jwtProperties = JwtProperties(
            accessExpirationTime = 3_600_000L,   // 1시간
            kioskExpirationTime = 86_400_000L,   // 24시간
            prefix = "Bearer",
            header = "Authorization",
            secretKey = Base64.getEncoder().encodeToString(rawKey),
        )
        jwtTokenGenerator = JwtTokenGenerator(secretKey, jwtProperties)
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    @Test
    @DisplayName("createAccessToken 호출 시 유효한 JWT 토큰을 반환하며 tokenType이 ACCESS_TOKEN이다")
    fun `createAccessToken returns valid JWT with ACCESS_TOKEN type`() {
        val token = jwtTokenGenerator.createAccessToken(userId = 1L, role = "ROLE_USER")

        assertNotNull(token)
        assertTrue(token.isNotBlank())

        val claims = parseClaims(token)
        assertEquals("1", claims.subject)
        assertEquals("ROLE_USER", claims["role"])
        assertEquals("ACCESS_TOKEN", claims["tokenType"])
    }

    @Test
    @DisplayName("createKioskToken 호출 시 유효한 JWT 토큰을 반환하며 tokenType이 KIOSK_TOKEN이다")
    fun `createKioskToken returns valid JWT with KIOSK_TOKEN type`() {
        val token = jwtTokenGenerator.createKioskToken(userId = 2L, role = "ROLE_MEMBER")

        assertNotNull(token)
        assertTrue(token.isNotBlank())

        val claims = parseClaims(token)
        assertEquals("2", claims.subject)
        assertEquals("ROLE_MEMBER", claims["role"])
        assertEquals("KIOSK_TOKEN", claims["tokenType"])
    }

    @Test
    @DisplayName("createAccessToken과 createKioskToken이 반환하는 토큰은 서로 다르다")
    fun `accessToken and kioskToken are different`() {
        val accessToken = jwtTokenGenerator.createAccessToken(userId = 1L, role = "ROLE_USER")
        val kioskToken = jwtTokenGenerator.createKioskToken(userId = 1L, role = "ROLE_USER")

        assertNotEquals(accessToken, kioskToken)
    }
}
