package devcoop.occount.member.application.support

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.login.KioskLoginAttempt
import devcoop.occount.member.application.output.EmailOtpRepository
import devcoop.occount.member.application.output.KioskLoginAttemptRepository
import devcoop.occount.member.application.output.PinChangeTicketRepository
import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.application.otp.EmailOtp
import devcoop.occount.member.application.pin.PinChangeTicket
import devcoop.occount.member.domain.user.AccountInfo
import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.User
import devcoop.occount.member.domain.user.UserInfo
import devcoop.occount.member.domain.user.UserSensitiveInfo
import devcoop.occount.member.domain.user.UserType
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant

private val sharedPasswordEncoder = FakePasswordEncoder()

fun userFixture(
    id: Long = 1L,
    username: String = "홍길동",
    email: String = "test@test.com",
    encodedPassword: String = sharedPasswordEncoder.encode("rawPassword"),
    encodedPin: String = sharedPasswordEncoder.encode("123456"),
    barcode: String? = "BARCODE123",
    cooperativeNumber: String? = null,
): User = User(
    id = id,
    userInfo = UserInfo(
        username = username,
        phone = "010-1234-5678",
        userType = UserType.STUDENT,
        cooperativeNumber = cooperativeNumber,
        userBarcode = barcode,
        birthDate = null,
    ),
    accountInfo = AccountInfo(
        email = email,
        password = encodedPassword,
        role = Role.ROLE_USER,
        pin = encodedPin,
    ),
    userSensitiveInfo = UserSensitiveInfo(ciNumber = "CI123456"),
)

class FakeUserRepository(
    initialUsers: List<User> = emptyList(),
    private val saveException: RuntimeException? = null,
) : UserRepository {
    private val usersById = linkedMapOf<Long, User>().apply {
        initialUsers.forEach { user -> put(user.getId(), user) }
    }
    private var nextId = (usersById.keys.maxOrNull() ?: 0L) + 1

    val savedUsers = mutableListOf<User>()

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
        saveException?.let { throw it }

        val persistedUser = if (user.getId() == 0L) user.copy(id = nextId++) else user

        usersById[persistedUser.getId()] = persistedUser
        savedUsers += persistedUser
        return persistedUser
    }

    override fun findAll(pageable: Pageable): Page<User> {
        val all = usersById.values.toList()
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(all.size)
        val to = (from + pageable.pageSize).coerceAtMost(all.size)
        return PageImpl(all.subList(from, to), pageable, all.size.toLong())
    }

    override fun searchByKeyword(keyword: String, pageable: Pageable): Page<User> {
        val matched = usersById.values.filter { it.matchesKeyword(keyword) }
        val from = (pageable.pageNumber * pageable.pageSize).coerceAtMost(matched.size)
        val to = (from + pageable.pageSize).coerceAtMost(matched.size)
        return PageImpl(matched.subList(from, to), pageable, matched.size.toLong())
    }
}

internal fun User.matchesKeyword(keyword: String): Boolean =
    getUsername().contains(keyword) ||
        getEmail().contains(keyword) ||
        (getCooperativeNumber()?.contains(keyword) == true)

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

fun verifiedEmailOtp(email: String, otpCode: String = "123456"): EmailOtp =
    EmailOtp(
        email = email,
        otpCode = otpCode,
        expiresAt = Instant.now().plusSeconds(EmailOtp.OTP_TTL_SECONDS),
        createdAt = Instant.now(),
        verified = true,
    )

class FakePinChangeTicketRepository(
    initialTickets: List<PinChangeTicket> = emptyList(),
) : PinChangeTicketRepository {
    private val ticketsByToken = linkedMapOf<String, PinChangeTicket>().apply {
        initialTickets.forEach { put(it.token, it) }
    }

    val savedTickets = mutableListOf<PinChangeTicket>()

    override fun save(ticket: PinChangeTicket): PinChangeTicket {
        ticketsByToken[ticket.token] = ticket
        savedTickets += ticket
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

class FakeKioskLoginAttemptRepository(
    initialAttempts: List<KioskLoginAttempt> = emptyList(),
) : KioskLoginAttemptRepository {
    private val attemptsByBarcode = linkedMapOf<String, KioskLoginAttempt>().apply {
        initialAttempts.forEach { put(it.userBarcode, it) }
    }

    val savedAttempts = mutableListOf<KioskLoginAttempt>()

    override fun findByBarcode(userBarcode: String): KioskLoginAttempt? = attemptsByBarcode[userBarcode]

    override fun save(attempt: KioskLoginAttempt): KioskLoginAttempt {
        attemptsByBarcode[attempt.userBarcode] = attempt
        savedAttempts += attempt
        return attempt
    }

    override fun deleteByBarcode(userBarcode: String) {
        attemptsByBarcode.remove(userBarcode)
    }
}

class FakeTokenGenerator : TokenGenerator {
    override fun createAccessToken(userId: Long, role: String): String = "access-$userId-$role"

    override fun createKioskToken(userId: Long, role: String): String = "kiosk-$userId-$role"
}

class FakeEventPublisher : EventPublisher {
    data class PublishedEvent(
        val topic: String,
        val key: String,
        val eventType: String,
        val payload: Any,
    )

    val published = mutableListOf<PublishedEvent>()

    override fun publish(topic: String, key: String, eventType: String, payload: Any) {
        published += PublishedEvent(topic, key, eventType, payload)
    }
}

class FakePasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: CharSequence?): String = "encoded:$rawPassword"

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?): Boolean {
        return encode(rawPassword) == encodedPassword
    }

    override fun upgradeEncoding(encodedPassword: String?): Boolean = false
}

fun duplicateUserSaveException(): RuntimeException = DataIntegrityViolationException("duplicate")
