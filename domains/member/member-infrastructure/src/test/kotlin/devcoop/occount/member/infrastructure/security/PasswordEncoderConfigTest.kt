package devcoop.occount.member.infrastructure.security

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@DisplayName("PasswordEncoderConfig 단위 테스트")
class PasswordEncoderConfigTest {
    @Test
    @DisplayName("passwordEncoder 빈은 인코딩/매칭이 동작하는 BCryptPasswordEncoder를 생성한다")
    fun `passwordEncoder bean produces a working BCrypt encoder`() {
        val encoder = PasswordEncoderConfig().passwordEncoder()

        assertTrue(encoder is BCryptPasswordEncoder)
        val encoded = encoder.encode("rawPassword")
        assertTrue(encoder.matches("rawPassword", encoded))
    }
}
