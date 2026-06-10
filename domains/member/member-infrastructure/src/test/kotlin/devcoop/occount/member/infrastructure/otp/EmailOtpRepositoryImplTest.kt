package devcoop.occount.member.infrastructure.otp

import devcoop.occount.member.application.otp.EmailOtp
import java.time.Instant
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

@DisplayName("EmailOtpRepositoryImpl 단위 테스트")
class EmailOtpRepositoryImplTest {
    private lateinit var jpaRepository: EmailOtpJpaRepository
    private lateinit var repository: EmailOtpRepositoryImpl

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = any<Any>() as T

    // Kotlin non-null Instant 파라미터에 안전하게 쓸 수 있는 any() 매처
    private fun anyInstant(): Instant = any(Instant::class.java) ?: Instant.EPOCH

    private val now = Instant.parse("2026-01-01T00:00:00Z")

    private fun domain() = EmailOtp(
        email = "test@test.com",
        otpCode = "123456",
        expiresAt = now.plusSeconds(300),
        verified = true,
        failCount = 2,
        createdAt = now,
    )

    private fun entity() = EmailOtpJpaEntity(
        email = "test@test.com",
        otpCode = "123456",
        expiresAt = now.plusSeconds(300),
        verified = true,
        failCount = 2,
        createdAt = now,
    )

    @BeforeEach
    fun setUp() {
        jpaRepository = mock(EmailOtpJpaRepository::class.java)
        repository = EmailOtpRepositoryImpl(jpaRepository)
    }

    @Test
    @DisplayName("save는 엔티티로 변환해 저장하고 도메인으로 되돌려 반환한다")
    fun `save maps to entity and back to domain`() {
        `when`(jpaRepository.save(anyArg<EmailOtpJpaEntity>())).thenReturn(entity())

        val result = repository.save(domain())

        assertEquals("test@test.com", result.email)
        assertEquals("123456", result.otpCode)
        assertEquals(true, result.verified)
        assertEquals(2, result.failCount)
        assertEquals(now, result.createdAt)
        verify(jpaRepository).save(anyArg())
    }

    @Test
    @DisplayName("findByEmail은 엔티티가 있으면 도메인을 반환한다")
    fun `findByEmail returns domain when entity exists`() {
        `when`(jpaRepository.findByEmail("test@test.com")).thenReturn(entity())

        assertEquals("test@test.com", repository.findByEmail("test@test.com")?.email)
    }

    @Test
    @DisplayName("findByEmail은 엔티티가 없으면 null을 반환한다")
    fun `findByEmail returns null when entity absent`() {
        `when`(jpaRepository.findByEmail("none@test.com")).thenReturn(null)

        assertNull(repository.findByEmail("none@test.com"))
    }

    @Test
    @DisplayName("findByEmailForUpdate는 비관적 락 조회 결과를 도메인으로 반환한다")
    fun `findByEmailForUpdate returns domain`() {
        `when`(jpaRepository.findByEmailForUpdate("test@test.com")).thenReturn(entity())

        assertEquals("test@test.com", repository.findByEmailForUpdate("test@test.com")?.email)
    }

    @Test
    @DisplayName("findByEmailForUpdate는 결과가 없으면 null을 반환한다")
    fun `findByEmailForUpdate returns null when absent`() {
        `when`(jpaRepository.findByEmailForUpdate("none@test.com")).thenReturn(null)

        assertNull(repository.findByEmailForUpdate("none@test.com"))
    }

    @Test
    @DisplayName("findValidByEmail은 만료되지 않은 OTP를 조회해 도메인으로 반환한다")
    fun `findValidByEmail returns domain`() {
        `when`(jpaRepository.findByEmailAndExpiresAtAfter(anyString(), anyInstant()))
            .thenReturn(entity())

        assertEquals("test@test.com", repository.findValidByEmail("test@test.com")?.email)
    }

    @Test
    @DisplayName("findValidByEmail은 유효한 OTP가 없으면 null을 반환한다")
    fun `findValidByEmail returns null when absent`() {
        `when`(jpaRepository.findByEmailAndExpiresAtAfter(anyString(), anyInstant()))
            .thenReturn(null)

        assertNull(repository.findValidByEmail("none@test.com"))
    }

    @Test
    @DisplayName("deleteByEmail은 JPA 레포지토리에 위임한다")
    fun `deleteByEmail delegates to jpa repository`() {
        repository.deleteByEmail("test@test.com")

        verify(jpaRepository).deleteByEmail("test@test.com")
    }
}
