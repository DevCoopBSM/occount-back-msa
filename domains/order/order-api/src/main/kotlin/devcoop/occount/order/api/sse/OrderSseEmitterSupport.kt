package devcoop.occount.order.api.sse

import devcoop.occount.order.application.shared.OrderStreamEvent
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

interface OrderSseEmitterSupport {
    fun create(): SseEmitter

    fun emit(emitter: SseEmitter, event: OrderStreamEvent)
}

@Component
class DefaultOrderSseEmitterSupport : OrderSseEmitterSupport {
    override fun create(): SseEmitter = SseEmitter(SSE_TIMEOUT_MS)

    override fun emit(emitter: SseEmitter, event: OrderStreamEvent) {
        try {
            val data = buildMap<String, Any?> {
                put("orderId", event.orderId)
                event.failureReason?.let { put("failureReason", it) }
            }
            emitter.send(
                SseEmitter.event()
                    .name(event.type.name.lowercase())
                    .data(data),
            )
        } catch (e: IOException) {
            emitter.completeWithError(e)
        }
    }

    companion object {
        private const val SSE_TIMEOUT_MS = 60 * 60 * 1000L
    }
}
