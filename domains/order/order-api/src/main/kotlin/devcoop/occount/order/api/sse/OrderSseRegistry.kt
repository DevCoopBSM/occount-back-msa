package devcoop.occount.order.api.sse

import devcoop.occount.order.application.port.OrderStatusNotifier
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap

@Component
class OrderSseRegistry : OrderStatusNotifier {
    private val emitters = ConcurrentHashMap<String, SseEmitter>()

    fun register(orderId: String, timeoutMs: Long): SseEmitter {
        val emitter = SseEmitter(timeoutMs)
        emitters[orderId] = emitter
        emitter.onCompletion { emitters.remove(orderId) }
        emitter.onTimeout { emitters.remove(orderId) }
        emitter.onError { emitters.remove(orderId) }
        return emitter
    }

    override fun notify(orderId: String, status: OrderStatus, failureReason: String?) {
        val emitter = emitters[orderId] ?: return
        try {
            emitter.send(SseEmitter.event().data(OrderResponse(orderId, status, failureReason)))
            if (status.isFinalForClient()) {
                emitter.complete()
                emitters.remove(orderId)
            }
        } catch (_: Exception) {
            emitters.remove(orderId)
        }
    }
}
