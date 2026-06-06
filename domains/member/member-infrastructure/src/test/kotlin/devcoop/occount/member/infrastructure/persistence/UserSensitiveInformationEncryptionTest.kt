package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.usecase.register.MemberRegisterRequest
import devcoop.occount.member.application.usecase.register.RegisterUserUseCase
import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.UserType
import devcoop.occount.member.infrastructure.crypto.CryptoConfig
import devcoop.occount.member.infrastructure.crypto.CryptoHelper
import devcoop.occount.member.infrastructure.crypto.SensitiveInformationHasher
import devcoop.occount.member.infrastructure.crypto.SensitiveInformationMigrationRunner
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestPropertySource
import javax.sql.DataSource

@DataJpaTest
@TestPropertySource(properties = ["app.encryption.secret-key=12345678901234567890123456789012"])
@DisplayName("회원 민감정보 암호화 JPA 통합 테스트")
class UserSensitiveInformationEncryptionTest @Autowired constructor(
    private val userJpaRepository: UserJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val entityManager: EntityManager,
    private val dataSource: DataSource,
    private val cryptoHelper: CryptoHelper,
    private val sensitiveInformationHasher: SensitiveInformationHasher,
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
                phoneHash = sensitiveInformationHasher.hash(phone),
                userBarcode = "BARCODE123",
                userType = UserType.STUDENT,
                cooperativeNumber = null,
                email = "test@test.com",
                password = "encodedPassword",
                role = Role.ROLE_USER,
                pin = "encodedPin",
                userCiNumber = ciNumber,
                userCiNumberHash = sensitiveInformationHasher.hash(ciNumber),
            ),
        )

        val rawValues = jdbcTemplate.queryForMap(
            "select username, phone, phone_hash, user_ci_number, user_ci_number_hash from common_user where id = ?",
            savedEntity.id,
        )
        entityManager.clear()

        assertEncrypted(rawValues["username"], username)
        assertEncrypted(rawValues["phone"], phone)
        assertEquals(sensitiveInformationHasher.hash(phone), rawValues["phone_hash"])
        assertEncrypted(rawValues["user_ci_number"], ciNumber)
        assertEquals(sensitiveInformationHasher.hash(ciNumber), rawValues["user_ci_number_hash"])

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
            "select username, phone, phone_hash, user_ci_number, user_ci_number_hash from common_user where email = ?",
            "register@test.com",
        )
        entityManager.clear()

        assertEncrypted(rawValues["username"], username)
        assertEncrypted(rawValues["phone"], phone)
        assertEquals(sensitiveInformationHasher.hash(phone), rawValues["phone_hash"])
        assertEncrypted(rawValues["user_ci_number"], ciNumber)
        assertEquals(sensitiveInformationHasher.hash(ciNumber), rawValues["user_ci_number_hash"])

        val foundUser = userRepository.findByEmail("register@test.com")
        assertEquals(username, foundUser!!.getUsername())
        assertEquals(phone, foundUser.getPhone())
        assertEquals(ciNumber, foundUser.getCiNumber())
    }

    @Test
    @DisplayName("같은 전화번호와 CI는 같은 HMAC 해시가 저장되어 DB unique 제약으로 중복을 막는다")
    fun `same sensitive values are rejected by hash unique constraints`() {
        val phone = "010-1111-2222"
        val ciNumber = "CI-SAME-123"
        userJpaRepository.saveAndFlush(
            UserJpaEntity(
                username = "첫번째",
                phone = phone,
                phoneHash = sensitiveInformationHasher.hash(phone),
                userBarcode = "BARCODE-FIRST",
                userType = UserType.STUDENT,
                cooperativeNumber = null,
                email = "first@test.com",
                password = "encodedPassword",
                role = Role.ROLE_USER,
                pin = "encodedPin",
                userCiNumber = ciNumber,
                userCiNumberHash = sensitiveInformationHasher.hash(ciNumber),
            ),
        )

        assertThrows(DataIntegrityViolationException::class.java) {
            userJpaRepository.saveAndFlush(
                UserJpaEntity(
                    username = "두번째",
                    phone = phone,
                    phoneHash = sensitiveInformationHasher.hash(phone),
                    userBarcode = "BARCODE-SECOND",
                    userType = UserType.STUDENT,
                    cooperativeNumber = null,
                    email = "second@test.com",
                    password = "encodedPassword",
                    role = Role.ROLE_USER,
                    pin = "encodedPin",
                    userCiNumber = ciNumber,
                    userCiNumberHash = sensitiveInformationHasher.hash(ciNumber),
                ),
            )
        }
    }

    @Test
    @DisplayName("기존 평문 민감정보는 startup migration으로 암호문과 검색 해시로 보정된다")
    fun `migration encrypts legacy plaintext values and fills hashes`() {
        val legacyPlainPhone = "ENC:010-3333-4444"
        val stalePhoneHash = sensitiveInformationHasher.hash("stale-phone")
        val staleCiHash = sensitiveInformationHasher.hash("stale-ci")
        jdbcTemplate.update(
            """
            insert into common_user(
                username, phone, phone_hash, user_ci_number, user_ci_number_hash,
                user_barcode, user_type, email, password, role, pin
            )
            values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            "평문사용자",
            legacyPlainPhone,
            stalePhoneHash,
            "CI-PLAIN-123",
            staleCiHash,
            "BARCODE-PLAIN",
            UserType.STUDENT.name,
            "plain@test.com",
            "encodedPassword",
            Role.ROLE_USER.name,
            "encodedPin",
        )
        val id = jdbcTemplate.queryForObject(
            "select id from common_user where email = ?",
            Long::class.java,
            "plain@test.com",
        )!!

        val migratedCount = SensitiveInformationMigrationRunner(
            jdbcTemplate = jdbcTemplate,
            dataSource = dataSource,
            cryptoHelper = cryptoHelper,
            sensitiveInformationHasher = sensitiveInformationHasher,
        ).migrate()
        entityManager.clear()

        assertEquals(1, migratedCount)
        val rawValues = jdbcTemplate.queryForMap(
            "select username, phone, phone_hash, user_ci_number, user_ci_number_hash from common_user where id = ?",
            id,
        )
        assertEncrypted(rawValues["username"], "평문사용자")
        assertEncrypted(rawValues["phone"], legacyPlainPhone)
        assertEquals(sensitiveInformationHasher.hash(legacyPlainPhone), rawValues["phone_hash"])
        assertEncrypted(rawValues["user_ci_number"], "CI-PLAIN-123")
        assertEquals(sensitiveInformationHasher.hash("CI-PLAIN-123"), rawValues["user_ci_number_hash"])

        val foundEntity = userJpaRepository.findById(id).orElseThrow()
        assertEquals("평문사용자", foundEntity.username)
        assertEquals(legacyPlainPhone, foundEntity.phone)
        assertEquals("CI-PLAIN-123", foundEntity.userCiNumber)
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
