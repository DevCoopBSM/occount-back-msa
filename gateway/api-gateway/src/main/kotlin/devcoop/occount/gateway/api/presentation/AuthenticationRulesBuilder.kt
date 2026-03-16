package devcoop.occount.gateway.api.presentation

import org.springframework.http.HttpMethod
import org.springframework.web.util.pattern.PathPatternParser

class AuthenticationRulesBuilder {
    private val rules = mutableListOf<AuthenticationRule>()

    fun rule(pathPattern: String): AuthenticationRuleBuilder {
        return createRule(null, pathPattern)
    }

    fun rule(method: HttpMethod, pathPattern: String): AuthenticationRuleBuilder {
        return createRule(method, pathPattern)
    }

    private fun createRule(method: HttpMethod?, pathPattern: String): AuthenticationRuleBuilder {
        return AuthenticationRuleBuilder(
            method = method,
            pathPattern = PathPatternParser.defaultInstance.parse(pathPattern),
            sink = rules::add,
        )
    }

    fun build(): List<AuthenticationRule> = rules.toList()
}
