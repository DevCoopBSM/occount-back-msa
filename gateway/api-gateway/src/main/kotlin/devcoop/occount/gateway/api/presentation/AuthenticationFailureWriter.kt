package devcoop.occount.gateway.api.presentation

import com.fasterxml.jackson.databind.ObjectMapper
import devcoop.occount.core.common.error.ErrorResponse
import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.exception.BusinessBaseException
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@Component
class AuthenticationFailureWriter(
    private val objectMapper: ObjectMapper,
) {
    fun writeUnauthorized(
        exchange: ServerWebExchange,
        exception: BusinessBaseException,
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.UNAUTHORIZED
        response.headers.contentType = MediaType.APPLICATION_JSON
        val body = objectMapper.writeValueAsBytes(ErrorResponse.of(exception.errorMessage))
        val buffer = response.bufferFactory().wrap(body)
        return response.writeWith(Mono.just(buffer))
    }

    fun writeForbidden(exchange: ServerWebExchange): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.FORBIDDEN
        response.headers.contentType = MediaType.APPLICATION_JSON
        val body = objectMapper.writeValueAsBytes(ErrorResponse.of(ErrorMessage.ACCESS_DENIED))
        val buffer = response.bufferFactory().wrap(body)
        return response.writeWith(Mono.just(buffer))
    }
}
