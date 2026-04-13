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
        if (request.items.isEmpty()) {
            throw OrderInvalidTotalPriceException()
        }

        val requestedItemIds = request.items.map { it.itemId }
        if (requestedItemIds.distinct().size != requestedItemIds.size) {
            throw OrderInvalidTotalPriceException()
        }

        request.items.forEach { it.validate() }

        val itemDataById = orderItemReader.findByIds(requestedItemIds.toSet())
            .filter { it.isActive }
            .associateBy { it.itemId }

        if (itemDataById.size != requestedItemIds.size) {
            throw OrderItemNotFoundException()
        }

        val lines = request.items.map { requestedItem ->
            val itemData = itemDataById[requestedItem.itemId]
                ?: throw OrderItemNotFoundException()

            if (requestedItem.itemName != itemData.itemName ||
                requestedItem.itemPrice != itemData.itemPrice
            ) {
                throw OrderInvalidTotalPriceException()
            }

            val calculatedLineTotalPrice = itemData.itemPrice * requestedItem.quantity

            OrderLine(
                itemId = itemData.itemId,
                itemNameSnapshot = itemData.itemName,
                unitPrice = itemData.itemPrice,
                quantity = requestedItem.quantity,
                totalPrice = calculatedLineTotalPrice,
            )
        }

        val totalAmount = lines.sumOf { it.totalPrice }
        if (request.totalAmount != totalAmount) {
            throw OrderInvalidTotalPriceException()
        }

        return ValidatedOrderRequest(
            lines = lines,
            totalAmount = totalAmount,
        )
    }
}

data class ValidatedOrderRequest(
    val lines: List<OrderLine>,
    val totalAmount: Int,
)
