package devcoop.occount.order.application.usecase.order.event

import devcoop.occount.core.common.event.OrderStockCompensatedEvent
import devcoop.occount.core.common.event.OrderStockCompensationFailedEvent
import devcoop.occount.core.common.event.OrderStockCompletedEvent
import devcoop.occount.core.common.event.OrderStockFailedEvent
import devcoop.occount.order.application.support.OrderLifecycleProcessor
import devcoop.occount.order.application.support.OrderMutationExecutor
import devcoop.occount.order.application.support.OrderPaymentRequestScheduler
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderStepStatus
import org.springframework.stereotype.Service

@Service
class HandleOrderStockEventUseCase(
    private val orderMutationExecutor: OrderMutationExecutor,
    private val orderLifecycleProcessor: OrderLifecycleProcessor,
    private val orderPaymentRequestScheduler: OrderPaymentRequestScheduler,
) {
    fun applyCompletedStock(event: OrderStockCompletedEvent, recordConsumption: () -> Unit) {
        val updated = orderMutationExecutor.updateOrderIdempotently(
            orderId = event.orderId,
            recordConsumption = recordConsumption,
        ) { current ->
            if (current.stockStatus != OrderStepStatus.PENDING) return@updateOrderIdempotently current
            current.copy(
                stockStatus = OrderStepStatus.SUCCEEDED,
                lines = event.items.map { item ->
                    OrderLine(
                        itemId = item.itemId,
                        itemNameSnapshot = item.itemName,
                        unitPrice = item.itemPrice,
                        quantity = item.quantity,
                        totalPrice = item.totalPrice,
                    )
                },
                payment = OrderPayment(totalAmount = event.totalAmount),
            )
        } ?: return

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
        orderPaymentRequestScheduler.schedulePaymentRequestIfEligible(updated.orderId)
    }

    fun applyFailedStock(event: OrderStockFailedEvent, recordConsumption: () -> Unit) {
        val updated = orderMutationExecutor.updateOrderIdempotently(
            orderId = event.orderId,
            recordConsumption = recordConsumption,
        ) { current ->
            if (current.stockStatus != OrderStepStatus.PENDING) return@updateOrderIdempotently current
            current.copy(
                stockStatus = OrderStepStatus.FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        } ?: return

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }

    fun applyCompensatedStock(event: OrderStockCompensatedEvent, recordConsumption: () -> Unit) {
        val updated = orderMutationExecutor.updateOrderIdempotently(
            orderId = event.orderId,
            recordConsumption = recordConsumption,
        ) { current ->
            if (current.stockStatus != OrderStepStatus.SUCCEEDED) return@updateOrderIdempotently current
            current.copy(stockStatus = OrderStepStatus.COMPENSATED)
        } ?: return

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }

    fun applyStockCompensationFailure(event: OrderStockCompensationFailedEvent, recordConsumption: () -> Unit) {
        val updated = orderMutationExecutor.updateOrderIdempotently(
            orderId = event.orderId,
            recordConsumption = recordConsumption,
        ) { current ->
            if (current.stockStatus != OrderStepStatus.SUCCEEDED) return@updateOrderIdempotently current
            current.copy(
                stockStatus = OrderStepStatus.COMPENSATION_FAILED,
                failureReason = current.failureReason ?: event.reason,
            )
        } ?: return

        orderLifecycleProcessor.processAfterOrderStateChange(updated)
    }
}
