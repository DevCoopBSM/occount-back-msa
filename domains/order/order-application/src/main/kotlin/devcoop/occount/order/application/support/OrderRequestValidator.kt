package devcoop.occount.order.application.support

import devcoop.occount.order.application.exception.OrderInvalidTotalPriceException
import devcoop.occount.order.application.exception.OrderItemNotFoundException
import devcoop.occount.order.application.output.OrderItemReader
import devcoop.occount.order.application.shared.OrderRequest
import devcoop.occount.order.domain.order.OrderLine
import org.springframework.stereotype.Component

@Component
class OrderRequestValidator(
    private val orderItemReader: OrderItemReader,
) {
    fun validate(request: OrderRequest): ValidatedOrderRequest {
        val itemIds = request.items.map { it.itemId }.toSet()
        val catalogItems = orderItemReader.findByIds(itemIds).associateBy { it.itemId }

        val lines = request.items.map { item ->
            val catalogItem = catalogItems[item.itemId] ?: throw OrderItemNotFoundException()
            if (!catalogItem.isActive) throw OrderItemNotFoundException()
            OrderLine(
                itemId = catalogItem.itemId,
                itemNameSnapshot = catalogItem.itemName,
                unitPrice = catalogItem.itemPrice,
                quantity = item.quantity,
                totalPrice = catalogItem.itemPrice * item.quantity,
            )
        }

        val calculatedTotal = lines.sumOf { it.totalPrice }
        if (calculatedTotal != request.totalAmount) throw OrderInvalidTotalPriceException()

        return ValidatedOrderRequest(lines = lines, totalAmount = calculatedTotal)
    }
}

data class ValidatedOrderRequest(
    val lines: List<OrderLine>,
    val totalAmount: Int,
)
