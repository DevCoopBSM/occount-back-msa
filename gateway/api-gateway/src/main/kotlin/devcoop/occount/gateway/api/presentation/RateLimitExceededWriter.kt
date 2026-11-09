package devcoop.occount.gateway.api.presentation

import devcoop.occount.core.common.error.ErrorMessage
import devcoop.occount.core.common.error.ErrorResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import tools.jackson.databind.ObjectMapper

@Component
class RateLimitExceededWriter(
    private val objectMapper: ObjectMapper,
) {
    fun writeTooManyRequests(
        exchange: ServerWebExchange,
        retryAfterSeconds: Long,
    ): Mono<Void> {
        val response = exchange.response
        response.statusCode = HttpStatus.TOO_MANY_REQUESTS
        response.headers.contentType = MediaType.APPLICATION_JSON
        response.headers.set(HttpHeaders.RETRY_AFTER, retryAfterSeconds.toString())
        val body = objectMapper.writeValueAsBytes(ErrorResponse.of(ErrorMessage.TOO_MANY_REQUESTS))
        val buffer = response.bufferFactory().wrap(body)
        return response.writeWith(Mono.just(buffer))
    }
}
