package devcoop.occount.order.application.order

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderItemPayload
import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.core.common.event.OrderPaymentPayload
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompensatedEvent
import devcoop.occount.core.common.event.OrderStockCompensationFailedEvent
import devcoop.occount.core.common.event.OrderStockCompensationItemPayload
import devcoop.occount.core.common.event.OrderStockCompensationRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.core.common.event.OrderStockFailedEvent
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
    transactionManager: PlatformTransactionManager,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun order(request: OrderRequest, userId: Long): OrderResponse {
        val order = transactionTemplate.execute {
            val createdOrder = orderRepository.save(
                OrderAggregate(
                    orderId = UUID.randomUUID().toString(),
                    userId = userId,
                    lines = request.orderInfos.map { orderInfo ->
                        OrderLine(
                            itemId = orderInfo.itemId,
                            itemNameSnapshot = orderInfo.itemName,
                            unitPrice = orderInfo.itemPrice,
                            quantity = orderInfo.orderQuantity,
                            totalPrice = orderInfo.totalPrice,
                        )
                    },
                    payment = OrderPayment(
                        type = request.paymentType,
                        totalAmount = request.totalAmount,
                    ),
                    status = OrderStatus.PROCESSING,
                    expiresAt = Instant.now().plus(TIMEOUT),
                ),
            )

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

            createdOrder
        } ?: throw IllegalStateException("Order transaction returned null")

        return awaitResult(order.orderId, userId)
    }

    fun cancel(orderId: String, userId: Long): OrderResponse {
        val updated = updateOrder(orderId) { current ->
            validateOwnership(current, userId)

            if (!current.status.canCancel()) {
                throw OrderCannotCancelException()
            }

            current.copy(
                cancelRequested = true,
                status = OrderStatus.CANCEL_REQUESTED,
                failureReason = current.failureReason ?: "Order cancelled by user",
            )
        }

        scheduleCompensations(updated.orderId)
        return toResponse(orderRepository.findById(orderId) ?: updated)
    }

    fun handlePaymentCompleted(event: OrderPaymentCompletedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(
                paymentStatus = OrderStepStatus.SUCCEEDED,
                paymentLogId = event.paymentLogId,
                pointsUsed = event.pointsUsed,
                cardAmount = event.cardAmount,
                transactionId = event.transactionId,
                approvalNumber = event.approvalNumber,
            )
        }

        scheduleCompensations(updated.orderId)
    }

    fun handlePaymentFailed(event: OrderPaymentFailedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(
                paymentStatus = OrderStepStatus.FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }

        scheduleCompensations(updated.orderId)
    }

    fun handleStockCompleted(event: OrderStockCompletedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(stockStatus = OrderStepStatus.SUCCEEDED)
        }

        scheduleCompensations(updated.orderId)
    }

    fun handleStockFailed(event: OrderStockFailedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(
                stockStatus = OrderStepStatus.FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }

        scheduleCompensations(updated.orderId)
    }

    fun handlePaymentCompensated(event: OrderPaymentCompensatedEvent) {
        updateOrder(event.orderId) { current ->
            current.copy(paymentStatus = OrderStepStatus.COMPENSATED)
        }
    }

    fun handlePaymentCompensationFailed(event: OrderPaymentCompensationFailedEvent) {
        updateOrder(event.orderId) { current ->
            current.copy(
                paymentStatus = OrderStepStatus.COMPENSATION_FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }
    }

    fun handleStockCompensated(event: OrderStockCompensatedEvent) {
        updateOrder(event.orderId) { current ->
            current.copy(stockStatus = OrderStepStatus.COMPENSATED)
        }
    }

    fun handleStockCompensationFailed(event: OrderStockCompensationFailedEvent) {
        updateOrder(event.orderId) { current ->
            current.copy(
                stockStatus = OrderStepStatus.COMPENSATION_FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }
    }

    private fun awaitResult(orderId: String, userId: Long): OrderResponse {
        val deadline = Instant.now().plus(TIMEOUT)

        while (true) {
            val current = orderRepository.findById(orderId)
                ?: throw OrderNotFoundException()
            validateOwnership(current, userId)

            if (current.status == OrderStatus.COMPLETED ||
                current.status == OrderStatus.FAILED ||
                current.status == OrderStatus.CANCELLED ||
                current.status == OrderStatus.COMPENSATION_FAILED ||
                current.status == OrderStatus.TIMED_OUT
            ) {
                return toResponse(current)
            }

            if (Instant.now().isAfter(deadline)) {
                return timeout(orderId, userId)
            }

            Thread.sleep(POLL_INTERVAL_MILLIS)
        }
    }

    private fun timeout(orderId: String, userId: Long): OrderResponse {
        val updated = updateOrder(orderId) { current ->
            validateOwnership(current, userId)

            if (current.status.isFinalForClient()) {
                return@updateOrder current
            }

            current.copy(
                cancelRequested = true,
                status = OrderStatus.TIMED_OUT,
                failureReason = current.failureReason ?: "Order processing timed out",
            )
        }

        scheduleCompensations(updated.orderId)
        return toResponse(orderRepository.findById(orderId) ?: updated)
    }

    private fun scheduleCompensations(orderId: String) {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                transactionTemplate.executeWithoutResult {
                    var current = orderRepository.findById(orderId)
                        ?: throw OrderNotFoundException()

                    if (shouldRequestPaymentCompensation(current)) {
                        current = orderRepository.save(
                            current.copy(paymentCompensationRequested = true),
                        )
                        eventPublisher.publish(
                            topic = DomainTopics.ORDER_PAYMENT_COMPENSATION_REQUESTED,
                            key = current.orderId,
                            eventType = DomainEventTypes.ORDER_PAYMENT_COMPENSATION_REQUESTED,
                            payload = OrderPaymentCompensationRequestedEvent(
                                orderId = current.orderId,
                                userId = current.userId,
                                paymentLogId = current.paymentLogId,
                                pointsUsed = current.pointsUsed,
                                cardAmount = current.cardAmount,
                            ),
                        )
                    }

                    if (shouldRequestStockCompensation(current)) {
                        current = orderRepository.save(
                            current.copy(stockCompensationRequested = true),
                        )
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
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ex
                }
            }
        }
    }

    private fun updateOrder(
        orderId: String,
        update: (OrderAggregate) -> OrderAggregate,
    ): OrderAggregate {
        repeat(MAX_RETRY_COUNT) { attempt ->
            try {
                return transactionTemplate.execute {
                    val current = orderRepository.findById(orderId)
                        ?: throw OrderNotFoundException()
                    val updated = update(current)
                    orderRepository.save(reconcile(updated))
                } ?: throw IllegalStateException("Order transaction returned null")
            } catch (ex: OptimisticLockingFailureException) {
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ex
                }
            }
        }

        throw IllegalStateException("Unreachable order retry state")
    }

    private fun reconcile(order: OrderAggregate): OrderAggregate {
        if (order.paymentStatus == OrderStepStatus.COMPENSATION_FAILED ||
            order.stockStatus == OrderStepStatus.COMPENSATION_FAILED
        ) {
            return order.copy(status = OrderStatus.COMPENSATION_FAILED)
        }

        if (order.cancelRequested) {
            if (order.paymentStatus.isPending() || order.stockStatus.isPending()) {
                return order.copy(
                    status = if (order.status == OrderStatus.TIMED_OUT) {
                        OrderStatus.TIMED_OUT
                    } else {
                        OrderStatus.CANCEL_REQUESTED
                    },
                )
            }

            if (order.paymentStatus.requiresCompensation() || order.stockStatus.requiresCompensation()) {
                return order.copy(
                    status = if (order.status == OrderStatus.TIMED_OUT) {
                        OrderStatus.TIMED_OUT
                    } else {
                        OrderStatus.COMPENSATING
                    },
                )
            }

            if (order.paymentStatus.isCompensationResolved() && order.stockStatus.isCompensationResolved()) {
                return order.copy(status = OrderStatus.CANCELLED)
            }
        }

        if (order.paymentStatus == OrderStepStatus.SUCCEEDED &&
            order.stockStatus == OrderStepStatus.SUCCEEDED
        ) {
            return order.copy(status = OrderStatus.COMPLETED)
        }

        val hasFailure = order.paymentStatus == OrderStepStatus.FAILED ||
            order.stockStatus == OrderStepStatus.FAILED
        if (hasFailure) {
            if (order.paymentStatus == OrderStepStatus.PENDING ||
                order.stockStatus == OrderStepStatus.PENDING
            ) {
                return order.copy(status = OrderStatus.PROCESSING)
            }

            if (order.paymentStatus.requiresCompensation() || order.stockStatus.requiresCompensation()) {
                return order.copy(status = OrderStatus.COMPENSATING)
            }

            return order.copy(status = OrderStatus.FAILED)
        }

        return order.copy(status = OrderStatus.PROCESSING)
    }

    private fun shouldRequestPaymentCompensation(order: OrderAggregate): Boolean {
        return order.shouldCompensate() &&
            order.paymentStatus == OrderStepStatus.SUCCEEDED &&
            !order.paymentCompensationRequested
    }

    private fun shouldRequestStockCompensation(order: OrderAggregate): Boolean {
        return order.shouldCompensate() &&
            order.stockStatus == OrderStepStatus.SUCCEEDED &&
            !order.stockCompensationRequested
    }

    private fun OrderAggregate.shouldCompensate(): Boolean {
        return cancelRequested ||
            paymentStatus == OrderStepStatus.FAILED ||
            stockStatus == OrderStepStatus.FAILED ||
            status == OrderStatus.TIMED_OUT
    }

    private fun OrderStepStatus.isPending(): Boolean = this == OrderStepStatus.PENDING

    private fun OrderStepStatus.requiresCompensation(): Boolean = this == OrderStepStatus.SUCCEEDED

    private fun OrderStepStatus.isCompensationResolved(): Boolean {
        return this == OrderStepStatus.FAILED || this == OrderStepStatus.COMPENSATED
    }

    private fun OrderStatus.canCancel(): Boolean {
        return this == OrderStatus.PENDING ||
            this == OrderStatus.PROCESSING ||
            this == OrderStatus.CANCEL_REQUESTED ||
            this == OrderStatus.COMPENSATING ||
            this == OrderStatus.TIMED_OUT
    }

    private fun OrderStatus.isFinalForClient(): Boolean {
        return this == OrderStatus.COMPLETED ||
            this == OrderStatus.FAILED ||
            this == OrderStatus.CANCELLED ||
            this == OrderStatus.COMPENSATION_FAILED ||
            this == OrderStatus.TIMED_OUT
    }

    private fun validateOwnership(order: OrderAggregate, userId: Long) {
        if (order.userId != userId) {
            throw OrderAccessDeniedException()
        }
    }

    private fun toResponse(order: OrderAggregate): OrderResponse {
        return OrderResponse(
            orderId = order.orderId,
            status = order.status,
            failureReason = order.failureReason,
        )
    }

    private companion object {
        const val MAX_RETRY_COUNT = 3
        const val POLL_INTERVAL_MILLIS = 200L
        val TIMEOUT: Duration = Duration.ofSeconds(30)
    }
}
