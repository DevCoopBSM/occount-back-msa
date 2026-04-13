package devcoop.occount.order.application.support

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderItemPayload
import devcoop.occount.core.common.event.OrderPaymentPayload
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.domain.order.OrderAggregate
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class OrderPaymentRequestScheduler(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun schedulePaymentRequestIfEligible(orderId: String) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    val persistedOrder = orderRepository.findPersistedById(orderId)
                        ?: throw OrderNotFoundException()
                    val order = persistedOrder.order

                    if (!order.isReadyForPaymentRequest()) {
                        return@executeWithoutResult
                    }

                    val requestedOrder = orderRepository.save(
                        order.copy(paymentRequested = true),
                        persistedOrder.persistenceVersion,
                    )

                    publishPaymentRequested(requestedOrder, attempt)
                }
                return
            } catch (ex: OptimisticLockingFailureException) {
                log.warn("결제 요청 스케줄링 중 낙관적 락 충돌 - 주문={} 시도={}", orderId, attempt)
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ex
                }
                Thread.sleep(BASE_BACKOFF_MILLIS * (1L shl attempt))
            }
        }
    }

    private fun publishPaymentRequested(order: OrderAggregate, attempt: Int) {
        log.info("결제 요청 이벤트 발행 - 주문={} 시도={}", order.orderId, attempt)
        eventPublisher.publish(
            topic = DomainTopics.ORDER_PAYMENT_REQUESTED,
            key = order.orderId,
            eventType = DomainEventTypes.ORDER_PAYMENT_REQUESTED,
            payload = OrderPaymentRequestedEvent(
                orderId = order.orderId,
                userId = order.userId,
                payment = OrderPaymentPayload(
                    type = order.payment.type,
                    totalAmount = order.payment.totalAmount,
                ),
                items = order.lines.map { line ->
                    OrderItemPayload(
                        itemId = line.itemId,
                        itemName = line.itemNameSnapshot,
                        itemPrice = line.unitPrice,
                        quantity = line.quantity,
                        totalPrice = line.totalPrice,
                    )
                },
            ),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderPaymentRequestScheduler::class.java)
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_BACKOFF_MILLIS = 50L
    }
}
