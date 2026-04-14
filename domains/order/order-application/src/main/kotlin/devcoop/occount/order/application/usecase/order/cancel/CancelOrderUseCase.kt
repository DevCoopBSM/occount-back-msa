package devcoop.occount.order.application.usecase.order.cancel

import devcoop.occount.order.application.exception.OrderAccessDeniedException
import devcoop.occount.order.application.exception.OrderCannotCancelException
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.shared.OrderResponse
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderResponseMapper
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.canCancel
import org.springframework.stereotype.Service

@Service
class CancelOrderUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderRepository: OrderRepository,
    private val orderLifecycleProcessor: OrderLifecycleProcessor,
    private val orderResponseMapper: OrderResponseMapper,
) {
    fun cancel(orderId: String, kioskId: String): OrderResponse {
        val current = orderRepository.findById(orderId) ?: throw OrderNotFoundException()
        if (current.kioskId != kioskId) throw OrderAccessDeniedException()
        if (!current.status.canCancel()) throw OrderCannotCancelException()

        val updated = orderMutationExecutor.updateOrder(orderId) { order ->
            order.copy(
                cancelRequested = true,
                status = OrderStatus.CANCEL_REQUESTED,
                failureReason = order.failureReason ?: "사용자에 의해 주문이 취소되었습니다",
            )
        }

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
        return orderResponseMapper.toResponse(updated)
    }
}
