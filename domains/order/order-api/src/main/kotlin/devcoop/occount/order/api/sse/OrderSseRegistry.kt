package devcoop.occount.order.api.sse

import devcoop.occount.order.application.port.OrderStatusNotifier
import devcoop.occount.order.application.shared.OrderStreamEvent
import devcoop.occount.order.application.shared.isTerminal
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

@Component
class OrderSseRegistry(
    private val emitterSupport: OrderSseEmitterSupport,
) : OrderStatusNotifier {
    private val sessions = ConcurrentHashMap<Long, Session>()

    fun register(orderId: Long, getCurrentEvent: () -> OrderStreamEvent): SseEmitter {
        val emitter = emitterSupport.create()

        // 1. 구독 먼저 등록 → notify() 유실 방지
        val session = Session(emitter = emitter, lastEmitted = AtomicReference(null))
        val previous = sessions.put(orderId, session)
        attachLifecycle(orderId, session)
        previous?.emitter?.complete()

        // 2. 등록 후 현재 상태 조회
        val current = try {
            getCurrentEvent()
        } catch (e: Exception) {
            sessions.remove(orderId, session)
            emitter.completeWithError(e)
            return emitter
        }

        if (current.type.isTerminal()) {
            emitterSupport.emit(emitter, current)
            emitter.complete()
            sessions.remove(orderId, session)
            return emitter
        }

        // 3. 구독과 조회 사이에 notify()가 먼저 도착했으면 초기 이벤트 전송 생략
        if (session.lastEmitted.compareAndSet(null, current)) {
            emitterSupport.emit(emitter, current)
        }

        return emitter
    }

    override fun notify(event: OrderStreamEvent) {
        val session = sessions[event.orderId] ?: return
        val prev = session.lastEmitted.getAndSet(event)
        if (prev?.type == event.type) return

        emitterSupport.emit(session.emitter, event)
        if (event.type.isTerminal()) {
            session.emitter.complete()
            sessions.remove(event.orderId, session)
        }
    }

    private fun attachLifecycle(orderId: Long, session: Session) {
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
