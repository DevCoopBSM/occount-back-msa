package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.*
import devcoop.occount.member.infrastructure.crypto.SensitiveInformationHash
import devcoop.occount.member.infrastructure.crypto.SensitiveInformationHasher
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.util.Optional

@DisplayName("UserRepositoryImpl 단위 테스트")
class UserRepositoryImplTest {

    private lateinit var userJpaRepository: UserJpaRepository
    private lateinit var userRepositoryImpl: UserRepositoryImpl

    // Kotlin + 순수 Mockito에서 non-null 타입에 any() 매처를 사용하기 위한 헬퍼
    @Suppress("UNCHECKED_CAST")
    private fun <T> anyArg(): T = any<Any>() as T

    // Kotlin non-null 파라미터에 eq 매처를 안전하게 쓰기 위한 헬퍼 (eq는 null을 반환하므로 원본값으로 폴백)
    private fun <T> eqArg(value: T): T = eq(value) ?: value

    @BeforeEach
    fun setUp() {
        SensitiveInformationHash.configure(SensitiveInformationHasher("12345678901234567890123456789012"))
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
        email = "test@test.com",
        password = "encodedPassword",
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
        `when`(userJpaRepository.findByEmail("test@test.com")).thenReturn(entity)

        val result = userRepositoryImpl.findByEmail("test@test.com")

        assertNotNull(result)
        assertEquals("test@test.com", result!!.getEmail())
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회하면 null을 반환한다")
    fun `findByUserEmail returns null when entity not found`() {
        `when`(userJpaRepository.findByEmail("notfound@test.com")).thenReturn(null)

        val result = userRepositoryImpl.findByEmail("notfound@test.com")

        assertNull(result)
    }

    @Test
    @DisplayName("유저 저장 시 JPA 저장 후 도메인 객체로 변환하여 반환한다")
    fun `save stores entity and returns domain user`() {
        val domainToSave = User(
            userInfo = UserInfo("홍길동", "010-1234-5678", UserType.STUDENT, null, null, null),
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
        `when`(userJpaRepository.existsByEmail("test@test.com")).thenReturn(true)

        val result = userRepositoryImpl.existsByEmail("test@test.com")

        assertTrue(result)
        verify(userJpaRepository).existsByEmail("test@test.com")
    }

    @Test
    @DisplayName("키워드 검색 시 JPA 레포지토리에 위임하고 도메인 객체로 변환한다")
    fun `searchByKeyword delegates to JPA repository and maps to domain`() {
        val pageable = PageRequest.of(0, 10)
        val page = PageImpl(listOf(createEntity(id = 1L)), pageable, 1L)
        `when`(userJpaRepository.searchByKeyword(eqArg("홍길동"), anyArg())).thenReturn(page)

        val result = userRepositoryImpl.searchByKeyword("홍길동", pageable)

        assertEquals(1, result.content.size)
        assertEquals("홍길동", result.content.first().getUsername())
        verify(userJpaRepository).searchByKeyword(eqArg("홍길동"), anyArg())
    }

    @Test
    @DisplayName("키워드의 LIKE 메타문자(% _ 백슬래시)를 이스케이프해 위임한다")
    fun `searchByKeyword escapes like wildcards before delegating`() {
        val pageable = PageRequest.of(0, 10)
        `when`(userJpaRepository.searchByKeyword(anyArg(), anyArg()))
            .thenReturn(PageImpl(emptyList(), pageable, 0L))

        userRepositoryImpl.searchByKeyword("a%b_c\\d", pageable)

        verify(userJpaRepository).searchByKeyword(eqArg("a\\%b\\_c\\\\d"), anyArg())
    }
}
