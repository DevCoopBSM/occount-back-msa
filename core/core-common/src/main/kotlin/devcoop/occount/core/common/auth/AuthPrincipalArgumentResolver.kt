package devcoop.occount.core.common.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer

/**
 * [AuthUser] 가 붙은 [AuthPrincipal] 파라미터를 인증 헤더로부터 해석해 주입한다.
 */
class AuthPrincipalArgumentResolver : HandlerMethodArgumentResolver {
    override fun supportsParameter(parameter: MethodParameter): Boolean =
        parameter.hasParameterAnnotation(AuthUser::class.java) &&
            AuthPrincipal::class.java.isAssignableFrom(parameter.parameterType)

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): AuthPrincipal {
        val request = webRequest.getNativeRequest(HttpServletRequest::class.java)
            ?: throw InvalidAuthenticatedRequestException()
        return RequestAuthPrincipalResolver.resolve(request)
    }
}
