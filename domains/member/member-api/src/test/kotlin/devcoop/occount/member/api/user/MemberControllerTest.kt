package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.member.api.support.FakeUserRepository
import devcoop.occount.member.api.support.mockMvc
import devcoop.occount.member.api.support.userFixture
import devcoop.occount.member.application.usecase.query.UserQueryService
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MemberControllerTest {
    @Test
    fun `find user info returns authenticated user response`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(
                    FakeUserRepository(
                        initialUsers = listOf(userFixture(id = 7L, username = "Tester")),
                    ),
                ),
            ),
        )

        mockMvc.perform(
            get("/users/pre-order-info")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("Tester"))
    }

    @Test
    fun `find user info returns 401 when authenticated user header is missing`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(FakeUserRepository()),
            ),
        )

        mockMvc.perform(get("/users/pre-order-info"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }
}
