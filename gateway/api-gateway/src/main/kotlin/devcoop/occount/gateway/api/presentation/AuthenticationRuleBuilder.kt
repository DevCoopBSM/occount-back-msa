package devcoop.occount.gateway.api.presentation

import org.springframework.http.HttpMethod
import org.springframework.web.util.pattern.PathPattern

class AuthenticationRuleBuilder(
    private val method: HttpMethod?,
    private val pathPattern: PathPattern,
    private val sink: (AuthenticationRule) -> Unit,
) {
    fun permitAll() {
        sink(AuthenticationRule(method, pathPattern, access = AuthenticationRule.Access.PERMIT_ALL))
    }

    fun authenticated() {
        sink(AuthenticationRule(method, pathPattern, access = AuthenticationRule.Access.AUTHENTICATED))
    }

    fun adminOnly() {
        sink(AuthenticationRule(method, pathPattern, access = AuthenticationRule.Access.ADMIN_ONLY))
    }
}
