package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.core.common.event.OrderPaymentCompletedEvent
import devcoop.occount.core.common.event.OrderPaymentFailedEvent
import devcoop.occount.core.common.event.OrderPaymentRequestedEvent
import devcoop.occount.payment.application.shared.PaymentDetails
import devcoop.occount.payment.application.shared.PaymentFacade
import devcoop.occount.payment.application.shared.PaymentItem
import devcoop.occount.payment.application.shared.PaymentRequest
import devcoop.occount.payment.domain.payment.TransactionType
import org.springframework.stereotype.Service

@Service
class ProcessOrderPaymentUseCase(
    private val paymentFacade: PaymentFacade,
    private val eventPublisher: EventPublisher,
) {
    fun process(event: OrderPaymentRequestedEvent) {
        try {
            val result = paymentFacade.execute(
                event.userId,
                PaymentRequest(
                    type = TransactionType.valueOf(event.payment.type.name),
                    payment = PaymentDetails(
                        items = event.items.map { item ->
                            PaymentItem(
                                itemId = item.itemId.toString(),
                                itemName = item.itemName,
                                itemPrice = item.itemPrice,
                                quantity = item.quantity,
                                totalPrice = item.totalPrice,
                            )
                        },
                        totalAmount = event.payment.totalAmount,
                    ),
                ),
            )

            eventPublisher.publish(
                topic = DomainTopics.ORDER_PAYMENT_COMPLETED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_PAYMENT_COMPLETED,
                payload = OrderPaymentCompletedEvent(
                    orderId = event.orderId,
                    userId = event.userId,
                    paymentLogId = result.paymentLogId ?: 0L,
                    pointsUsed = result.pointsUsed ?: 0,
                    cardAmount = result.cardAmount ?: 0,
                    totalAmount = result.totalAmount ?: event.payment.totalAmount,
                    transactionId = result.transactionId,
                    approvalNumber = result.approvalNumber,
                ),
            )
        } catch (ex: Exception) {
            eventPublisher.publish(
                topic = DomainTopics.ORDER_PAYMENT_FAILED,
                key = event.orderId,
                eventType = DomainEventTypes.ORDER_PAYMENT_FAILED,
                payload = OrderPaymentFailedEvent(
                    orderId = event.orderId,
                    userId = event.userId,
                    reason = ex.message ?: "Payment processing failed",
                ),
            )
        }
    }
}
