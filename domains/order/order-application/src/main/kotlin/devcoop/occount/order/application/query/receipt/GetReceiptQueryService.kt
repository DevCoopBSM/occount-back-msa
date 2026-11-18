package devcoop.occount.order.application.query.receipt

import devcoop.occount.order.application.exception.OrderAccessDeniedException
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.exception.OrderReceiptNotAvailableException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.support.ReceiptResponseMapper
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderStatus
import org.springframework.stereotype.Service

@Service
class GetReceiptQueryService(
    private val orderRepository: OrderRepository,
    private val receiptResponseMapper: ReceiptResponseMapper,
) {
    fun getReceipt(orderId: Long, userId: Long?, kioskId: String?): ReceiptResponse {
        val order = orderRepository.findById(orderId) ?: throw OrderNotFoundException()
        validateAccess(order, userId, kioskId)
        validateReceiptAvailable(order)
        return receiptResponseMapper.toResponse(order, resolvePaymentType(order))
    }

    private fun validateAccess(order: OrderAggregate, userId: Long?, kioskId: String?) {
        if (order.userId != null) {
            if (order.userId != userId) throw OrderAccessDeniedException()
            return
        }

        if (order.kioskId != kioskId) throw OrderAccessDeniedException()
    }

    private fun validateReceiptAvailable(order: OrderAggregate) {
        if (order.status != OrderStatus.COMPLETED || order.lines.isEmpty()) {
            throw OrderReceiptNotAvailableException()
        }
    }

    private fun resolvePaymentType(order: OrderAggregate): ReceiptPaymentType {
        val pointsUsed = order.paymentResult.pointsUsed
        val cardAmount = order.paymentResult.cardAmount
        return when {
            pointsUsed > 0 && cardAmount > 0 -> ReceiptPaymentType.MIXED
            pointsUsed > 0 && cardAmount == 0 -> ReceiptPaymentType.POINT
            pointsUsed == 0 && cardAmount > 0 -> ReceiptPaymentType.CARD
            else -> throw OrderReceiptNotAvailableException()
        }
    }
}
