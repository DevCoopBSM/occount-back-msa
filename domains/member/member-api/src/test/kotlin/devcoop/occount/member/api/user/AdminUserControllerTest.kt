package devcoop.occount.member.api.user

import devcoop.occount.member.api.support.ApiAdviceHandler
import devcoop.occount.member.api.support.FakeUserRepository
import devcoop.occount.member.api.support.userFixture
import devcoop.occount.member.application.query.AdminUserQueryService
import org.junit.jupiter.api.Test
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.PropertyNamingStrategies
import tools.jackson.module.kotlin.jacksonMapperBuilder

class AdminUserControllerTest {
    private fun mockMvc(controller: AdminUserController): MockMvc {
        val objectMapper = jacksonMapperBuilder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build()
        return MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(ApiAdviceHandler())
            .setCustomArgumentResolvers(PageableHandlerMethodArgumentResolver())
            .setMessageConverters(JacksonJsonHttpMessageConverter(objectMapper))
            .build()
    }

    private fun controller(users: List<devcoop.occount.member.domain.user.User>): AdminUserController =
        AdminUserController(AdminUserQueryService(FakeUserRepository(initialUsers = users)))

    @Test
    fun `findAllUsers returns 200 with snake_case paging meta`() {
        val users = (1L..23L).map { userFixture(id = it, email = "user$it@test.com") }
        val mockMvc = mockMvc(controller(users))

        mockMvc.perform(get("/users").param("page", "0").param("size", "10"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.users.length()").value(10))
            .andExpect(jsonPath("$.total_count").value(23))
            .andExpect(jsonPath("$.total_pages").value(3))
            .andExpect(jsonPath("$.current_page").value(0))
            .andExpect(jsonPath("$.page_size").value(10))
    }

    @Test
    fun `findAllUsers defaults to size 10 when size param absent`() {
        val users = (1L..23L).map { userFixture(id = it, email = "user$it@test.com") }
        val mockMvc = mockMvc(controller(users))

        mockMvc.perform(get("/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.users.length()").value(10))
            .andExpect(jsonPath("$.page_size").value(10))
    }

    @Test
    fun `findAllUsers filters by keyword query param`() {
        val users = listOf(
            userFixture(id = 1L, username = "김철수", email = "chulsoo@test.com"),
            userFixture(id = 2L, username = "이영희", email = "younghee@test.com"),
            userFixture(id = 3L, username = "박철수", email = "park@test.com"),
        )
        val mockMvc = mockMvc(controller(users))

        mockMvc.perform(get("/users").param("keyword", "철수"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.users.length()").value(2))
            .andExpect(jsonPath("$.total_count").value(2))
            .andExpect(jsonPath("$.users[0].username").value("김철수"))
            .andExpect(jsonPath("$.users[1].username").value("박철수"))
    }

    @Test
    fun `findAllUsers returns full list when keyword is blank`() {
        val users = (1L..23L).map { userFixture(id = it, email = "user$it@test.com") }
        val mockMvc = mockMvc(controller(users))

        mockMvc.perform(get("/users").param("keyword", "   "))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.total_count").value(23))
    }

    @Test
    fun `findAllUsers exposes only safe fields and omits sensitive keys`() {
        val mockMvc = mockMvc(controller(listOf(userFixture(id = 1L, username = "홍길동", barcode = "BARCODE-1"))))

        mockMvc.perform(get("/users"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.users[0].id").value(1))
            .andExpect(jsonPath("$.users[0].username").value("홍길동"))
            .andExpect(jsonPath("$.users[0].user_barcode").value("BARCODE-1"))
            .andExpect(jsonPath("$.users[0].password").doesNotExist())
            .andExpect(jsonPath("$.users[0].pin").doesNotExist())
            .andExpect(jsonPath("$.users[0].user_pin").doesNotExist())
            .andExpect(jsonPath("$.users[0].ci_number").doesNotExist())
    }
}
