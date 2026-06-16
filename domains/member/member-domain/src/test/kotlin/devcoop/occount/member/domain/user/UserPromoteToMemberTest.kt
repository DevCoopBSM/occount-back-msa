package devcoop.occount.member.domain.user

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class UserPromoteToMemberTest {
    private fun user(role: Role): User = User(
        id = 1L,
        userInfo = UserInfo(
            username = "홍길동",
            phone = "01000000000",
            userType = UserType.STUDENT,
            cooperativeNumber = null,
            userBarcode = null,
            birthDate = LocalDate.of(2008, 1, 1),
        ),
        accountInfo = AccountInfo(email = "a@b.com", password = "pw", role = role, pin = "0000"),
        userSensitiveInfo = UserSensitiveInfo(ciNumber = "ci"),
    )

    @Test
    fun `promotes ROLE_USER to ROLE_MEMBER`() {
        assertEquals(Role.ROLE_MEMBER, user(Role.ROLE_USER).promoteToMember().getRole())
    }

    @Test
    fun `is a no-op for non ROLE_USER`() {
        val admin = user(Role.ROLE_ADMIN)
        assertSame(admin, admin.promoteToMember())

        val member = user(Role.ROLE_MEMBER)
        assertSame(member, member.promoteToMember())
    }
}
