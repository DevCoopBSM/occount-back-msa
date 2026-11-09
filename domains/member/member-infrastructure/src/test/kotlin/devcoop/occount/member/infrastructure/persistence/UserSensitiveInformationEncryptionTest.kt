package devcoop.occount.member.infrastructure.persistence

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.otp.OtpPurpose
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.usecase.register.MemberRegisterRequest
import devcoop.occount.member.application.usecase.register.RegisterUserUseCase
import devcoop.occount.member.domain.user.AccountInfo
import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.User
import devcoop.occount.member.domain.user.UserInfo
import devcoop.occount.member.domain.user.UserSensitiveInfo
import devcoop.occount.member.domain.user.UserType
import devcoop.occount.member.infrastructure.crypto.CryptoConfig
import devcoop.occount.member.infrastructure.crypto.CryptoHelper
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
import org.springframework.boot.persistence.autoconfigure.EntityScan
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.TestPropertySource
import java.time.Instant
import java.time.LocalDate

@DataJpaTest
@TestPropertySource(properties = ["app.encryption.secret-key=12345678901234567890123456789012"])
@DisplayName("회원 민감정보 암호화 JPA 통합 테스트")
class UserSensitiveInformationEncryptionTest @Autowired constructor(
    private val userJpaRepository: UserJpaRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val entityManager: EntityManager,
    private val cryptoHelper: CryptoHelper,
) {
    @Test
    @DisplayName("Repository 저장 후 DB raw 값은 암호문이고 repository 조회 값은 복호화된 평문이다")
    fun `repository encrypts raw values and decrypts domain values`() {
        val userRepository = UserRepositoryImpl(userJpaRepository, cryptoHelper)
        val username = "홍길동"
        val phone = "010-1234-5678"
        val ciNumber = "CI123456"
        val birthDate = LocalDate.of(2000, 1, 15)

        val savedUser = userRepository.save(
            User(
                userInfo = UserInfo(
                    username = username,
                    phone = phone,
                    userType = UserType.STUDENT,
                    cooperativeNumber = null,
                    userBarcode = "BARCODE123",
                    birthDate = birthDate,
                ),
                accountInfo = AccountInfo("test@test.com", "encodedPassword", Role.ROLE_USER, "encodedPin"),
                userSensitiveInfo = UserSensitiveInfo(ciNumber),
            ),
        )

        val rawValues = jdbcTemplate.queryForMap(
            "select username, phone, user_ci_number, birth_date from common_user where id = ?",
            savedUser.getId(),
        )
        entityManager.clear()

        // username 은 암호화 대상에서 제외되어 평문으로 저장된다(@Convert 미적용).
        assertEquals(username, rawValues["username"])
        assertEncrypted(rawValues["phone"], phone)
        assertEquals(phone, cryptoHelper.decrypt(rawValues["phone"] as String))
        assertEncrypted(rawValues["user_ci_number"], ciNumber)
        assertEquals(ciNumber, cryptoHelper.decrypt(rawValues["user_ci_number"] as String))
        assertEncrypted(rawValues["birth_date"], birthDate.toString())
        assertEquals(birthDate.toString(), cryptoHelper.decrypt(rawValues["birth_date"] as String))

        val foundUser = userRepository.findById(savedUser.getId())
        assertEquals(username, foundUser!!.getUsername())
        assertEquals(phone, foundUser.getPhone())
        assertEquals(ciNumber, foundUser.getCiNumber())
        assertEquals(birthDate, foundUser.getBirthDate())
    }

    @Test
    @DisplayName("회원가입 use case 저장 후 DB raw 값은 암호문이고 repository 조회 값은 복호화된 평문이다")
    fun `register use case stores encrypted sensitive information`() {
        val userRepository = UserRepositoryImpl(userJpaRepository, cryptoHelper)
        val registerUserUseCase = RegisterUserUseCase(
            userRepository = userRepository,
            eventPublisher = NoOpEventPublisher(),
            passwordEncoder = TestPasswordEncoder(),
            emailOtpRepository = VerifiedEmailOtpRepository("register@test.com"),
        )
        val username = "가입자"
        val phone = "010-9999-8888"
        val ciNumber = "CI-REGISTER-123"
        val birthDate = LocalDate.of(2001, 2, 16)

        registerUserUseCase.register(
            MemberRegisterRequest(
                userCiNumber = ciNumber,
                username = username,
                userPhone = phone,
                birthDate = birthDate,
                userEmail = "register@test.com",
                password = "password1234",
                pin = "123456",
            ),
        )
        entityManager.flush()

        val rawValues = jdbcTemplate.queryForMap(
            "select username, phone, user_ci_number, birth_date from common_user where email = ?",
            "register@test.com",
        )
        entityManager.clear()

        // username 은 암호화 대상에서 제외되어 평문으로 저장된다(@Convert 미적용).
        assertEquals(username, rawValues["username"])
        assertEncrypted(rawValues["phone"], phone)
        assertEquals(phone, cryptoHelper.decrypt(rawValues["phone"] as String))
        assertEncrypted(rawValues["user_ci_number"], ciNumber)
        assertEquals(ciNumber, cryptoHelper.decrypt(rawValues["user_ci_number"] as String))
        assertEncrypted(rawValues["birth_date"], birthDate.toString())
        assertEquals(birthDate.toString(), cryptoHelper.decrypt(rawValues["birth_date"] as String))

        val foundUser = userRepository.findByEmail("register@test.com")
        assertEquals(username, foundUser!!.getUsername())
        assertEquals(phone, foundUser.getPhone())
        assertEquals(ciNumber, foundUser.getCiNumber())
        assertEquals(birthDate, foundUser.getBirthDate())
    }

    @Test
    @DisplayName("같은 전화번호와 CI도 랜덤 암호화로 서로 다른 raw 암호문이 저장된다")
    fun `same sensitive values are stored as different ciphertexts`() {
        val userRepository = UserRepositoryImpl(userJpaRepository, cryptoHelper)
        val phone = "010-1111-2222"
        val ciNumber = "CI-SAME-123"

        userRepository.save(createUser("first@test.com", "BARCODE-FIRST", phone, ciNumber))
        userRepository.save(createUser("second@test.com", "BARCODE-SECOND", phone, ciNumber))

        val rows = jdbcTemplate.queryForList(
            "select phone, user_ci_number from common_user where email in (?, ?) order by email",
            "first@test.com",
            "second@test.com",
        )

        assertNotEquals(rows[0]["phone"], rows[1]["phone"])
        assertEquals(phone, cryptoHelper.decrypt(rows[0]["phone"] as String))
        assertEquals(phone, cryptoHelper.decrypt(rows[1]["phone"] as String))
        assertNotEquals(rows[0]["user_ci_number"], rows[1]["user_ci_number"])
        assertEquals(ciNumber, cryptoHelper.decrypt(rows[0]["user_ci_number"] as String))
        assertEquals(ciNumber, cryptoHelper.decrypt(rows[1]["user_ci_number"] as String))
    }

    @Test
    @DisplayName("동일한 암호문은 DB unique 제약으로 중복 저장되지 않는다")
    fun `same encrypted values are rejected by unique constraints`() {
        val encryptedPhone = cryptoHelper.encrypt("010-2222-3333")
        val encryptedCiNumber = cryptoHelper.encrypt("CI-UNIQUE-123")

        userJpaRepository.saveAndFlush(
            UserJpaEntity(
                username = "첫번째",
                phone = encryptedPhone,
                userBarcode = "BARCODE-UNIQUE-FIRST",
                userType = UserType.STUDENT,
                email = "unique-first@test.com",
                password = "encodedPassword",
                role = Role.ROLE_USER,
                pin = "encodedPin",
                userCiNumber = encryptedCiNumber,
                birthDate = cryptoHelper.encrypt("2000-01-15"),
            ),
        )

        assertThrows(DataIntegrityViolationException::class.java) {
            userJpaRepository.saveAndFlush(
                UserJpaEntity(
                    username = "두번째",
                    phone = encryptedPhone,
                    userBarcode = "BARCODE-UNIQUE-SECOND",
                    userType = UserType.STUDENT,
                    email = "unique-second@test.com",
                    password = "encodedPassword",
                    role = Role.ROLE_USER,
                    pin = "encodedPin",
                    userCiNumber = cryptoHelper.encrypt("CI-UNIQUE-456"),
                    birthDate = cryptoHelper.encrypt("2000-01-16"),
                ),
            )
        }
    }

    private fun createUser(
        email: String,
        barcode: String,
        phone: String,
        ciNumber: String,
    ): User {
        return User(
            userInfo = UserInfo(
                username = "사용자",
                phone = phone,
                userType = UserType.STUDENT,
                cooperativeNumber = null,
                userBarcode = barcode,
                birthDate = LocalDate.of(2000, 1, 15),
            ),
            accountInfo = AccountInfo(email, "encodedPassword", Role.ROLE_USER, "encodedPin"),
            userSensitiveInfo = UserSensitiveInfo(ciNumber),
        )
    }

    private fun assertEncrypted(rawValue: Any?, plainText: String) {
        val rawText = rawValue as String
        assertNotEquals(plainText, rawText)
        assertTrue(rawText.startsWith(CryptoHelper.ENCRYPTION_PREFIX))
    }

    private class NoOpEventPublisher : EventPublisher {
        override fun publish(topic: String, key: String, eventType: String, payload: Any) = Unit
    }

    private class VerifiedEmailOtpRepository(
        private val verifiedEmail: String,
    ) : EmailOtpRepository {
        private var deletedEmail: String? = null

        override fun save(emailOtp: EmailOtp): EmailOtp = emailOtp

        override fun findByEmail(email: String): EmailOtp? = findValidByEmail(email)

        override fun findByEmailForUpdate(email: String): EmailOtp? = findValidByEmail(email)

        override fun findValidByEmail(email: String): EmailOtp? {
            if (email != verifiedEmail) {
                return null
            }
            return EmailOtp(
                email = email,
                otpCode = "123456",
                expiresAt = Instant.now().plusSeconds(60),
                purpose = OtpPurpose.REGISTER,
                verified = true,
                createdAt = Instant.now(),
            )
        }

        override fun deleteByEmail(email: String) {
            deletedEmail = email
        }
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
