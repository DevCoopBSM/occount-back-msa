package devcoop.occount.order.application.support

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompensationItemPayload
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.domain.order.OrderAggregate
import org.slf4j.LoggerFactory
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate

@Service
class OrderCompensationScheduler(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun scheduleRequiredCompensations(orderId: String) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    val persistedOrder = loadPersistedOrder(orderId)
                    val requestedCompensations = markRequestedCompensations(persistedOrder)
                        ?: return@executeWithoutResult

                    publishRequestedCompensations(requestedCompensations, attempt)
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
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_BACKOFF_MILLIS = 50L
    }

    private data class RequestedCompensations(
        val order: OrderAggregate,
        val paymentCompensationRequested: Boolean,
        val stockCompensationRequested: Boolean,
    )
}
