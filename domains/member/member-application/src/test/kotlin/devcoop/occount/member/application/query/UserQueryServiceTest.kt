package devcoop.occount.member.application.query

import devcoop.occount.member.application.exception.UserNotFoundException
import devcoop.occount.member.application.support.FakeUserRepository
import devcoop.occount.member.application.support.userFixture
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("UserQueryService 단위 테스트")
class UserQueryServiceTest {
    @Test
    @DisplayName("userId로 유저를 조회하면 username이 담긴 UserPreOrderInfoResponse를 반환한다")
    fun `findPreOrderInfo returns response with username when user exists`() {
        val userQueryService = UserQueryService(
            FakeUserRepository(
                initialUsers = listOf(userFixture(username = "홍길동")),
            ),
        )

        val result = userQueryService.findPreOrderInfo(1L)

        assertEquals("홍길동", result.username)
    }

    @Test
    @DisplayName("존재하지 않는 userId로 조회 시 UserNotFoundException이 발생한다")
    fun `findPreOrderInfo throws UserNotFoundException when user not found`() {
        val userQueryService = UserQueryService(FakeUserRepository())

        assertFailsWith<UserNotFoundException> {
            userQueryService.findPreOrderInfo(999L)
        }
    }
}
