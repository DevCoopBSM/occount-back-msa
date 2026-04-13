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
import devcoop.occount.order.application.support.OrderPendingResultRegistry
import devcoop.occount.order.application.support.OrderRequestValidator
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderStatus
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture

@Service
class CreateOrderUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderRepository: OrderRepository,
    private val orderRequestValidator: OrderRequestValidator,
    private val orderPendingResultRegistry: OrderPendingResultRegistry,
    private val expireOrderUseCase: ExpireOrderUseCase,
    private val eventPublisher: EventPublisher,
    @param:Value("\${order.timeout-seconds:30}") private val timeoutSeconds: Long,
) {
    fun placeOrder(request: OrderRequest, userId: Long): CompletableFuture<OrderResponse> {
        val validatedRequest = orderRequestValidator.validate(request)
        val orderId = UUID.randomUUID().toString()
        val responseFuture = orderPendingResultRegistry.registerPendingOrder(orderId, timeoutSeconds) {
            log.warn("주문 처리 시간 초과 - 주문={}", orderId)
            expireOrderUseCase.expire(orderId)
        }

        try {
            val createdOrder = orderMutationExecutor.executeInNewTransaction {
                val savedOrder = orderRepository.save(
                    OrderAggregate(
                        orderId = orderId,
                        userId = userId,
                        lines = validatedRequest.lines,
                        payment = OrderPayment(
                            type = request.paymentType,
                            totalAmount = validatedRequest.totalAmount,
                        ),
                        status = OrderStatus.PROCESSING,
                        expiresAt = Instant.now().plus(Duration.ofSeconds(timeoutSeconds)),
                    ),
                )

                publishOrderRequested(savedOrder, userId)
                savedOrder
            }
            log.info("주문 생성 완료 - 주문={} 사용자={}", createdOrder.orderId, userId)
        } catch (ex: Exception) {
            orderPendingResultRegistry.failPendingOrder(orderId, ex)
        }

        return responseFuture
    }

    private fun publishOrderRequested(createdOrder: OrderAggregate, userId: Long) {
        eventPublisher.publish(
            topic = DomainTopics.ORDER_REQUESTED,
            key = createdOrder.orderId,
            eventType = DomainEventTypes.ORDER_REQUESTED,
            payload = OrderRequestedEvent(
                orderId = createdOrder.orderId,
                userId = userId,
                payment = OrderPaymentPayload(
                    type = createdOrder.payment.type,
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
    }
}
