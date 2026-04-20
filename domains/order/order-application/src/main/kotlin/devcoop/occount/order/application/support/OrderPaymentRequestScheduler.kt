package devcoop.occount.order.application.support

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderItemPayload
import devcoop.occount.core.common.event.OrderPaymentPayload
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.order.application.exception.OrderConcurrencyException
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.exception.OrderTransactionFailedException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.TransactionPort
import devcoop.occount.order.domain.order.OrderAggregate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderPaymentRequestScheduler(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
    private val transactionPort: TransactionPort,
) {
    fun schedulePaymentRequestIfEligible(orderId: String) {
        repeat(OrderRetryPolicy.MAX_RETRY_COUNT) { attempt ->
            try {
                transactionPort.executeInNewTransaction {
                    val persistedOrder = orderRepository.findPersistedById(orderId)
                        ?: throw OrderNotFoundException()
                    val order = persistedOrder.order

                    if (!order.isReadyForPaymentRequest()) {
                        return@executeInNewTransaction
                    }

                    val updatedOrder = orderRepository.save(
                        order.copy(paymentRequested = true),
                        persistedOrder.persistenceVersion,
                    )
                    publishPaymentRequested(updatedOrder, attempt)
                }
                return
            } catch (ex: OrderConcurrencyException) {
                log.warn("결제 요청 스케줄링 중 낙관적 락 충돌 - 주문={} 시도={}", orderId, attempt)
                if (attempt == OrderRetryPolicy.MAX_RETRY_COUNT - 1) {
                    throw OrderTransactionFailedException()
                }
                Thread.sleep(OrderRetryPolicy.BASE_BACKOFF_MILLIS * (1L shl attempt))
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
                kioskId = order.kioskId,
                userId = order.userId,
                payment = OrderPaymentPayload(
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
    }
}
