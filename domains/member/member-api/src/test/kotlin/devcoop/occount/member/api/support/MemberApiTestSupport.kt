package devcoop.occount.member.api.support

import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.member.application.output.TokenGenerator
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.User
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders

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

fun mockMvc(vararg controllers: Any): MockMvc {
    val messageConverter = MappingJackson2HttpMessageConverter(
        jacksonObjectMapper(),
    )
    return MockMvcBuilders.standaloneSetup(*controllers)
        .setControllerAdvice(ApiAdviceHandler())
        .setMessageConverters(messageConverter)
        .build()
}
