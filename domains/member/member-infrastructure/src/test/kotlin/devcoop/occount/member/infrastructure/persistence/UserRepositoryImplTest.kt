package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import java.util.Optional

@DisplayName("UserRepositoryImpl 단위 테스트")
class UserRepositoryImplTest {

    private lateinit var userJpaRepository: UserJpaRepository
    private lateinit var userRepositoryImpl: UserRepositoryImpl

    // Kotlin + 순수 Mockito에서 non-null 타입에 any() 매처를 사용하기 위한 헬퍼
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = any<Any>() as T

    @BeforeEach
    fun setUp() {
        userJpaRepository = mock(UserJpaRepository::class.java)
        userRepositoryImpl = UserRepositoryImpl(userJpaRepository)
    }

    private fun createEntity(id: Long = 1L) = UserJpaEntity(
        id = id,
        username = "홍길동",
        phone = "010-1234-5678",
        userBarcode = "BARCODE123",
        userType = UserType.STUDENT,
        cooperativeNumber = null,
        userEmail = "test@test.com",
        userPassword = "encodedPassword",
        role = Role.ROLE_USER,
        pin = "encodedPin",
        userCiNumber = "CI123456",
    )

    @Test
    @DisplayName("존재하는 id로 조회하면 도메인 객체를 반환한다")
    fun `findById returns domain user when entity exists`() {
        val entity = createEntity(id = 1L)
        `when`(userJpaRepository.findById(1L)).thenReturn(Optional.of(entity))

        val result = userRepositoryImpl.findById(1L)

        assertNotNull(result)
        assertEquals(1L, result!!.getId())
        assertEquals("홍길동", result.getUsername())
    }

    @Test
    @DisplayName("존재하지 않는 id로 조회하면 null을 반환한다")
    fun `findById returns null when entity not found`() {
        `when`(userJpaRepository.findById(999L)).thenReturn(Optional.empty())

        val result = userRepositoryImpl.findById(999L)

        assertNull(result)
    }

    @Test
    @DisplayName("존재하는 바코드로 조회하면 도메인 객체를 반환한다")
    fun `findByUserBarcode returns domain user when entity exists`() {
        val entity = createEntity()
        `when`(userJpaRepository.findByUserBarcode("BARCODE123")).thenReturn(entity)

        val result = userRepositoryImpl.findByUserBarcode("BARCODE123")

        assertNotNull(result)
        assertEquals("BARCODE123", result!!.getUserBarcode())
    }

    @Test
    @DisplayName("존재하지 않는 바코드로 조회하면 null을 반환한다")
    fun `findByUserBarcode returns null when entity not found`() {
        `when`(userJpaRepository.findByUserBarcode("INVALID")).thenReturn(null)

        val result = userRepositoryImpl.findByUserBarcode("INVALID")

        assertNull(result)
    }

    @Test
    @DisplayName("존재하는 이메일로 조회하면 도메인 객체를 반환한다")
    fun `findByUserEmail returns domain user when entity exists`() {
        val entity = createEntity()
        `when`(userJpaRepository.findByUserEmail("test@test.com")).thenReturn(entity)

        val result = userRepositoryImpl.findByUserEmail("test@test.com")

        assertNotNull(result)
        assertEquals("test@test.com", result!!.getEmail())
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 null을 반환한다")
    fun `findByUserEmail returns null when entity not found`() {
        `when`(userJpaRepository.findByUserEmail("notfound@test.com")).thenReturn(null)

        val result = userRepositoryImpl.findByUserEmail("notfound@test.com")

        assertNull(result)
    }

    @Test
    @DisplayName("유저 저장 시 JPA 저장 후 도메인 객체로 변환하여 반환한다")
    fun `save stores entity and returns domain user`() {
        val domainToSave = User(
            userInfo = UserInfo("홍길동", "010-1234-5678", UserType.STUDENT, null, null),
            accountInfo = AccountInfo("test@test.com", "encodedPassword", Role.ROLE_USER, "encodedPin"),
            userSensitiveInfo = UserSensitiveInfo("CI123456"),
        )
        val savedEntity = createEntity(id = 10L)
        `when`(userJpaRepository.save(anyArg<UserJpaEntity>())).thenReturn(savedEntity)

        val result = userRepositoryImpl.save(domainToSave)

        assertEquals(10L, result.getId())
        verify(userJpaRepository).save(anyArg())
    }

    @Test
    @DisplayName("이메일 존재 여부 확인 시 JPA 레포지토리에 위임한다")
    fun `existsByUserEmail delegates to JPA repository`() {
        `when`(userJpaRepository.existsByUserEmail("test@test.com")).thenReturn(true)

        val result = userRepositoryImpl.existsByUserEmail("test@test.com")

        assertTrue(result)
        verify(userJpaRepository).existsByUserEmail("test@test.com")
    }
}
