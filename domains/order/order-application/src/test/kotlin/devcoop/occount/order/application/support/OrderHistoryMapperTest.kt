package devcoop.occount.order.application.support

import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderPaymentResult
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class OrderHistoryMapperTest {
    private val mapper = OrderHistoryMapper()

    @Test
    fun `maps order lines and payment result into history item`() {
        val createdAt = Instant.parse("2026-06-11T09:00:00Z")
        val order = OrderAggregate(
            orderId = 1001L,
            userId = 7L,
            lines = listOf(
                OrderLine(itemId = 1L, itemNameSnapshot = "삼각김밥", unitPrice = 1500, quantity = 3, totalPrice = 4500),
            ),
            payment = OrderPayment(totalAmount = 4500),
            status = OrderStatus.COMPLETED,
            paymentStatus = OrderStepStatus.SUCCEEDED,
            kioskId = "kiosk-1",
            expiresAt = createdAt.plusSeconds(30),
            paymentResult = OrderPaymentResult(
                paymentLogId = 5001L,
                pointsUsed = 500,
                cardAmount = 4000,
                transactionId = "TXN-1",
                approvalNumber = "00012345",
            ),
            createdAt = createdAt,
        )

        val item = mapper.toHistoryItem(order)

        assertEquals(1001L, item.orderId)
        assertEquals(OrderStatus.COMPLETED, item.status)
        assertEquals(createdAt, item.orderDate)
        assertEquals(4500, item.totalAmount)
        assertEquals(1, item.lines.size)
        assertEquals("삼각김밥", item.lines.single().itemNameSnapshot)
        assertEquals(3, item.lines.single().quantity)
        assertEquals(5001L, item.payment.paymentLogId)
        assertEquals(OrderStepStatus.SUCCEEDED, item.payment.paymentStatus)
        assertEquals(500, item.payment.pointsUsed)
        assertEquals(4000, item.payment.cardAmount)
        assertEquals("TXN-1", item.payment.transactionId)
        assertEquals("00012345", item.payment.approvalNumber)
    }
}
