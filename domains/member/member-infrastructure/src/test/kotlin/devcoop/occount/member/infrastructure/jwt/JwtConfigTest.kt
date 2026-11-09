package devcoop.occount.member.infrastructure.jwt

import java.util.Base64
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("JWT 설정/프로퍼티 단위 테스트")
class JwtConfigTest {
    @Test
    @DisplayName("JwtProperties는 모든 설정 값을 보존한다")
    fun `JwtProperties preserves all values`() {
        val properties = JwtProperties(
            accessExpirationTime = 1000L,
            kioskExpirationTime = 2000L,
            prefix = "Bearer ",
            header = "Authorization",
            secretKey = "secret",
        )

        assertEquals(1000L, properties.accessExpirationTime)
        assertEquals(2000L, properties.kioskExpirationTime)
        assertEquals("Bearer ", properties.prefix)
        assertEquals("Authorization", properties.header)
        assertEquals("secret", properties.secretKey)
    }

    @Test
    @DisplayName("jwtSecretKey 빈은 Base64 시크릿으로 HMAC SecretKey를 생성한다")
    fun `jwtSecretKey bean builds an HMAC key from base64 secret`() {
        val base64Secret = Base64.getEncoder().encodeToString(ByteArray(32) { 1 })
        val properties = JwtProperties(
            accessExpirationTime = 1000L,
            kioskExpirationTime = 2000L,
            prefix = "Bearer ",
            header = "Authorization",
            secretKey = base64Secret,
        )

        val secretKey = JwtPropertiesConfig().jwtSecretKey(properties)

        assertNotNull(secretKey)
        assertEquals("HmacSHA256", secretKey.algorithm)
    }
}
