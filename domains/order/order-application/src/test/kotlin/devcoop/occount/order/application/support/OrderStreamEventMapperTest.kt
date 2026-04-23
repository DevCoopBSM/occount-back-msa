package devcoop.occount.order.application.support

import devcoop.occount.order.application.shared.OrderStreamEventType
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class OrderStreamEventMapperTest {
    private val mapper = OrderStreamEventMapper(OrderResponseMapper())

    @Test
    fun `pending processing order maps to ORDER_ACCEPTED`() {
        val event = mapper.toStreamEvent(baseOrder())

        assertEquals(OrderStreamEventType.ORDER_ACCEPTED, event.type)
    }

    @Test
    fun `stock succeeded order maps to STOCK_CONFIRMED`() {
        val event = mapper.toStreamEvent(
            baseOrder(stockStatus = OrderStepStatus.SUCCEEDED),
        )

        assertEquals(OrderStreamEventType.STOCK_CONFIRMED, event.type)
    }

    @Test
    fun `payment requested order maps to PAYMENT_REQUESTED`() {
        val event = mapper.toStreamEvent(
            baseOrder(
                stockStatus = OrderStepStatus.SUCCEEDED,
                paymentRequested = true,
            ),
        )

        assertEquals(OrderStreamEventType.PAYMENT_REQUESTED, event.type)
    }

    @Test
    fun `final status maps directly to matching event type`() {
        val event = mapper.toStreamEvent(
            baseOrder(status = OrderStatus.COMPENSATION_FAILED),
        )

        assertEquals(OrderStreamEventType.COMPENSATION_FAILED, event.type)
    }

    private fun baseOrder(
        status: OrderStatus = OrderStatus.PROCESSING,
        stockStatus: OrderStepStatus = OrderStepStatus.PENDING,
        paymentStatus: OrderStepStatus = OrderStepStatus.PENDING,
        paymentRequested: Boolean = false,
    ): OrderAggregate {
        return OrderAggregate(
            orderId = "order-1",
            userId = 1L,
            payment = OrderPayment(totalAmount = 0),
            status = status,
            stockStatus = stockStatus,
            paymentStatus = paymentStatus,
            kioskId = "kiosk-1",
            expiresAt = Instant.now().plusSeconds(30),
            paymentRequested = paymentRequested,
        )
    }
}
