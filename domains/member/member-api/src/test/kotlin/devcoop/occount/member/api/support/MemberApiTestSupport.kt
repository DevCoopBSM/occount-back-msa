package devcoop.occount.member.api.support

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.otp.OtpPurpose
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.EmailSender
import devcoop.occount.member.application.output.IdentityVerificationClient
import devcoop.occount.member.application.output.VerifiedIdentity
import devcoop.occount.member.application.usecase.identity.VerifyIdentityUseCase
import devcoop.occount.member.application.usecase.otp.SendEmailOtpUseCase
import devcoop.occount.member.application.usecase.otp.VerifyEmailOtpUseCase
import devcoop.occount.member.application.usecase.password.ChangePasswordUseCase
import devcoop.occount.member.application.usecase.pin.ChangePinUseCase
import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.User
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import tools.jackson.module.kotlin.jacksonMapperBuilder
import java.time.Instant

private val sharedPasswordEncoder = FakePasswordEncoder()

fun userFixture(
    id: Long = 1L,
    username: String = "홍길동",
    email: String = "test@test.com",
    encodedPassword: String = sharedPasswordEncoder.encode("password1234"),
    encodedPin: String = sharedPasswordEncoder.encode("123456"),
    barcode: String? = "BARCODE123",
): User {
    val user = User.register(
        userCiNumber = "CI123456",
        username = username,
        phone = "010-1234-5678",
        email = email,
        encodedPassword = encodedPassword,
        encodedPin = encodedPin,
    )
    return (if (barcode != null) user.withBarcode(barcode) else user).copy(id = id)
}

class FakeUserRepository(
    initialUsers: List<User> = emptyList(),
) : UserRepository {
    private val usersById = linkedMapOf<Long, User>().apply {
        initialUsers.forEach { user -> put(user.getId(), user) }
    }
    private var nextId = (usersById.keys.maxOrNull() ?: 0L) + 1

    override fun findById(id: Long): User? = usersById[id]

    override fun findByUserBarcode(userBarcode: String): User? {
        return usersById.values.firstOrNull { it.getUserBarcode() == userBarcode }
    }

    override fun findByEmail(userEmail: String): User? {
        return usersById.values.firstOrNull { it.getEmail() == userEmail }
    }

    override fun existsByEmail(userEmail: String): Boolean {
        return usersById.values.any { it.getEmail() == userEmail }
    }

    override fun save(user: User): User {
        val persistedUser = if (user.getId() == 0L) user.copy(id = nextId++) else user
        usersById[persistedUser.getId()] = persistedUser
        return persistedUser
    }
}

class FakeTokenGenerator : TokenGenerator {
    override fun createAccessToken(userId: Long, role: String): String = "access-$userId-$role"

    override fun createKioskToken(userId: Long, role: String): String = "kiosk-$userId-$role"
}

class FakeEventPublisher : EventPublisher {
    override fun publish(topic: String, key: String, eventType: String, payload: Any) = Unit
}

class FakePasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: CharSequence?): String = "encoded:$rawPassword"

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
        return encode(rawPassword) == encodedPassword
    }

    override fun upgradeEncoding(encodedPassword: String?): Boolean = false
}

class FakeEmailSender : EmailSender {
    override fun sendOtp(to: String, otpCode: String) = Unit
}

class FakeEmailOtpRepository(
    initialOtpsByEmail: Map<String, EmailOtp> = emptyMap(),
) : EmailOtpRepository {
    private val otpsByEmail = initialOtpsByEmail.toMutableMap()

    override fun save(emailOtp: EmailOtp): EmailOtp {
        otpsByEmail[emailOtp.email] = emailOtp
        return emailOtp
    }

    override fun findByEmail(email: String): EmailOtp? = otpsByEmail[email]

    override fun findByEmailForUpdate(email: String): EmailOtp? = otpsByEmail[email]

    override fun findValidByEmail(email: String): EmailOtp? =
        otpsByEmail[email]?.takeIf { !it.isExpired() }

    override fun deleteByEmail(email: String) {
        otpsByEmail.remove(email)
    }
}

class FakeIdentityVerificationClient(
    private val response: VerifiedIdentity = VerifiedIdentity(
        ciNumber = "CI_TEST_123",
        username = "홍길동",
        phone = "01012345678",
        birthDate = java.time.LocalDate.of(2000, 1, 15),
    ),
) : IdentityVerificationClient {
    override fun verify(identityVerificationId: String): VerifiedIdentity = response
}

fun verifiedEmailOtp(email: String, otpCode: String = "123456"): EmailOtp =
    EmailOtp(
        email = email,
        otpCode = otpCode,
        expiresAt = Instant.now().plusSeconds(EmailOtp.OTP_TTL_SECONDS),
        createdAt = Instant.now(),
        verified = true,
    )

fun passwordResetOtp(email: String, otpCode: String = "123456"): EmailOtp =
    EmailOtp(
        email = email,
        otpCode = otpCode,
        expiresAt = Instant.now().plusSeconds(EmailOtp.OTP_TTL_SECONDS),
        purpose = OtpPurpose.PASSWORD_RESET,
        createdAt = Instant.now(),
    )

private val noopTransactionManager = object : AbstractPlatformTransactionManager() {
    override fun doGetTransaction(): Any = Object()
    override fun doBegin(transaction: Any, definition: TransactionDefinition) = Unit
    override fun doCommit(status: DefaultTransactionStatus) = Unit
    override fun doRollback(status: DefaultTransactionStatus) = Unit
}

fun testSendEmailOtpUseCase(emailOtpRepository: EmailOtpRepository) =
    SendEmailOtpUseCase(
        emailOtpRepository = emailOtpRepository,
        emailSender = FakeEmailSender(),
        transactionManager = noopTransactionManager,
    )

fun testVerifyIdentityUseCase() =
    VerifyIdentityUseCase(
        identityVerificationClient = FakeIdentityVerificationClient(),
    )

fun testVerifyEmailOtpUseCase(emailOtpRepository: EmailOtpRepository) =
    VerifyEmailOtpUseCase(
        emailOtpRepository = emailOtpRepository,
    )

fun testChangePasswordUseCase(
    userRepository: UserRepository = FakeUserRepository(),
    emailOtpRepository: EmailOtpRepository = FakeEmailOtpRepository(),
) = ChangePasswordUseCase(
    userRepository = userRepository,
    emailOtpRepository = emailOtpRepository,
    passwordEncoder = FakePasswordEncoder(),
)

fun testChangePinUseCase(
    userRepository: UserRepository = FakeUserRepository(),
) = ChangePinUseCase(
    userRepository = userRepository,
    passwordEncoder = FakePasswordEncoder(),
)

fun mockMvc(vararg controllers: Any): MockMvc {
    val messageConverter = JacksonJsonHttpMessageConverter(jacksonMapperBuilder())
    return MockMvcBuilders.standaloneSetup(*controllers)
        .setControllerAdvice(ApiAdviceHandler())
        .setMessageConverters(messageConverter)
        .build()
}
