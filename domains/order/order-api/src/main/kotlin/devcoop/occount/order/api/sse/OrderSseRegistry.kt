package devcoop.occount.order.api.sse

import devcoop.occount.order.application.port.OrderStatusNotifier
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.util.concurrent.ConcurrentHashMap

@Component
class OrderSseRegistry : OrderStatusNotifier {
    private val sinks = ConcurrentHashMap<String, Sinks.Many<ServerSentEvent<OrderResponse>>>()

    fun register(orderId: String): Flux<ServerSentEvent<OrderResponse>> {
        val sink = Sinks.many().unicast().onBackpressureBuffer<ServerSentEvent<OrderResponse>>()
        sinks[orderId] = sink
        return sink.asFlux().doFinally { sinks.remove(orderId) }
    }

    override fun notify(orderId: String, status: OrderStatus, failureReason: String?) {
        val sink = sinks[orderId] ?: return
        sink.tryEmitNext(
            ServerSentEvent.builder(OrderResponse(orderId, status, failureReason))
                .event(status.name)
                .build(),
        )
        if (status.isFinalForClient()) {
            sink.tryEmitComplete()
            sinks.remove(orderId)
        }
    }
}