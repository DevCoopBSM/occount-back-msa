package devcoop.occount.order.application.support

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompensationItemPayload
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.order.application.exception.OrderConcurrencyException
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.exception.OrderTransactionFailedException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.application.output.TransactionPort
import devcoop.occount.order.domain.order.OrderAggregate
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class OrderCompensationScheduler(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
    private val transactionPort: TransactionPort,
) {
    fun scheduleRequiredCompensations(orderId: String) {
        repeat(OrderRetryPolicy.MAX_RETRY_COUNT) { attempt ->
            try {
                var compensationsToPublish: RequestedCompensations? = null
                transactionPort.executeInNewTransaction {
                    val persistedOrder = loadPersistedOrder(orderId)
                    compensationsToPublish = markRequestedCompensations(persistedOrder)
                }
                // DB 커밋 후 이벤트 발행 — 트랜잭션 내 발행 시 DB 롤백과 이벤트 불일치 방지
                compensationsToPublish?.let { publishRequestedCompensations(it, attempt) }
                return
            } catch (ex: OrderConcurrencyException) {
                log.warn("보상 처리 중 낙관적 락 충돌 - 주문={} 시도={}", orderId, attempt)
                if (attempt == OrderRetryPolicy.MAX_RETRY_COUNT - 1) {
                    throw OrderTransactionFailedException()
                }
                backoff(attempt)
            }
        }
    }

    private fun backoff(attempt: Int) {
        Thread.sleep(OrderRetryPolicy.BASE_BACKOFF_MILLIS * (1L shl attempt))
    }

    private fun loadPersistedOrder(orderId: String): PersistedOrder {
        return orderRepository.findPersistedById(orderId)
            ?: throw OrderNotFoundException()
    }

    private fun markRequestedCompensations(persistedOrder: PersistedOrder): RequestedCompensations? {
        val order = persistedOrder.order
        val shouldRequestPaymentCompensation = order.shouldRequestPaymentCompensation()
        val shouldRequestStockCompensation = order.shouldRequestStockCompensation()

        if (!shouldRequestPaymentCompensation && !shouldRequestStockCompensation) {
            return null
        }

        val updatedOrder = orderRepository.save(
            order.copy(
                paymentCompensationRequested = order.paymentCompensationRequested || shouldRequestPaymentCompensation,
                stockCompensationRequested = order.stockCompensationRequested || shouldRequestStockCompensation,
            ),
            persistedOrder.persistenceVersion,
        )

        return RequestedCompensations(
            order = updatedOrder,
            paymentCompensationRequested = shouldRequestPaymentCompensation,
            stockCompensationRequested = shouldRequestStockCompensation,
        )
    }

    private fun publishRequestedCompensations(requestedCompensations: RequestedCompensations, attempt: Int) {
        if (requestedCompensations.paymentCompensationRequested) {
            publishPaymentCompensationRequested(requestedCompensations.order, attempt)
        }

        if (requestedCompensations.stockCompensationRequested) {
            publishStockCompensationRequested(requestedCompensations.order, attempt)
        }
    }

    private fun publishPaymentCompensationRequested(order: OrderAggregate, attempt: Int) {
        log.info("결제 보상 요청 이벤트 발행 - 주문={} 시도={}", order.orderId, attempt)
        eventPublisher.publish(
            topic = DomainTopics.ORDER_PAYMENT_COMPENSATION_REQUESTED,
            key = order.orderId,
            eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATION_REQUESTED,
            payload = OrderPaymentCompensationRequestedEvent(
                orderId = order.orderId,
                userId = order.userId,
                paymentLogId = order.paymentResult.paymentLogId,
                pointsUsed = order.paymentResult.pointsUsed,
                cardAmount = order.paymentResult.cardAmount,
            ),
        )
    }

    private fun publishStockCompensationRequested(order: OrderAggregate, attempt: Int) {
        log.info("재고 보상 요청 이벤트 발행 - 주문={} 시도={}", order.orderId, attempt)
        eventPublisher.publish(
            topic = DomainTopics.ORDER_STOCK_COMPENSATION_REQUESTED,
            key = order.orderId,
            eventType = DomainEventTypes.ORDER_STOCK_COMPENSATION_REQUESTED,
            payload = OrderStockCompensationRequestedEvent(
                orderId = order.orderId,
                items = order.lines.map { line ->
                    OrderStockCompensationItemPayload(
                        itemId = line.itemId,
                        quantity = line.quantity,
                    )
                },
            ),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(OrderCompensationScheduler::class.java)
    }

    private data class RequestedCompensations(
        val order: OrderAggregate,
        val paymentCompensationRequested: Boolean,
        val stockCompensationRequested: Boolean,
    )
}
