package devcoop.occount.gateway.api.presentation

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.core.common.exception.BusinessBaseException
import devcoop.occount.gateway.api.application.TokenAuthenticator
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class GatewayAuthenticationFilter(
    private val authenticationPolicy: AuthenticationPolicy,
    private val tokenAuthenticator: TokenAuthenticator,
    private val authenticatedRequestMutator: GatewayAuthenticatedRequestMutator,
    private val authenticationFailureWriter: AuthenticationFailureWriter,
) : GlobalFilter, Ordered {
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        val request = exchange.request
        val access = authenticationPolicy.resolveAccess(request.method, request.path.value())
        if (access == AuthenticationRule.Access.PERMIT_ALL) {
            return chain.filter(exchange)
        }

        if (access == AuthenticationRule.Access.OPTIONAL_AUTH) {
            val authHeader = request.headers.getFirst(HttpHeaders.AUTHORIZATION)
                ?: return chain.filter(
                    exchange.mutate().request(
                        request.mutate().headers { it.remove(AuthHeaders.AUTHENTICATED_USER_ID) }.build()
                    ).build()
                )

            val authenticatedUser = try {
                tokenAuthenticator.authenticate(authHeader)
            } catch (e: BusinessBaseException) {
                return authenticationFailureWriter.writeUnauthorized(exchange, e)
            }
            val mutatedRequest = authenticatedRequestMutator.mutate(request, authenticatedUser)
            return chain.filter(exchange.mutate().request(mutatedRequest).build())
        }

        val authenticatedUser = try {
            tokenAuthenticator.authenticate(request.headers.getFirst(HttpHeaders.AUTHORIZATION))
        } catch (e: BusinessBaseException) {
            return authenticationFailureWriter.writeUnauthorized(exchange, e)
        }

        if (access == AuthenticationRule.Access.ADMIN_ONLY && authenticatedUser.role != "ROLE_ADMIN") {
            return authenticationFailureWriter.writeForbidden(exchange)
        }

        val mutatedRequest = authenticatedRequestMutator.mutate(request, authenticatedUser)
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
    }
}
