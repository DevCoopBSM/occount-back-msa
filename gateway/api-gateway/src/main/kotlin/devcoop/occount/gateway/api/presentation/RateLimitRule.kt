package devcoop.occount.gateway.api.presentation

import org.springframework.http.HttpMethod
import org.springframework.http.server.PathContainer
import org.springframework.web.util.pattern.PathPattern

class RateLimitRule(
    private val method: HttpMethod?,
    private val pathPattern: PathPattern,
    val spec: RateLimitSpec,
) {
    fun matches(requestMethod: HttpMethod, requestPath: String): Boolean {
        return (method == null || method == requestMethod) &&
                pathPattern.matches(PathContainer.parsePath(requestPath))
    }
}
