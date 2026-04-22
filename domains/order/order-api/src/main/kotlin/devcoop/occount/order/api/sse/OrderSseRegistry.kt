package devcoop.occount.order.api.sse

import devcoop.occount.order.application.port.OrderStatusNotifier
import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Component
class OrderSseRegistry(
    private val emitterSupport: OrderSseEmitterSupport,
) : OrderStatusNotifier {
    private val sessions = ConcurrentHashMap<String, Session>()

    fun register(current: OrderStreamEvent): SseEmitter {
        val emitter = emitterSupport.create()

        if (current.payload.status.isFinalForClient()) {
            emitterSupport.emit(emitter, current)
            emitter.complete()
            return emitter
        }

        val session = Session(
            emitter = emitter,
            lastEmitted = AtomicReference(current),
        )
        val previous = sessions.put(current.payload.orderId, session)
        attachLifecycle(current.payload.orderId, session)
        previous?.emitter?.complete()
        emitterSupport.emit(emitter, current)

        return emitter
    }

    override fun notify(event: OrderStreamEvent) {
        val session = sessions[event.payload.orderId] ?: return
        if (session.lastEmitted.get() == event) {
            return
        }

        session.lastEmitted.set(event)
        emitterSupport.emit(session.emitter, event)
        if (event.payload.status.isFinalForClient()) {
            session.emitter.complete()
            sessions.remove(event.payload.orderId, session)
        }
    }

    private fun attachLifecycle(orderId: String, session: Session) {
        session.emitter.onCompletion {
            sessions.remove(orderId, session)
        }
        session.emitter.onTimeout {
            session.emitter.complete()
            sessions.remove(orderId, session)
        }
        session.emitter.onError {
            sessions.remove(orderId, session)
        }
    }

    private data class Session(
        val emitter: SseEmitter,
        val lastEmitted: AtomicReference<OrderStreamEvent>,
    )
}
