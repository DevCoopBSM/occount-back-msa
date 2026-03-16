package devcoop.occount.gateway.api.presentation

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.gateway.api.application.AuthenticatedUser
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component

@Component
class GatewayAuthenticatedRequestMutator {
    fun mutate(request: ServerHttpRequest, authenticatedUser: AuthenticatedUser): ServerHttpRequest {
        return request.mutate()
            .headers { headers ->
                headers.remove(HttpHeaders.AUTHORIZATION)
                headers.remove(AuthHeaders.AUTHENTICATED_USER_ID)
                headers.set(AuthHeaders.AUTHENTICATED_USER_ID, authenticatedUser.userId.toString())
            }
            .build()
    }
}
