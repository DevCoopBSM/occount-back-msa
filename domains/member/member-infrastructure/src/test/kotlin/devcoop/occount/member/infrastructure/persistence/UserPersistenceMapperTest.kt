package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.member.domain.user.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UserPersistenceMapper 단위 테스트")
class UserPersistenceMapperTest {

    private fun createEntity(id: Long = 1L) = UserJpaEntity(
        id = id,
        username = "홍길동",
        phone = "010-1234-5678",
        userBarcode = "BARCODE123",
        userType = UserType.STUDENT,
        cooperativeNumber = "COOP001",
        email = "test@test.com",
        password = "encodedPassword",
        role = Role.ROLE_USER,
        pin = "encodedPin",
        userCiNumber = "CI123456",
    )

    private fun createDomain(id: Long = 1L) = User(
        id = id,
        userInfo = UserInfo(
            username = "홍길동",
            phone = "010-1234-5678",
            userType = UserType.STUDENT,
            cooperativeNumber = "COOP001",
            userBarcode = "BARCODE123",
        ),
        accountInfo = AccountInfo(
            email = "test@test.com",
            password = "encodedPassword",
            role = Role.ROLE_USER,
            pin = "encodedPin",
        ),
        userSensitiveInfo = UserSensitiveInfo(ciNumber = "CI123456"),
    )

    @Test
    @DisplayName("JPA 엔티티를 도메인 객체로 변환하면 모든 필드가 올바르게 매핑된다")
    fun `toDomain maps all fields from entity to domain`() {
        val entity = createEntity(id = 5L)

        val domain = UserPersistenceMapper.toDomain(entity)

        assertEquals(5L, domain.getId())
        assertEquals("홍길동", domain.getUsername())
        assertEquals("010-1234-5678", domain.getPhone())
        assertEquals("BARCODE123", domain.getUserBarcode())
        assertEquals(UserType.STUDENT, domain.getUserType())
        assertEquals("COOP001", domain.getCooperativeNumber())
        assertEquals("test@test.com", domain.getEmail())
        assertEquals("encodedPassword", domain.getPassword())
        assertEquals(Role.ROLE_USER, domain.getRole())
        assertEquals("encodedPin", domain.getUserPin())
        assertEquals("CI123456", domain.getCiNumber())
    }

    @Test
    @DisplayName("도메인 객체를 JPA 엔티티로 변환하면 모든 필드가 올바르게 매핑된다")
    fun `toEntity maps all fields from domain to entity`() {
        val domain = createDomain(id = 5L)

        val entity = UserPersistenceMapper.toEntity(domain)

        assertEquals(5L, entity.id)
        assertEquals("홍길동", entity.username)
        assertEquals("010-1234-5678", entity.phone)
        assertEquals("BARCODE123", entity.userBarcode)
        assertEquals(UserType.STUDENT, entity.userType)
        assertEquals("COOP001", entity.cooperativeNumber)
        assertEquals("test@test.com", entity.email)
        assertEquals("encodedPassword", entity.password)
        assertEquals(Role.ROLE_USER, entity.role)
        assertEquals("encodedPin", entity.pin)
        assertEquals("CI123456", entity.userCiNumber)
    }

    @Test
    @DisplayName("nullable 필드가 null인 엔티티를 도메인으로 변환해도 null이 유지된다")
    fun `toDomain preserves null fields`() {
        val entity = UserJpaEntity(
            id = 1L,
            username = "홍길동",
            phone = null,
            userBarcode = null,
            userType = UserType.STUDENT,
            cooperativeNumber = null,
            email = "test@test.com",
            password = "encodedPassword",
            role = Role.ROLE_USER,
            pin = "encodedPin",
            userCiNumber = null,
        )

        val domain = UserPersistenceMapper.toDomain(entity)

        assertNull(domain.getPhone())
        assertNull(domain.getUserBarcode())
        assertNull(domain.getCooperativeNumber())
        assertNull(domain.getCiNumber())
    }
}
