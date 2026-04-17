package devcoop.occount.order.application.usecase.order.create

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderItemPayload
import devcoop.occount.core.common.event.OrderPaymentPayload
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderRequestValidator
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class CreateOrderUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderRepository: OrderRepository,
    private val orderRequestValidator: OrderRequestValidator,
    private val eventPublisher: EventPublisher,
) {
    fun placeOrder(request: OrderRequest, userId: Long?, kioskId: String): OrderResponse {
        val validatedRequest = orderRequestValidator.validate(request)
        val orderId = UUID.randomUUID().toString()

        val createdOrder = orderMutationExecutor.executeInNewTransaction {
            val createdOrder = orderRepository.save(
                OrderAggregate(
                    orderId = orderId,
                    userId = userId,
                    lines = validatedRequest.lines,
                    payment = OrderPayment(
                        totalAmount = validatedRequest.totalAmount,
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
                payment = OrderPaymentPayload(
                    totalAmount = createdOrder.payment.totalAmount,
                ),
                items = createdOrder.lines.map { line ->
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
        private val log = LoggerFactory.getLogger(CreateOrderUseCase::class.java)
        private val TIMEOUT_SECONDS = Duration.ofSeconds(30)
    }
}
