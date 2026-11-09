package devcoop.occount.member.api.user

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.member.api.support.FakePinChangeTicketRepository
import devcoop.occount.member.api.support.FakeUserRepository
import devcoop.occount.member.api.support.mockMvc
import devcoop.occount.member.api.support.testChangePinUseCase
import devcoop.occount.member.api.support.testVerifyPasswordForPinChangeUseCase
import devcoop.occount.member.api.support.userFixture
import devcoop.occount.member.api.support.validPinChangeTicket
import devcoop.occount.member.application.query.UserQueryService
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class MemberControllerTest {
    private fun controller(
        userQueryService: UserQueryService = UserQueryService(FakeUserRepository()),
        verifyPasswordForPinChangeUseCase: devcoop.occount.member.application.usecase.pin.VerifyPasswordForPinChangeUseCase =
            testVerifyPasswordForPinChangeUseCase(),
        changePinUseCase: devcoop.occount.member.application.usecase.pin.ChangePinUseCase = testChangePinUseCase(),
    ) = MemberController(
        userQueryService = userQueryService,
        verifyPasswordForPinChangeUseCase = verifyPasswordForPinChangeUseCase,
        changePinUseCase = changePinUseCase,
    )

    @Test
    fun `find user info returns authenticated user response`() {
        val mockMvc = mockMvc(
            controller(
                userQueryService = UserQueryService(
                    FakeUserRepository(initialUsers = listOf(userFixture(id = 7L, username = "Tester"))),
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
        val mockMvc = mockMvc(controller())

        mockMvc.perform(get("/users/pre-order-info"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }

    @Test
    fun `find user barcode returns authenticated user barcode response`() {
        val mockMvc = mockMvc(
            controller(
                userQueryService = UserQueryService(
                    FakeUserRepository(initialUsers = listOf(userFixture(id = 7L, barcode = "BARCODE-007"))),
                ),
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
        val mockMvc = mockMvc(controller())

        mockMvc.perform(get("/users/barcode"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }

    @Test
    fun `find member info returns authenticated user member info response`() {
        val mockMvc = mockMvc(
            controller(
                userQueryService = UserQueryService(
                    FakeUserRepository(
                        initialUsers = listOf(userFixture(id = 7L, username = "Tester", email = "tester@test.com")),
                    ),
                ),
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
        val mockMvc = mockMvc(controller())

        mockMvc.perform(get("/users/me"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }

    @Test
    fun `issuePinChangeTicket returns 201 with ticket when password matches`() {
        val mockMvc = mockMvc(
            controller(
                verifyPasswordForPinChangeUseCase = testVerifyPasswordForPinChangeUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(id = 7L))),
                ),
            ),
        )

        mockMvc.perform(
            post("/users/pin/change-ticket")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": "password1234"}"""),
        ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.ticket").isNotEmpty)
            .andExpect(jsonPath("$.expires_in").value(300))
    }

    @Test
    fun `issuePinChangeTicket returns 401 when password is wrong`() {
        val mockMvc = mockMvc(
            controller(
                verifyPasswordForPinChangeUseCase = testVerifyPasswordForPinChangeUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(id = 7L))),
                ),
            ),
        )

        mockMvc.perform(
            post("/users/pin/change-ticket")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": "wrong-password"}"""),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("비밀번호가 일치하지 않습니다."))
    }

    @Test
    fun `issuePinChangeTicket returns 400 when password is blank`() {
        val mockMvc = mockMvc(controller())

        mockMvc.perform(
            post("/users/pin/change-ticket")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"password": ""}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.password").value("비밀번호는 비어있을 수 없습니다."))
    }

    @Test
    fun `changePin returns 204 when ticket is valid`() {
        val ticketRepository = FakePinChangeTicketRepository(
            initialTickets = listOf(validPinChangeTicket(token = "ticket-1", userId = 7L)),
        )
        val mockMvc = mockMvc(
            controller(
                changePinUseCase = testChangePinUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(id = 7L))),
                    pinChangeTicketRepository = ticketRepository,
                ),
            ),
        )

        mockMvc.perform(
            put("/users/pin")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ticket": "ticket-1", "new_pin": "4321"}"""),
        ).andExpect(status().isNoContent)
    }

    @Test
    fun `changePin returns 401 when ticket is unknown`() {
        val mockMvc = mockMvc(
            controller(
                changePinUseCase = testChangePinUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(id = 7L))),
                    pinChangeTicketRepository = FakePinChangeTicketRepository(),
                ),
            ),
        )

        mockMvc.perform(
            put("/users/pin")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ticket": "unknown", "new_pin": "4321"}"""),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("비밀번호 확인이 필요합니다. 비밀번호 확인을 먼저 진행해주세요."))
    }

    @Test
    fun `changePin returns 400 when new pin format is invalid`() {
        val ticketRepository = FakePinChangeTicketRepository(
            initialTickets = listOf(validPinChangeTicket(token = "ticket-1", userId = 7L)),
        )
        val mockMvc = mockMvc(
            controller(
                changePinUseCase = testChangePinUseCase(
                    userRepository = FakeUserRepository(listOf(userFixture(id = 7L))),
                    pinChangeTicketRepository = ticketRepository,
                ),
            ),
        )

        mockMvc.perform(
            put("/users/pin")
                .header(AuthHeaders.AUTHENTICATED_USER_ID, "7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ticket": "ticket-1", "new_pin": "abc"}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.new_pin").value("PIN은 4~6자리 숫자여야 합니다."))
    }

    @Test
    fun `changePin returns 401 when authenticated user header is missing`() {
        val mockMvc = mockMvc(controller())

        mockMvc.perform(
            put("/users/pin")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"ticket": "ticket-1", "new_pin": "4321"}"""),
        ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("잘못된 토큰 형식입니다."))
    }
}
