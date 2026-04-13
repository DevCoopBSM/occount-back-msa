package devcoop.occount.order.application.usecase.order.create

import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderPendingResultRegistry
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.isFinalForClient
import org.springframework.stereotype.Service

@Service
class ExpireOrderUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderLifecycleProcessor: OrderLifecycleProcessor,
    private val orderPendingResultRegistry: OrderPendingResultRegistry,
    private val orderResponseMapper: OrderResponseMapper,
) {
    fun expire(orderId: String): OrderResponse {
        orderPendingResultRegistry.removePendingOrder(orderId)

        val updated = orderMutationExecutor.updateOrder(orderId) { current ->
            if (current.status.isFinalForClient()) {
                return@updateOrder current
            }

            current.copy(
                cancelRequested = true,
                status = OrderStatus.TIMED_OUT,
                failureReason = current.failureReason ?: "주문 처리 시간이 초과되었습니다",
            )
        }

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
        return orderResponseMapper.toResponse(updated)
    }
}
