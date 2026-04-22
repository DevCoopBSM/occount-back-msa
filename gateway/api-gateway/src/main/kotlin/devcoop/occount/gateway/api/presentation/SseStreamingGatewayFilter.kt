package devcoop.occount.gateway.api.presentation

import org.reactivestreams.Publisher
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.core.io.buffer.DataBuffer
import org.springframework.http.MediaType
import org.springframework.http.server.reactive.ServerHttpResponse
import org.springframework.http.server.reactive.ServerHttpResponseDecorator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class SseStreamingGatewayFilter : GlobalFilter, Ordered {
    override fun getOrder(): Int = Ordered.HIGHEST_PRECEDENCE + 1

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        return chain.filter(exchange.mutate().response(SseFlushingResponse(exchange.response)).build())
    }
}

private class SseFlushingResponse(delegate: ServerHttpResponse) : ServerHttpResponseDecorator(delegate) {
    override fun writeWith(body: Publisher<out DataBuffer>): Mono<Void> {
        if (headers.contentType?.isCompatibleWith(MediaType.TEXT_EVENT_STREAM) == true) {
            return writeAndFlushWith(Flux.from(body).map { Flux.just(it) })
        }
        return super.writeWith(body)
    }
}
