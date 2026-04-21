package devcoop.occount.gateway.api.presentation

import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component

@Component
class AuthenticationPolicy {
    private val rules = authenticationRules {
        rule("/api/v3/actuator/**").permitAll()
        rule("/api/v3/auth").permitAll()
        rule("/api/v3/auth/**").permitAll()

        rule(HttpMethod.GET, "/api/v3/items/without-barcode").permitAll()
        rule(HttpMethod.GET, "/api/v3/items/categories").permitAll()
        rule(HttpMethod.GET, "/api/v3/items/{barcode}").permitAll()
        rule(HttpMethod.GET, "/api/v3/items").permitAll()

        rule("/api/v3/users/**").authenticated()
        rule(HttpMethod.POST, "/api/v3/orders").optionalAuth()
        rule(HttpMethod.POST, "/api/v3/orders/{orderId}/cancel").optionalAuth()
        rule(HttpMethod.GET, "/api/v3/orders/{orderId}").optionalAuth()
        rule("/api/v3/orders/**").authenticated()
        rule("/api/v3/points/**").authenticated()
        rule("/api/v3/wallet/**").authenticated()

        rule(HttpMethod.POST, "/api/v3/items").adminOnly()
        rule(HttpMethod.POST, "/api/v3/items/sync").adminOnly()
        rule(HttpMethod.PUT, "/api/v3/items/{id}").adminOnly()
        rule(HttpMethod.DELETE, "/api/v3/items/{id}").adminOnly()
    }

    fun resolveAccess(method: HttpMethod?, path: String): AuthenticationRule.Access {
        if (method == null || method == HttpMethod.OPTIONS) {
            return AuthenticationRule.Access.PERMIT_ALL
        }

        return rules
            .firstOrNull { it.matches(method, path) }
            ?.access
            ?: AuthenticationRule.Access.AUTHENTICATED
    }

    private fun authenticationRules(init: AuthenticationRulesBuilder.() -> Unit): List<AuthenticationRule> {
        return AuthenticationRulesBuilder().apply(init).build()
    }
}
