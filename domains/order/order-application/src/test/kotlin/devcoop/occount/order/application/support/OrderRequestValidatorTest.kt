package devcoop.occount.order.application.support

import devcoop.occount.core.common.event.OrderPaymentType
import devcoop.occount.order.application.exception.OrderInvalidTotalPriceException
import devcoop.occount.order.application.exception.OrderItemNotFoundException
import devcoop.occount.order.application.output.OrderItemData
import devcoop.occount.order.application.output.OrderItemReader
import devcoop.occount.order.application.shared.OrderItemRequest
import devcoop.occount.order.application.shared.OrderRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class OrderRequestValidatorTest {
    private val orderItemReader = FakeOrderItemReader(
        listOf(
            OrderItemData(
                itemId = 101L,
                itemName = "Americano",
                itemPrice = 2000,
                isActive = true,
            ),
            OrderItemData(
                itemId = 202L,
                itemName = "Latte",
                itemPrice = 3000,
                isActive = false,
            ),
        ),
    )
    private val validator = OrderRequestValidator(orderItemReader)

    @Test
    fun `canonical catalog data is required for order placement`() {
        val request = OrderRequest(
            items = listOf(
                OrderItemRequest(
                    itemId = 101L,
                    itemName = "Americano",
                    itemPrice = 1000,
                    quantity = 1,
                ),
            ),
            paymentType = OrderPaymentType.PAYMENT,
            totalAmount = 1000,
        )

        assertThrows(OrderInvalidTotalPriceException::class.java) {
            validator.validate(request)
        }
    }

    @Test
    fun `inactive items are rejected`() {
        val request = OrderRequest(
            items = listOf(
                OrderItemRequest(
                    itemId = 202L,
                    itemName = "Latte",
                    itemPrice = 3000,
                    quantity = 1,
                ),
            ),
            paymentType = OrderPaymentType.PAYMENT,
            totalAmount = 3000,
        )

        assertThrows(OrderItemNotFoundException::class.java) {
            validator.validate(request)
        }
    }

    @Test
    fun `validated request returns canonical order lines and total amount`() {
        val request = OrderRequest(
            items = listOf(
                OrderItemRequest(
                    itemId = 101L,
                    itemName = "Americano",
                    itemPrice = 2000,
                    quantity = 2,
                ),
            ),
            paymentType = OrderPaymentType.PAYMENT,
            totalAmount = 4000,
        )

        val validated = validator.validate(request)

        assertEquals(4000, validated.totalAmount)
        assertEquals("Americano", validated.lines.single().itemNameSnapshot)
        assertEquals(2000, validated.lines.single().unitPrice)
    }

    private class FakeOrderItemReader(
        private val items: List<OrderItemData>,
    ) : OrderItemReader {
        override fun findByIds(itemIds: Set<Long>): List<OrderItemData> {
            return items.filter { it.itemId in itemIds }
        }
    }
}
