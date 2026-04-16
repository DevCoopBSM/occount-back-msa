package devcoop.occount.gateway.api.presentation

import org.springframework.http.HttpMethod
import org.springframework.http.server.PathContainer
import org.springframework.web.util.pattern.PathPattern

class AuthenticationRule(
    private val method: HttpMethod?,
    private val pathPattern: PathPattern,
    val access: Access,
) {
    fun matches(requestMethod: HttpMethod, requestPath: String): Boolean {
        return (method == null || method == requestMethod) &&
                pathPattern.matches(PathContainer.parsePath(requestPath))
    }

    enum class Access {
        PERMIT_ALL,
        OPTIONAL_AUTH,
        AUTHENTICATED,
        ADMIN_ONLY,
    }
}
