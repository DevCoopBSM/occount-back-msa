package devcoop.occount.gateway.api.presentation.auth

import devcoop.occount.gateway.api.application.AuthenticatedUser
import devcoop.occount.gateway.api.presentation.GatewayAuthenticatedRequestMutator
import devcoop.occount.gateway.api.presentation.AuthenticationFailureWriter
import devcoop.occount.gateway.api.presentation.GatewayAuthenticationFilter
import devcoop.occount.gateway.api.presentation.AuthenticationPolicy
import devcoop.occount.gateway.api.presentation.AuthenticationRule
import devcoop.occount.gateway.api.application.TokenAuthenticator
import devcoop.occount.gateway.api.infrastructure.InvalidTokenException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

class GatewayAuthenticationFilterTest {
    private val authenticationPolicy = mock(AuthenticationPolicy::class.java)
    private val tokenAuthenticator = mock(TokenAuthenticator::class.java)
    private val authenticatedRequestMutator = mock(GatewayAuthenticatedRequestMutator::class.java)
    private val authenticationFailureWriter = mock(AuthenticationFailureWriter::class.java)
    private val filter = GatewayAuthenticationFilter(
        authenticationPolicy,
        tokenAuthenticator,
        authenticatedRequestMutator,
        authenticationFailureWriter,
    )

    @Test
    fun `protected request authenticates and forwards mutated request`() {
        val request = MockServerHttpRequest.get("/api/v3/payments/execute")
            .header(HttpHeaders.AUTHORIZATION, "Bearer kiosk-token")
            .build()
        val exchange = MockServerWebExchange.from(request)
        val authenticatedUser = AuthenticatedUser(userId = 7L, role = "ROLE_USER")
        val mutatedRequest = request.mutate().header("X-Test", "forwarded").build()
        var forwardedExchange: ServerWebExchange? = null
        val chain = GatewayFilterChain { nextExchange ->
            forwardedExchange = nextExchange
            Mono.empty()
        }

        `when`(authenticationPolicy.resolveAccess(request.method, request.path.value()))
            .thenReturn(AuthenticationRule.Access.AUTHENTICATED)
        `when`(tokenAuthenticator.authenticate("Bearer kiosk-token")).thenReturn(authenticatedUser)
        `when`(authenticatedRequestMutator.mutate(request, authenticatedUser)).thenReturn(mutatedRequest)

        filter.filter(exchange, chain).block()

        assertEquals("forwarded", forwardedExchange?.request?.headers?.getFirst("X-Test"))
        verify(tokenAuthenticator).authenticate("Bearer kiosk-token")
        verify(authenticatedRequestMutator).mutate(request, authenticatedUser)
        verifyNoInteractions(authenticationFailureWriter)
    }

    @Test
    fun `authentication failure delegates to unauthorized writer`() {
        val request = MockServerHttpRequest.get("/api/v3/orders/current")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
            .build()
        val exchange = MockServerWebExchange.from(request)
        val chain = GatewayFilterChain { Mono.empty<Void>() }
        val expected = Mono.empty<Void>()
        val exception = InvalidTokenException()

        `when`(authenticationPolicy.resolveAccess(request.method, request.path.value()))
            .thenReturn(AuthenticationRule.Access.AUTHENTICATED)
        `when`(tokenAuthenticator.authenticate("Bearer invalid-token"))
            .thenThrow(exception)
        `when`(authenticationFailureWriter.writeUnauthorized(exchange, exception)).thenReturn(expected)

        val actual = filter.filter(exchange, chain)

        assertSame(expected, actual)
        verify(authenticationFailureWriter).writeUnauthorized(exchange, exception)
        verifyNoInteractions(authenticatedRequestMutator)
    }

    @Test
    fun `unprotected request bypasses authentication`() {
        val request = MockServerHttpRequest.get("/api/v3/items/88012341234").build()
        val exchange = MockServerWebExchange.from(request)
        var forwardedExchange: ServerWebExchange? = null
        val chain = GatewayFilterChain { nextExchange ->
            forwardedExchange = nextExchange
            Mono.empty()
        }

        `when`(authenticationPolicy.resolveAccess(request.method, request.path.value()))
            .thenReturn(AuthenticationRule.Access.PERMIT_ALL)

        filter.filter(exchange, chain).block()

        assertNotNull(forwardedExchange)
        assertTrue(forwardedExchange === exchange)
        verifyNoInteractions(tokenAuthenticator, authenticatedRequestMutator, authenticationFailureWriter)
    }

