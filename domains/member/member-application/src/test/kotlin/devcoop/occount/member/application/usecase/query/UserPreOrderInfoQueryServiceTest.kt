package devcoop.occount.member.application.usecase.query

import devcoop.occount.member.application.exception.PointNotFoundException
import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.output.UserRepository
import devcoop.occount.member.application.output.WalletPointReader
import devcoop.occount.member.application.query.UserPreOrderInfoQueryService
import devcoop.occount.member.domain.user.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*

@DisplayName("UserQueryService 단위 테스트")
class UserPreOrderInfoQueryServiceTest {

    private lateinit var userRepository: UserRepository
    private lateinit var walletPointReader: WalletPointReader
    private lateinit var userPreOrderInfoQueryService: UserPreOrderInfoQueryService

    @BeforeEach
    fun setUp() {
        userRepository = mock(UserRepository::class.java)
        walletPointReader = mock(WalletPointReader::class.java)
        userPreOrderInfoQueryService = UserPreOrderInfoQueryService(userRepository, walletPointReader)
    }

    private fun createUser(username: String) = User(
        id = 1L,
        userInfo = UserInfo(username, null, UserType.STUDENT, null, null),
        accountInfo = AccountInfo("test@test.com", "pw", Role.ROLE_USER, "pin"),
        userSensitiveInfo = UserSensitiveInfo(null),
    )

    @Test
    @DisplayName("userId로 유저를 조회하면 username과 point가 담긴 UserPreOrderInfoResponse를 반환한다")
    fun `findPreOrderInfo returns response with username and point when user exists`() {
        val user = createUser("홍길동")
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(walletPointReader.getPoint(1L)).thenReturn(3000)

        val result = userPreOrderInfoQueryService.findPreOrderInfo(1L)

        assertEquals("홍길동", result.username)
        assertEquals(3000, result.point)
    }

    @Test
    @DisplayName("존재하지 않는 userId로 조회 시 UserNotFoundException이 발생한다")
    fun `findPreOrderInfo throws UserNotFoundException when user not found`() {
        `when`(userRepository.findById(999L)).thenReturn(null)

        assertThrows(UserNotFoundException::class.java) {
            userPreOrderInfoQueryService.findPreOrderInfo(999L)
        }

        verifyNoInteractions(walletPointReader)
    }

    @Test
    @DisplayName("포인트 조회에 실패하면 PointNotFoundException이 발생한다")
    fun `findPreOrderInfo throws PointNotFoundException when point not found`() {
        val user = createUser("홍길동")
        `when`(userRepository.findById(1L)).thenReturn(user)
        `when`(walletPointReader.getPoint(1L)).thenThrow(PointNotFoundException())

        assertThrows(PointNotFoundException::class.java) {
            userPreOrderInfoQueryService.findPreOrderInfo(1L)
        }
    }
}
