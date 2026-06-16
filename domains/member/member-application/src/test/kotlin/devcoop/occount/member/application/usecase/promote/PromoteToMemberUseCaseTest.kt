package devcoop.occount.member.application.usecase.promote

import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.AccountInfo
import devcoop.occount.member.domain.user.Role
import devcoop.occount.member.domain.user.User
import devcoop.occount.member.domain.user.UserInfo
import devcoop.occount.member.domain.user.UserSensitiveInfo
import devcoop.occount.member.domain.user.UserType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import kotlin.test.Test
import kotlin.test.assertEquals

class PromoteToMemberUseCaseTest {
    @Test
    fun `promotes a ROLE_USER to ROLE_MEMBER`() {
        val repository = FakeUserRepository(user(id = 1L, role = Role.ROLE_USER))
        val useCase = PromoteToMemberUseCase(repository)

        useCase.promote(1L)

        assertEquals(Role.ROLE_MEMBER, repository.findById(1L)!!.getRole())
    }

    @Test
    fun `is idempotent for an already promoted member`() {
        val repository = FakeUserRepository(user(id = 1L, role = Role.ROLE_MEMBER))
        val useCase = PromoteToMemberUseCase(repository)

        useCase.promote(1L)

        assertEquals(0, repository.saveCount)
    }

    @Test
    fun `does nothing when user is missing`() {
        val repository = FakeUserRepository()
        val useCase = PromoteToMemberUseCase(repository)

        useCase.promote(404L)

        assertEquals(0, repository.saveCount)
    }

    private fun user(id: Long, role: Role): User = User(
        id = id,
        userInfo = UserInfo(
            username = "홍길동",
            phone = "01000000000",
            userType = UserType.STUDENT,
            cooperativeNumber = null,
            userBarcode = null,
            birthDate = null,
        ),
        accountInfo = AccountInfo(email = "a@b.com", password = "pw", role = role, pin = "0000"),
        userSensitiveInfo = UserSensitiveInfo(ciNumber = "ci"),
    )

    private class FakeUserRepository(initial: User? = null) : UserRepository {
        private val store = mutableMapOf<Long, User>().apply { initial?.let { put(it.getId(), it) } }
        var saveCount = 0

        override fun findById(id: Long): User? = store[id]
        override fun findByUserBarcode(userBarcode: String): User? = null
        override fun findByEmail(userEmail: String): User? = null
        override fun existsByEmail(userEmail: String): Boolean = false
        override fun save(user: User): User {
            saveCount++
            store[user.getId()] = user
            return user
        }
        override fun findAll(pageable: Pageable): Page<User> = throw UnsupportedOperationException()
        override fun searchByKeyword(keyword: String, pageable: Pageable): Page<User> = throw UnsupportedOperationException()
    }
}
