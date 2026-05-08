package devcoop.occount.member.application.support

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.User
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder

private val sharedPasswordEncoder = FakePasswordEncoder()

fun userFixture(
    id: Long = 1L,
    username: String = "홍길동",
    email: String = "test@test.com",
    encodedPassword: String = sharedPasswordEncoder.encode("rawPassword"),
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
