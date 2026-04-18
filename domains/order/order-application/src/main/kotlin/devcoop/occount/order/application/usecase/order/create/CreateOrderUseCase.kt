package devcoop.occount.order.application.usecase.order.create

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderRequestedItemPayload
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.RequestedOrderLine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class CreateOrderUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
) {
    fun placeOrder(request: OrderRequest, userId: Long?, kioskId: String): OrderResponse {
        val orderId = UUID.randomUUID().toString()
        val requestedLines = request.items.map { RequestedOrderLine(itemId = it.itemId, quantity = it.quantity) }

        val createdOrder = orderMutationExecutor.executeInNewTransaction {
            val createdOrder = orderRepository.save(
                OrderAggregate(
                    orderId = orderId,
                    userId = userId,
                    requestedLines = requestedLines,
                    payment = OrderPayment(
                        totalAmount = 0,
                    ),
                    status = OrderStatus.PROCESSING,
                    kioskId = kioskId,
                    expiresAt = Instant.now().plus(TIMEOUT_SECONDS),
                ),
            )
            publishOrderRequested(createdOrder, userId)
            createdOrder
        }
        log.info("주문 생성 완료 - 주문={} 사용자={}", createdOrder.orderId, userId)

        return OrderResponse(orderId = createdOrder.orderId, status = createdOrder.status)
    }

    private fun publishOrderRequested(createdOrder: OrderAggregate, userId: Long?) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_REQUESTED,
            key = createdOrder.orderId,
            eventType = DomainEventTypes.ORDER_REQUESTED,
            payload = OrderRequestedEvent(
                orderId = createdOrder.orderId,
                userId = userId,
                kioskId = createdOrder.kioskId,
                items = createdOrder.requestedLines.map { line ->
                    OrderRequestedItemPayload(
                        itemId = line.itemId,
                        quantity = line.quantity,
                    )
                },
            ),
        )
    }

    companion object {
        private val log = LoggerFactory.getLogger(CreateOrderUseCase::class.java)
        private val TIMEOUT_SECONDS = Duration.ofSeconds(30)
    }
}
