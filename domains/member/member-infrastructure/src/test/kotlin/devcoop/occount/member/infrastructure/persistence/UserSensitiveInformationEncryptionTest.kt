package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.usecase.register.MemberRegisterRequest
import devcoop.occount.member.application.usecase.register.RegisterUserUseCase
import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.UserType
import devcoop.occount.member.infrastructure.crypto.CryptoConfig
import devcoop.occount.member.infrastructure.crypto.CryptoHelper
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestPropertySource

@DataJpaTest
@TestPropertySource(properties = ["app.encryption.secret-key=12345678901234567890123456789012"])
@DisplayName("회원 민감정보 암호화 JPA 통합 테스트")
class UserSensitiveInformationEncryptionTest @Autowired constructor(
    private val userJpaRepository: UserJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val entityManager: EntityManager,
) {
    @Test
    @DisplayName("Entity 저장 후 DB raw 값은 암호문이고 JPA 조회 값은 복호화된 평문이다")
    fun `jpa encrypts raw values and decrypts entity values`() {
        val username = "홍길동"
        val phone = "010-1234-5678"
        val ciNumber = "CI123456"
        val savedEntity = userJpaRepository.saveAndFlush(
            UserJpaEntity(
                username = username,
                phone = phone,
                userBarcode = "BARCODE123",
                userType = UserType.STUDENT,
                cooperativeNumber = null,
                email = "test@test.com",
                password = "encodedPassword",
                role = Role.ROLE_USER,
                pin = "encodedPin",
                userCiNumber = ciNumber,
            ),
        )

        val rawValues = jdbcTemplate.queryForMap(
            "select username, phone, user_ci_number from common_user where id = ?",
            savedEntity.id,
        )
        entityManager.clear()

        assertEncrypted(rawValues["username"], username)
        assertEncrypted(rawValues["phone"], phone)
        assertEncrypted(rawValues["user_ci_number"], ciNumber)

        val foundEntity = userJpaRepository.findById(savedEntity.id).orElseThrow()
        assertEquals(username, foundEntity.username)
        assertEquals(phone, foundEntity.phone)
        assertEquals(ciNumber, foundEntity.userCiNumber)
    }

    @Test
    @DisplayName("회원가입 use case 저장 후 DB raw 값은 암호문이고 repository 조회 값은 복호화된 평문이다")
    fun `register use case stores encrypted sensitive information`() {
        val userRepository = UserRepositoryImpl(userJpaRepository)
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = userRepository,
            eventPublisher = NoOpEventPublisher(),
            passwordEncoder = TestPasswordEncoder(),
            defaultPin = "000000",
        )
        val username = "가입자"
        val phone = "010-9999-8888"
        val ciNumber = "CI-REGISTER-123"

        registerUserUseCase.register(
            MemberRegisterRequest(
                userCiNumber = ciNumber,
                username = username,
                userPhone = phone,
                userEmail = "register@test.com",
                password = "password1234",
            ),
        )
        entityManager.flush()

        val rawValues = jdbcTemplate.queryForMap(
            "select username, phone, user_ci_number from common_user where email = ?",
            "register@test.com",
        )
        entityManager.clear()

        assertEncrypted(rawValues["username"], username)
        assertEncrypted(rawValues["phone"], phone)
        assertEncrypted(rawValues["user_ci_number"], ciNumber)

        val foundUser = userRepository.findByEmail("register@test.com")
        assertEquals(username, foundUser!!.getUsername())
        assertEquals(phone, foundUser.getPhone())
        assertEquals(ciNumber, foundUser.getCiNumber())
    }

    @Test
    @DisplayName("같은 전화번호와 CI도 랜덤 IV 때문에 서로 다른 암호문으로 저장된다")
    fun `same sensitive values are stored as different ciphertexts`() {
        val phone = "010-1111-2222"
        val ciNumber = "CI-SAME-123"
        val firstEntity = userJpaRepository.saveAndFlush(
            UserJpaEntity(
                username = "첫번째",
                phone = phone,
                userBarcode = "BARCODE-FIRST",
                userType = UserType.STUDENT,
                cooperativeNumber = null,
                email = "first@test.com",
                password = "encodedPassword",
                role = Role.ROLE_USER,
                pin = "encodedPin",
                userCiNumber = ciNumber,
            ),
        )
        val secondEntity = userJpaRepository.saveAndFlush(
            UserJpaEntity(
                username = "두번째",
                phone = phone,
                userBarcode = "BARCODE-SECOND",
                userType = UserType.STUDENT,
                cooperativeNumber = null,
                email = "second@test.com",
                password = "encodedPassword",
                role = Role.ROLE_USER,
                pin = "encodedPin",
                userCiNumber = ciNumber,
            ),
        )

        val rawRows = jdbcTemplate.queryForList(
            "select phone, user_ci_number from common_user where id in (?, ?) order by id",
            firstEntity.id,
            secondEntity.id,
        )
        entityManager.clear()

        assertEquals(2, rawRows.size)
        assertEncrypted(rawRows[0]["phone"], phone)
        assertEncrypted(rawRows[1]["phone"], phone)
        assertNotEquals(rawRows[0]["phone"], rawRows[1]["phone"])
        assertEncrypted(rawRows[0]["user_ci_number"], ciNumber)
        assertEncrypted(rawRows[1]["user_ci_number"], ciNumber)
        assertNotEquals(rawRows[0]["user_ci_number"], rawRows[1]["user_ci_number"])

        val firstFoundEntity = userJpaRepository.findById(firstEntity.id).orElseThrow()
        val secondFoundEntity = userJpaRepository.findById(secondEntity.id).orElseThrow()
        assertEquals(phone, firstFoundEntity.phone)
        assertEquals(phone, secondFoundEntity.phone)
        assertEquals(ciNumber, firstFoundEntity.userCiNumber)
        assertEquals(ciNumber, secondFoundEntity.userCiNumber)
    }

    private fun assertEncrypted(rawValue: Any?, plainText: String) {
        val rawText = rawValue as String
        assertNotEquals(plainText, rawText)
        assertTrue(rawText.startsWith(CryptoHelper.ENCRYPTION_PREFIX))
    }

    private class NoOpEventPublisher : EventPublisher {
        override fun publish(topic: String, key: String, eventType: String, payload: Any) = Unit
    }

    private class TestPasswordEncoder : PasswordEncoder {
        override fun encode(rawPassword: CharSequence?): String = "encoded:$rawPassword"

        override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
            return encode(rawPassword) == encodedPassword
        }

        override fun upgradeEncoding(encodedPassword: String?): Boolean = false
    }
}

@SpringBootConfiguration
@EnableAutoConfiguration
@EntityScan(basePackageClasses = [UserJpaEntity::class])
@EnableJpaRepositories(basePackageClasses = [UserJpaRepository::class])
@Import(CryptoConfig::class)
class UserSensitiveInformationEncryptionTestConfig