    @Test
    fun `optional auth request with valid token forwards mutated request`() {
        val request = MockServerHttpRequest.post("/api/v3/orders")
            .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
            .build()
        val exchange = MockServerWebExchange.from(request)
        val authenticatedUser = AuthenticatedUser(userId = 7L, role = "ROLE_USER")
        val mutatedRequest = request.mutate().header("X-Test", "forwarded").build()
        var forwardedExchange: ServerWebExchange? = null
        val chain = GatewayFilterChain { nextExchange ->
            forwardedExchange = nextExchange
            Mono.empty()
        }

        `when`(authenticationPolicy.resolveAccess(request.method, request.path.value()))
            .thenReturn(AuthenticationRule.Access.OPTIONAL_AUTH)
        `when`(tokenAuthenticator.authenticate("Bearer user-token")).thenReturn(authenticatedUser)
        `when`(authenticatedRequestMutator.mutate(request, authenticatedUser)).thenReturn(mutatedRequest)

        filter.filter(exchange, chain).block()

        assertEquals("forwarded", forwardedExchange?.request?.headers?.getFirst("X-Test"))
        verify(tokenAuthenticator).authenticate("Bearer user-token")
        verify(authenticatedRequestMutator).mutate(request, authenticatedUser)
        verifyNoInteractions(authenticationFailureWriter)
    }

    @Test
    fun `optional auth request without token bypasses authentication`() {
        val request = MockServerHttpRequest.post("/api/v3/orders").build()
        val exchange = MockServerWebExchange.from(request)
        var forwardedExchange: ServerWebExchange? = null
        val chain = GatewayFilterChain { nextExchange ->
            forwardedExchange = nextExchange
            Mono.empty()
        }

        `when`(authenticationPolicy.resolveAccess(request.method, request.path.value()))
            .thenReturn(AuthenticationRule.Access.OPTIONAL_AUTH)

        filter.filter(exchange, chain).block()

        assertNotNull(forwardedExchange)
        assertTrue(forwardedExchange === exchange)
        verifyNoInteractions(tokenAuthenticator, authenticatedRequestMutator, authenticationFailureWriter)
    }

    @Test
    fun `optional auth request with invalid token returns unauthorized`() {
        val request = MockServerHttpRequest.post("/api/v3/orders")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
            .build()
        val exchange = MockServerWebExchange.from(request)
        val chain = GatewayFilterChain { Mono.empty<Void>() }
        val expected = Mono.empty<Void>()
        val exception = InvalidTokenException()

        `when`(authenticationPolicy.resolveAccess(request.method, request.path.value()))
            .thenReturn(AuthenticationRule.Access.OPTIONAL_AUTH)
        `when`(tokenAuthenticator.authenticate("Bearer invalid-token"))
            .thenThrow(exception)
        `when`(authenticationFailureWriter.writeUnauthorized(exchange, exception)).thenReturn(expected)

        val actual = filter.filter(exchange, chain)

        assertSame(expected, actual)
        verify(authenticationFailureWriter).writeUnauthorized(exchange, exception)
        verifyNoInteractions(authenticatedRequestMutator)
    }

    @Test
    fun `admin only request returns forbidden for non admin user`() {
        val request = MockServerHttpRequest.post("/api/v3/items/sync")
            .header(HttpHeaders.AUTHORIZATION, "Bearer user-token")
            .build()
        val exchange = MockServerWebExchange.from(request)
        val chain = GatewayFilterChain { Mono.empty<Void>() }
        val expected = Mono.empty<Void>()

        `when`(authenticationPolicy.resolveAccess(request.method, request.path.value()))
            .thenReturn(AuthenticationRule.Access.ADMIN_ONLY)
        `when`(tokenAuthenticator.authenticate("Bearer user-token"))
            .thenReturn(AuthenticatedUser(userId = 7L, role = "ROLE_USER"))
        `when`(authenticationFailureWriter.writeForbidden(exchange)).thenReturn(expected)

        val actual = filter.filter(exchange, chain)

        assertSame(expected, actual)
        verify(authenticationFailureWriter).writeForbidden(exchange)
        verifyNoInteractions(authenticatedRequestMutator)
    }
}
