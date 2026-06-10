package devcoop.occount.member.api.auth

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.AuthUser
import devcoop.occount.core.common.auth.AuthWebMvcConfig
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.mock.web.MockServletContext
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext
import org.springframework.web.servlet.config.annotation.EnableWebMvc

/**
 * [AuthWebMvcConfig] 가 실제 서블릿 웹 컨텍스트에서 [AuthPrincipalArgumentResolver] 를
 * 자동 등록해, DispatcherServlet 경로로 들어온 요청에 @AuthUser 가 주입되는지 검증한다.
 *
 * (단위 컨트롤러 테스트는 standalone MockMvc 에 리졸버를 수동 등록하므로,
 *  자동 등록 경로 자체는 이 테스트로만 검증된다.)
 */
@DisplayName("AuthWebMvcConfig 자동 등록 통합 테스트")
class AuthArgumentResolverIntegrationTest {

    @RestController
    class ProbeController {
        @GetMapping("/__probe/me")
        fun me(@AuthUser principal: AuthPrincipal): String = principal.userId.toString()
    }

    @Configuration
    @EnableWebMvc
    @Import(AuthWebMvcConfig::class)
    class TestWebConfig {
        @Bean
        fun probeController() = ProbeController()
    }

    private fun mockMvc() = AnnotationConfigWebApplicationContext().apply {
        servletContext = MockServletContext()
        register(TestWebConfig::class.java)
        refresh()
    }.let { MockMvcBuilders.webAppContextSetup(it).build() }

    @Test
    @DisplayName("@AuthUser 파라미터에 인증 헤더의 userId가 주입된다")
    fun `resolves auth principal from header`() {
        mockMvc().perform(
            get("/__probe/me").header(AuthHeaders.AUTHENTICATED_USER_ID, "42"),
        ).andExpect(status().isOk)
            .andExpect(content().string("42"))
    }
}
