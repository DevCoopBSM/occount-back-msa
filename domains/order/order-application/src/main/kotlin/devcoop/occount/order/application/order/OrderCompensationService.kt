package devcoop.occount.order.application.order

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompensationItemPayload
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class OrderCompensationService(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun scheduleCompensations(orderId: String) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    var current = orderRepository.findById(orderId)
                        ?: throw OrderNotFoundException()

                    if (current.shouldRequestPaymentCompensation()) {
                        current = orderRepository.save(
                            current.copy(paymentCompensationRequested = true),
                        )
                        log.info("결제 보상 요청 이벤트 발행 - 주문={} 시도={}", orderId, attempt)
                        eventPublisher.publish(
                            topic = DomainTopics.ORDER_PAYMENT_COMPENSATION_REQUESTED,
                            key = current.orderId,
                            eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATION_REQUESTED,
                            payload = OrderPaymentCompensationRequestedEvent(
                                orderId = current.orderId,
                                userId = current.userId,
                                paymentLogId = current.paymentResult.paymentLogId,
                                pointsUsed = current.paymentResult.pointsUsed,
                                cardAmount = current.paymentResult.cardAmount,
                            ),
                        )
                    }

                    if (current.shouldRequestStockCompensation()) {
                        current = orderRepository.save(
                            current.copy(stockCompensationRequested = true),
                        )
                        log.info("재고 보상 요청 이벤트 발행 - 주문={} 시도={}", orderId, attempt)
                        eventPublisher.publish(
                            topic = DomainTopics.ORDER_STOCK_COMPENSATION_REQUESTED,
                            key = current.orderId,
                            eventType = DomainEventTypes.ORDER_STOCK_COMPENSATION_REQUESTED,
                            payload = OrderStockCompensationRequestedEvent(
                                orderId = current.orderId,
                                items = current.lines.map { line ->
                                    OrderStockCompensationItemPayload(
                                        itemId = line.itemId,
                                        quantity = line.quantity,
                                    )
                                },
                            ),
                        )
                    }
                }
                return
            } catch (ex: OptimisticLockingFailureException) {
                log.warn("보상 처리 중 낙관적 락 충돌 - 주문={} 시도={}", orderId, attempt)
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ex
                }
                backoff(attempt)
            }
        }
    }

    private fun backoff(attempt: Int) {
        Thread.sleep(BASE_BACKOFF_MILLIS * (1L shl attempt))
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderCompensationService::class.java)
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_BACKOFF_MILLIS = 50L
    }
}
