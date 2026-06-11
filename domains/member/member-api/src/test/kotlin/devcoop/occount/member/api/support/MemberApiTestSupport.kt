package devcoop.occount.member.api.support

import devcoop.occount.core.common.auth.AuthPrincipalArgumentResolver
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.otp.OtpPurpose
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.EmailSender
import devcoop.occount.member.application.output.IdentityVerificationClient
import devcoop.occount.member.application.output.PinChangeTicketRepository
import devcoop.occount.member.application.output.VerifiedIdentity
import devcoop.occount.member.application.pin.PinChangeTicket
import devcoop.occount.member.application.usecase.identity.VerifyIdentityUseCase
import devcoop.occount.member.application.usecase.otp.SendEmailOtpUseCase
import devcoop.occount.member.application.usecase.otp.VerifyEmailOtpUseCase
import devcoop.occount.member.application.usecase.password.ChangePasswordUseCase
import devcoop.occount.member.application.usecase.pin.ChangePinUseCase
import devcoop.occount.member.application.usecase.pin.VerifyPasswordForPinChangeUseCase
import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.AbstractPlatformTransactionManager
import org.springframework.transaction.support.DefaultTransactionStatus
import tools.jackson.databind.PropertyNamingStrategies
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

    override fun findAll(pageable: Pageable): Page<User> {
        val all = usersById.values.toList()
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(all.size)
        val to = (from + pageable.pageSize).coerceAtMost(all.size)
        return PageImpl(all.subList(from, to), pageable, all.size.toLong())
    }

    override fun searchByKeyword(keyword: String, pageable: Pageable): Page<User> {
        val matched = usersById.values.filter {
            it.getUsername().contains(keyword) ||
                it.getEmail().contains(keyword) ||
                (it.getCooperativeNumber()?.contains(keyword) == true)
        }
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(matched.size)
        val to = (from + pageable.pageSize).coerceAtMost(matched.size)
        return PageImpl(matched.subList(from, to), pageable, matched.size.toLong())
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

class FakePinChangeTicketRepository(
    initialTickets: List<PinChangeTicket> = emptyList(),
) : PinChangeTicketRepository {
    private val ticketsByToken = linkedMapOf<String, PinChangeTicket>().apply {
        initialTickets.forEach { put(it.token, it) }
    }

    override fun save(ticket: PinChangeTicket): PinChangeTicket {
        ticketsByToken[ticket.token] = ticket
        return ticket
    }

    override fun findByToken(token: String): PinChangeTicket? = ticketsByToken[token]

    override fun deleteByToken(token: String) {
        ticketsByToken.remove(token)
    }

    override fun deleteByUserId(userId: Long) {
        ticketsByToken.values.removeIf { it.userId == userId }
    }
}

fun validPinChangeTicket(token: String, userId: Long): PinChangeTicket =
    PinChangeTicket(
        token = token,
        userId = userId,
        expiresAt = Instant.now().plusSeconds(PinChangeTicket.TTL_SECONDS),
        createdAt = Instant.now(),
    )

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
    pinChangeTicketRepository: PinChangeTicketRepository = FakePinChangeTicketRepository(),
) = ChangePinUseCase(
    userRepository = userRepository,
    pinChangeTicketRepository = pinChangeTicketRepository,
    passwordEncoder = FakePasswordEncoder(),
)

fun testVerifyPasswordForPinChangeUseCase(
    userRepository: UserRepository = FakeUserRepository(),
    pinChangeTicketRepository: PinChangeTicketRepository = FakePinChangeTicketRepository(),
) = VerifyPasswordForPinChangeUseCase(
    userRepository = userRepository,
    pinChangeTicketRepository = pinChangeTicketRepository,
    passwordEncoder = FakePasswordEncoder(),
)

fun mockMvc(vararg controllers: Any): MockMvc {
    // 실제 member-api 설정(spring.jackson.property-naming-strategy: SNAKE_CASE)과 동일한
    // 직렬화 규칙으로 컨트롤러 계약을 검증한다.
    val objectMapper = jacksonMapperBuilder()
        .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        .build()
    val messageConverter = JacksonJsonHttpMessageConverter(objectMapper)
    return MockMvcBuilders.standaloneSetup(*controllers)
        .setControllerAdvice(ApiAdviceHandler())
        .setCustomArgumentResolvers(AuthPrincipalArgumentResolver())
        .setMessageConverters(messageConverter)
        .build()
}
