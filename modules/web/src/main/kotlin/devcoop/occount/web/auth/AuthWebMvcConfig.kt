package devcoop.occount.web.auth

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * [AuthPrincipalArgumentResolver] 를 MVC 컨트롤러 인자 해석기로 등록한다.
 *
 * 서블릿(WebMvc) 환경에서만 동작하며, WebFlux 기반인 게이트웨이에서는 로드되지 않는다.
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
class AuthWebMvcConfig : WebMvcConfigurer {
    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(AuthPrincipalArgumentResolver())
    }
}
