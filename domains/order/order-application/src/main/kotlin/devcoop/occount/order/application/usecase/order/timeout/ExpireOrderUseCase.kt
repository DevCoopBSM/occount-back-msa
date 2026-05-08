package devcoop.occount.order.application.usecase.order.timeout

import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderPaymentCancellationEventPublisher
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.stereotype.Service

@Service
class ExpireOrderUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderLifecycleProcessor: OrderLifecycleProcessor,
    private val orderPaymentCancellationEventPublisher: OrderPaymentCancellationEventPublisher,
    private val orderResponseMapper: OrderResponseMapper,
) {
    fun expire(orderId: Long): OrderResponse {
        val updated = orderMutationExecutor.updateOrder(
            orderId = orderId,
            update = { current ->
                if (current.status.isFinalForClient()) {
                    return@updateOrder current
                }

                val expiredOrder = current.copy(
                    cancelRequested = true,
                    status = OrderStatus.TIMED_OUT,
                    failureReason = current.failureReason ?: "주문 처리 시간이 초과되었습니다",
                )
                if (expiredOrder.shouldRequestPendingPaymentCancellation()) {
                    expiredOrder.copy(paymentCancellationRequested = true)
                } else {
                    expiredOrder
                }
            },
            afterUpdate = { updatedOrder ->
                if (updatedOrder.paymentCancellationRequested) {
                    orderPaymentCancellationEventPublisher.publish(updatedOrder)
                }
            },
        )

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
        return orderResponseMapper.toResponse(updated)
    }
}
