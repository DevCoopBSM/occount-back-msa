package devcoop.occount.order.application.usecase.order.event

import devcoop.occount.core.common.event.OrderStockCompensatedEvent
import devcoop.occount.core.common.event.OrderStockCompensationFailedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.core.common.event.OrderStockFailedEvent
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderPaymentRequestScheduler
import devcoop.occount.order.domain.order.OrderStepStatus
import org.springframework.stereotype.Service

@Service
class HandleOrderStockEventUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderLifecycleProcessor: OrderLifecycleProcessor,
    private val orderPaymentRequestScheduler: OrderPaymentRequestScheduler,
) {
    fun applyCompletedStock(event: OrderStockCompletedEvent) {
        val updated = orderMutationExecutor.updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(stockStatus = OrderStepStatus.SUCCEEDED)
        }

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
        orderPaymentRequestScheduler.schedulePaymentRequestIfEligible(updated.orderId)
    }

    fun applyFailedStock(event: OrderStockFailedEvent) {
        val updated = orderMutationExecutor.updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.PENDING) {
                return@updateOrder current
            }

            current.copy(
                stockStatus = OrderStepStatus.FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }

    fun applyCompensatedStock(event: OrderStockCompensatedEvent) {
        val updated = orderMutationExecutor.updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.SUCCEEDED) {
                return@updateOrder current
            }

            current.copy(stockStatus = OrderStepStatus.COMPENSATED)
        }

        orderLifecycleProcessor.completePendingOrderIfFinal(updated)
    }

    fun applyStockCompensationFailure(event: OrderStockCompensationFailedEvent) {
        val updated = orderMutationExecutor.updateOrder(event.orderId) { current ->
            if (current.stockStatus != OrderStepStatus.SUCCEEDED) {
                return@updateOrder current
            }

            current.copy(
                stockStatus = OrderStepStatus.COMPENSATION_FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        }

        orderLifecycleProcessor.completePendingOrderIfFinal(updated)
    }
}
