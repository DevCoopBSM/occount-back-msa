package devcoop.occount.order.application.order

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderItemPayload
import devcoop.occount.core.common.event.OrderPaymentCompensatedEvent
import devcoop.occount.core.common.event.OrderPaymentCompensationFailedEvent
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.core.common.event.OrderPaymentPayload
import devcoop.occount.core.common.event.OrderRequestedEvent
import devcoop.occount.core.common.event.OrderStockCompensatedEvent
import devcoop.occount.core.common.event.OrderStockCompensationFailedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.core.common.event.OrderStockFailedEvent
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderPaymentResult
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import devcoop.occount.order.domain.order.canCancel
import devcoop.occount.order.domain.order.isFinalForClient
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher,
    private val compensationService: OrderCompensationService,
    transactionManager: PlatformTransactionManager,
    @param:Value("\${order.timeout-seconds:30}") private val timeoutSeconds: Long,
) {
    private val transactionTemplate = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    private val pendingFutures = ConcurrentHashMap<String, CompletableFuture<OrderResponse>>()

    fun order(request: OrderRequest, userId: Long): CompletableFuture<OrderResponse> {
        request.orderInfos.forEach { it.validate() }

        val orderId = UUID.randomUUID().toString()

        val future = CompletableFuture<OrderResponse>()
            .orTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .exceptionally { ex ->
                if (ex is TimeoutException) {
                    log.warn("주문 처리 시간 초과 - 주문={}", orderId)
                    handleTimeout(orderId, userId)
                } else {
                    pendingFutures.remove(orderId)
                    throw ex
                }
            }

        pendingFutures[orderId] = future

        try {
            transactionTemplate.executeWithoutResult {
                val createdOrder = orderRepository.save(
                    OrderAggregate(
                        orderId = orderId,
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
                        expiresAt = Instant.now().plus(Duration.ofSeconds(timeoutSeconds)),
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

                log.info("주문 생성 완료 - 주문={} 사용자={}", createdOrder.orderId, userId)
            }
        } catch (ex: Exception) {
            pendingFutures.remove(orderId)
            future.completeExceptionally(ex)
        }

        return future
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
                failureReason = current.failureReason ?: "사용자에 의해 주문이 취소되었습니다",
            )
        }

        compensationService.scheduleCompensations(updated.orderId)
        completeIfFinal(updated)
        return toResponse(updated)
    }

    fun handlePaymentCompleted(event: OrderPaymentCompletedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(
                paymentStatus = OrderStepStatus.SUCCEEDED,
                paymentResult = OrderPaymentResult(
                    paymentLogId = event.paymentLogId,
                    pointsUsed = event.pointsUsed,
                    cardAmount = event.cardAmount,
                    transactionId = event.transactionId,
                    approvalNumber = event.approvalNumber,
                ),
            )
        }

        compensationService.scheduleCompensations(updated.orderId)
        completeIfFinal(updated)
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

        compensationService.scheduleCompensations(updated.orderId)
        completeIfFinal(updated)
    }

    fun handleStockCompleted(event: OrderStockCompletedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(stockStatus = OrderStepStatus.SUCCEEDED)
        }

        compensationService.scheduleCompensations(updated.orderId)
        completeIfFinal(updated)
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

        compensationService.scheduleCompensations(updated.orderId)
        completeIfFinal(updated)
    }

    fun handlePaymentCompensated(event: OrderPaymentCompensatedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.SUCCEEDED) {
                return@updateOrder current
            }
            current.copy(paymentStatus = OrderStepStatus.COMPENSATED)
        }
        completeIfFinal(updated)
    }

    fun handlePaymentCompensationFailed(event: OrderPaymentCompensationFailedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.paymentStatus != OrderStepStatus.SUCCEEDED) {
                return@updateOrder current
            }
            current.copy(
                paymentStatus = OrderStepStatus.COMPENSATION_FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }
        completeIfFinal(updated)
    }

    fun handleStockCompensated(event: OrderStockCompensatedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.SUCCEEDED) {
                return@updateOrder current
            }
            current.copy(stockStatus = OrderStepStatus.COMPENSATED)
        }
        completeIfFinal(updated)
    }

    fun handleStockCompensationFailed(event: OrderStockCompensationFailedEvent) {
        val updated = updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.SUCCEEDED) {
                return@updateOrder current
            }
            current.copy(
                stockStatus = OrderStepStatus.COMPENSATION_FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }
        completeIfFinal(updated)
    }

    private fun handleTimeout(orderId: String, userId: Long): OrderResponse {
        pendingFutures.remove(orderId)

        val updated = updateOrder(orderId) { current ->
            validateOwnership(current, userId)

            if (current.status.isFinalForClient()) {
                return@updateOrder current
            }

            current.copy(
                cancelRequested = true,
                status = OrderStatus.TIMED_OUT,
                failureReason = current.failureReason ?: "주문 처리 시간이 초과되었습니다",
            )
        }

        compensationService.scheduleCompensations(updated.orderId)
        return toResponse(updated)
    }

    private fun completeIfFinal(order: OrderAggregate) {
        if (!order.status.isFinalForClient()) return
        val future = pendingFutures.remove(order.orderId) ?: return
        future.complete(toResponse(order))
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
                    orderRepository.save(updated.reconcile())
                }
            } catch (ex: OptimisticLockingFailureException) {
                log.warn("낙관적 락 충돌 - 주문={} 시도={}", orderId, attempt)
                if (attempt == MAX_RETRY_COUNT - 1) {
                    throw ex
                }
                Thread.sleep(BASE_BACKOFF_MILLIS * (1L shl attempt))
            }
        }

        throw OrderTransactionFailedException()
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

    companion object {
        private val log = LoggerFactory.getLogger(OrderService::class.java)
        private const val MAX_RETRY_COUNT = 3
        private const val BASE_BACKOFF_MILLIS = 50L
    }
}
