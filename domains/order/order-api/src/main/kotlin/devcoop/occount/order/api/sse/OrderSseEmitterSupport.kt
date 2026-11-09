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
            emitter.send(
                SseEmitter.event()
                    .name(event.type.name.lowercase())
                    .data(eventData(event)),
            )
        } catch (e: IOException) {
            emitter.completeWithError(e)
        }
    }

    companion object {
        private const val SSE_TIMEOUT_MS = 60 * 60 * 1000L

        /**
         * SSE data 페이로드. Map 키는 Jackson property-naming-strategy(SNAKE_CASE)를 타지 않으므로
         * 외부 JSON 계약(snake_case)에 맞춰 키를 직접 명시한다.
         */
        internal fun eventData(event: OrderStreamEvent): Map<String, Any?> = buildMap {
            put("order_id", event.orderId)
            event.failureReason?.let { put("failure_reason", it) }
        }
    }
}
