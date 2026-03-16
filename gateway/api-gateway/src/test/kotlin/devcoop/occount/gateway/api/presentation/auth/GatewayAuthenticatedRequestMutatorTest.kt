package devcoop.occount.gateway.api.presentation.auth

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.gateway.api.application.AuthenticatedUser
import devcoop.occount.gateway.api.presentation.GatewayAuthenticatedRequestMutator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest

class GatewayAuthenticatedRequestMutatorTest {
    private val mutator = GatewayAuthenticatedRequestMutator()

    @Test
    fun `mutate writes trusted headers and strips client authentication headers`() {
        val request = MockServerHttpRequest.get("/api/v3/payments/execute")
            .header(HttpHeaders.AUTHORIZATION, "Bearer kiosk-token")
            .header(AuthHeaders.AUTHENTICATED_USER_ID, "999")
            .build()

        val mutatedRequest = mutator.mutate(request, AuthenticatedUser(7L, "ROLE_USER"))

        assertNull(mutatedRequest.headers.getFirst(HttpHeaders.AUTHORIZATION))
        assertEquals("7", mutatedRequest.headers.getFirst(AuthHeaders.AUTHENTICATED_USER_ID))
    }
}
