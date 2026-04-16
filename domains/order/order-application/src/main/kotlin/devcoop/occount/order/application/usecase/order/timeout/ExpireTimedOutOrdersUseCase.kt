package devcoop.occount.order.application.usecase.order.timeout

import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.usecase.order.timeout.ExpireOrderUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ExpireTimedOutOrdersUseCase(
    private val orderRepository: OrderRepository,
    private val expireOrderUseCase: ExpireOrderUseCase,
) {
    fun expire(now: Instant = Instant.now()) {
        orderRepository.findExpiredNonFinalOrderIds(now).forEach { orderId ->
            try {
                expireOrderUseCase.expire(orderId)
            } catch (ex: Exception) {
                log.warn("만료 주문 처리 실패 - 주문={}", orderId, ex)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExpireTimedOutOrdersUseCase::class.java)
    }
}
