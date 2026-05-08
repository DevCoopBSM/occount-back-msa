package devcoop.occount.order.application.support

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.ItemStockCompensationPayload
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.order.application.exception.OrderNotFoundException
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
    fun scheduleRequiredCompensations(orderId: Long) {
        runCatching {
            scheduleCompensationIfNeeded(
                orderId = orderId,
                shouldMark = { it.shouldRequestPaymentCompensation() },
                mark = { it.copy(paymentCompensationRequested = true) },
                publish = ::publishPaymentCompensationRequested,
            )
        }.onFailure { log.error("결제 보상 처리 실패 - 주문={}", orderId, it) }
        runCatching {
            scheduleCompensationIfNeeded(
                orderId = orderId,
                shouldMark = { it.shouldRequestStockCompensation() },
                mark = { it.copy(stockCompensationRequested = true) },
                publish = ::publishStockCompensationRequested,
            )
        }.onFailure { log.error("재고 보상 처리 실패 - 주문={}", orderId, it) }
    }

    private fun scheduleCompensationIfNeeded(
        orderId: Long,
        shouldMark: (OrderAggregate) -> Boolean,
        mark: (OrderAggregate) -> OrderAggregate,
        publish: (OrderAggregate) -> Unit,
    ) {
        transactionPort.executeInNewTransaction {
            val persisted = loadPersistedOrder(orderId)
            if (!shouldMark(persisted.order)) return@executeInNewTransaction
            val updatedOrder = orderRepository.save(mark(persisted.order), persisted.persistenceVersion)
            publish(updatedOrder)
        }
    }

    private fun loadPersistedOrder(orderId: Long): PersistedOrder {
        return orderRepository.findPersistedById(orderId)
            ?: throw OrderNotFoundException()
    }

    private fun publishPaymentCompensationRequested(order: OrderAggregate) {
        log.info("결제 보상 요청 이벤트 발행 - 주문={}", order.orderId)
        eventPublisher.publish(
            topic = DomainTopics.ORDER_PAYMENT_COMPENSATION_REQUESTED,
            key = order.orderId.toString(),
            eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATION_REQUESTED,
            payload = OrderPaymentCompensationRequestedEvent(
                orderId = order.orderId,
                kioskId = order.kioskId,
                userId = order.userId,
                paymentLogId = order.paymentResult.paymentLogId,
                pointsUsed = order.paymentResult.pointsUsed,
                cardAmount = order.paymentResult.cardAmount,
            ),
        )
    }

    private fun publishStockCompensationRequested(order: OrderAggregate) {
        log.info("재고 보상 요청 이벤트 발행 - 주문={}", order.orderId)
        eventPublisher.publish(
            topic = DomainTopics.ITEM_STOCK_COMPENSATION_REQUESTED,
            key = order.orderId.toString(),
            eventType = DomainEventTypes.ITEM_STOCK_COMPENSATION_REQUESTED,
            payload = OrderStockCompensationRequestedEvent(
                orderId = order.orderId,
                items = order.lines.map { line ->
                    ItemStockCompensationPayload(
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
}
