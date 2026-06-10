package devcoop.occount.member.infrastructure.otp

import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("EmailOtpJpaEntity 단위 테스트")
class EmailOtpJpaEntityTest {
    @Test
    @DisplayName("주 생성자로 생성하면 모든 필드가 그대로 보존된다")
    fun `primary constructor preserves all fields`() {
        val expiresAt = Instant.parse("2026-01-01T00:05:00Z")
        val createdAt = Instant.parse("2026-01-01T00:00:00Z")

        val entity = EmailOtpJpaEntity(
            email = "test@test.com",
            otpCode = "123456",
            expiresAt = expiresAt,
            verified = true,
            failCount = 3,
            createdAt = createdAt,
        )

        assertEquals("test@test.com", entity.email)
        assertEquals("123456", entity.otpCode)
        assertEquals(expiresAt, entity.expiresAt)
        assertEquals(true, entity.verified)
        assertEquals(3, entity.failCount)
        assertEquals(createdAt, entity.createdAt)
    }

    @Test
    @DisplayName("JPA용 protected 기본 생성자로도 인스턴스를 생성할 수 있다")
    fun `protected no-arg constructor is usable for JPA`() {
        val constructor = EmailOtpJpaEntity::class.java.getDeclaredConstructor()
        constructor.isAccessible = true

        val entity = constructor.newInstance()

        assertEquals("", entity.email)
        assertEquals("", entity.otpCode)
        assertEquals(Instant.EPOCH, entity.expiresAt)
        assertEquals(false, entity.verified)
        assertEquals(0, entity.failCount)
    }
}
