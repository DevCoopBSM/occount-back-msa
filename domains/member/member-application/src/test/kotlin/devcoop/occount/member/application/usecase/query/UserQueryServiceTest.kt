package devcoop.occount.member.application.usecase.query

import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.domain.user.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("UserQueryService 단위 테스트")
class UserQueryServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var userQueryService: UserQueryService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        userQueryService = UserQueryService(userRepository)
    }

    private fun createUser(username: String) = User(
        id = 1L,
        userInfo = UserInfo(username, null, UserType.STUDENT, null, null),
        accountInfo = AccountInfo("test@test.com", "pw", Role.ROLE_USER, "pin"),
        userSensitiveInfo = UserSensitiveInfo(null),
    )

    @Test
    @DisplayName("userId로 유저를 조회하면 username이 담긴 UserPreOrderInfoResponse를 반환한다")
    fun `findPreOrderInfo returns response with username when user exists`() {
        val user = createUser("홍길동")
        `when`(userRepository.findById(1L)).thenReturn(user)

        val result = userQueryService.findPreOrderInfo(1L)

        assertEquals("홍길동", result.username)
    }

    @Test
    @DisplayName("존재하지 않는 userId로 조회 시 UserNotFoundException이 발생한다")
    fun `findPreOrderInfo throws UserNotFoundException when user not found`() {
        `when`(userRepository.findById(999L)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userQueryService.findPreOrderInfo(999L)
        }
    }
}
