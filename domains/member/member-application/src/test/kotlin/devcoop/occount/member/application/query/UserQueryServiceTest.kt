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

    @Test
    @DisplayName("userId로 유저를 조회하면 userBarcode가 담긴 UserBarcodeResponse를 반환한다")
    fun `findUserBarcode returns response with user barcode when user exists`() {
        val userQueryService = UserQueryService(
            FakeUserRepository(
                initialUsers = listOf(userFixture(barcode = "BARCODE-777")),
            ),
        )

        val result = userQueryService.findUserBarcode(1L)

        assertEquals("BARCODE-777", result.userBarcode)
    }

    @Test
    @DisplayName("존재하지 않는 userId로 바코드 조회 시 UserNotFoundException이 발생한다")
    fun `findUserBarcode throws UserNotFoundException when user not found`() {
        val userQueryService = UserQueryService(FakeUserRepository())

        assertFailsWith<UserNotFoundException> {
            userQueryService.findUserBarcode(999L)
        }
    }

    @Test
    @DisplayName("userId로 유저를 조회하면 회원정보가 담긴 MemberInfoResponse를 반환한다")
    fun `findMemberInfo returns response with member info when user exists`() {
        val userQueryService = UserQueryService(
            FakeUserRepository(
                initialUsers = listOf(userFixture(username = "홍길동", email = "hong@test.com")),
            ),
        )

        val result = userQueryService.findMemberInfo(1L)

        assertEquals("홍길동", result.username)
        assertEquals("hong@test.com", result.email)
    }

    @Test
    @DisplayName("존재하지 않는 userId로 회원정보 조회 시 UserNotFoundException이 발생한다")
    fun `findMemberInfo throws UserNotFoundException when user not found`() {
        val userQueryService = UserQueryService(FakeUserRepository())

        assertFailsWith<UserNotFoundException> {
            userQueryService.findMemberInfo(999L)
        }
    }
}
