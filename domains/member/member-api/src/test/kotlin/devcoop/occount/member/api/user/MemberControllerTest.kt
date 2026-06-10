package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.member.api.support.FakeUserRepository
import devcoop.occount.member.api.support.mockMvc
import devcoop.occount.member.api.support.testChangePinUseCase
import devcoop.occount.member.api.support.userFixture
import devcoop.occount.member.application.query.UserQueryService
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
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
                changePinUseCase = testChangePinUseCase(),
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
                changePinUseCase = testChangePinUseCase(),
            ),
        )

        mockMvc.perform(get("/users/pre-order-info"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }

    @Test
    fun `find user barcode returns authenticated user barcode response`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(
                    FakeUserRepository(
                        initialUsers = listOf(userFixture(id = 7L, barcode = "BARCODE-007")),
                    ),
                ),
                changePinUseCase = testChangePinUseCase(),
            ),
        )

        mockMvc.perform(
            get("/users/barcode")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.user_barcode").value("BARCODE-007"))
    }

    @Test
    fun `find user barcode returns 401 when authenticated user header is missing`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(FakeUserRepository()),
                changePinUseCase = testChangePinUseCase(),
            ),
        )

        mockMvc.perform(get("/users/barcode"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }

    @Test
    fun `find member info returns authenticated user member info response`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(
                    FakeUserRepository(
                        initialUsers = listOf(userFixture(id = 7L, username = "Tester", email = "tester@test.com")),
                    ),
                ),
                changePinUseCase = testChangePinUseCase(),
            ),
        )

        mockMvc.perform(
            get("/users/me")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7"),
        ).andExpect(status().isOk)
            .andExpect(jsonPath("$.username").value("Tester"))
            .andExpect(jsonPath("$.email").value("tester@test.com"))
    }

    @Test
    fun `find member info returns 401 when authenticated user header is missing`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(FakeUserRepository()),
                changePinUseCase = testChangePinUseCase(),
            ),
        )

        mockMvc.perform(get("/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }

    @Test
    fun `changePin returns 204 when current password matches`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(FakeUserRepository()),
                changePinUseCase = testChangePinUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(id = 7L))),
                ),
            ),
        )

        mockMvc.perform(
            post("/users/pin/change")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": "password1234", "new_pin": "4321"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `changePin returns 401 when current password is wrong`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(FakeUserRepository()),
                changePinUseCase = testChangePinUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(id = 7L))),
                ),
            ),
        )

        mockMvc.perform(
            post("/users/pin/change")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": "wrong-password", "new_pin": "4321"}"""),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    fun `changePin returns 400 when new pin format is invalid`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(FakeUserRepository()),
                changePinUseCase = testChangePinUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(id = 7L))),
                ),
            ),
        )

        mockMvc.perform(
            post("/users/pin/change")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": "password1234", "new_pin": "abc"}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.newPin").value("PIN은 4~6자리 숫자여야 합니다."))
    }

    @Test
    fun `changePin returns 401 when authenticated user header is missing`() {
        val mockMvc = mockMvc(
            MemberController(
                userQueryService = UserQueryService(FakeUserRepository()),
                changePinUseCase = testChangePinUseCase(),
            ),
        )

        mockMvc.perform(
            post("/users/pin/change")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": "password1234", "new_pin": "4321"}"""),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }
}
