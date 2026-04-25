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
    override fun create(): SseEmitter = SseEmitter(0L)

    override fun emit(emitter: SseEmitter, event: OrderStreamEvent) {
        try {
            emitter.send(
                SseEmitter.event()
                    .name(event.type.name),
            )
        } catch (e: IOException) {
            emitter.completeWithError(e)
        }
    }
}
