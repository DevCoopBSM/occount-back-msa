package devcoop.occount.order.application.query.receipt

import devcoop.occount.order.application.exception.OrderAccessDeniedException
import devcoop.occount.order.application.exception.OrderNotFoundException
import devcoop.occount.order.application.exception.OrderReceiptNotAvailableException
import devcoop.occount.order.application.output.OrderRepository
import devcoop.occount.order.application.output.PersistedOrder
import devcoop.occount.order.application.support.ReceiptResponseMapper
import devcoop.occount.order.domain.order.OrderAggregate
import devcoop.occount.order.domain.order.OrderLine
import devcoop.occount.order.domain.order.OrderPayment
import devcoop.occount.order.domain.order.OrderPaymentResult
import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import devcoop.occount.order.domain.order.RequestedOrderLine
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant

class GetReceiptQueryServiceTest {
    @Test
    fun `completed member order returns receipt`() {
        val service = getReceiptQueryService(FakeOrderRepository(orderFixture()))

        val receipt = service.getReceipt(ORDER_ID, USER_ID, null)

        assertEquals(ORDER_ID, receipt.orderId)
        assertEquals(OrderStatus.COMPLETED, receipt.orderStatus)
        assertEquals("Americano", receipt.items.single().itemName)
        assertEquals(2, receipt.items.single().quantity)
        assertEquals(4000, receipt.items.single().totalPrice)
        assertEquals(ReceiptPaymentType.CARD, receipt.payment.type)
        assertEquals(4000, receipt.payment.totalAmount)
        assertEquals(0, receipt.payment.pointsUsed)
        assertEquals(4000, receipt.payment.cardAmount)
        assertEquals("ap-1", receipt.payment.approvalNumber)
        assertEquals("tx-1", receipt.payment.transactionId)
    }

    @Test
    fun `guest order allows matching kiosk id`() {
        val service = getReceiptQueryService(FakeOrderRepository(orderFixture(userId = null)))

        val receipt = service.getReceipt(ORDER_ID, null, KIOSK_ID)

        assertEquals(ORDER_ID, receipt.orderId)
    }

    @Test
    fun `member order denies different user`() {
        val service = getReceiptQueryService(FakeOrderRepository(orderFixture()))

        assertThrows(OrderAccessDeniedException::class.java) {
            service.getReceipt(ORDER_ID, userId = 999L, kioskId = KIOSK_ID)
        }
    }

    @Test
    fun `guest order denies different kiosk`() {
        val service = getReceiptQueryService(FakeOrderRepository(orderFixture(userId = null)))

        assertThrows(OrderAccessDeniedException::class.java) {
            service.getReceipt(ORDER_ID, userId = null, kioskId = "other-kiosk")
        }
    }

    @Test
    fun `non completed order cannot return receipt`() {
        val service = getReceiptQueryService(FakeOrderRepository(orderFixture(status = OrderStatus.PROCESSING)))

        assertThrows(OrderReceiptNotAvailableException::class.java) {
            service.getReceipt(ORDER_ID, USER_ID, KIOSK_ID)
        }
    }

    @Test
    fun `missing order throws not found`() {
        val service = getReceiptQueryService(FakeOrderRepository())

        assertThrows(OrderNotFoundException::class.java) {
            service.getReceipt(ORDER_ID, USER_ID, KIOSK_ID)
        }
    }

    @Test
    fun `point only payment type is point`() {
        val service = getReceiptQueryService(
            FakeOrderRepository(orderFixture(pointsUsed = 4000, cardAmount = 0, approvalNumber = null, transactionId = null)),
        )

        val receipt = service.getReceipt(ORDER_ID, USER_ID, KIOSK_ID)

        assertEquals(ReceiptPaymentType.POINT, receipt.payment.type)
    }

    @Test
    fun `mixed payment type is mixed`() {
        val service = getReceiptQueryService(FakeOrderRepository(orderFixture(pointsUsed = 1000, cardAmount = 3000)))

        val receipt = service.getReceipt(ORDER_ID, USER_ID, KIOSK_ID)

        assertEquals(ReceiptPaymentType.MIXED, receipt.payment.type)
    }

    @Test
    fun `zero point and zero card payment is unavailable`() {
        val service = getReceiptQueryService(FakeOrderRepository(orderFixture(pointsUsed = 0, cardAmount = 0)))

        assertThrows(OrderReceiptNotAvailableException::class.java) {
            service.getReceipt(ORDER_ID, USER_ID, KIOSK_ID)
        }
    }

    private fun orderFixture(
        userId: Long? = USER_ID,
        status: OrderStatus = OrderStatus.COMPLETED,
        pointsUsed: Int = 0,
        cardAmount: Int = 4000,
        approvalNumber: String? = "ap-1",
        transactionId: String? = "tx-1",
    ): OrderAggregate {
        return OrderAggregate(
            orderId = ORDER_ID,
            userId = userId,
            requestedLines = listOf(RequestedOrderLine(itemId = ITEM_ID, quantity = 2)),
            lines = listOf(
                OrderLine(
                    itemId = ITEM_ID,
                    itemNameSnapshot = "Americano",
                    unitPrice = 2000,
                    quantity = 2,
                    totalPrice = 4000,
                ),
            ),
            payment = OrderPayment(totalAmount = 4000),
            status = status,
            paymentStatus = if (status == OrderStatus.COMPLETED) OrderStepStatus.SUCCEEDED else OrderStepStatus.PENDING,
            stockStatus = if (status == OrderStatus.COMPLETED) OrderStepStatus.SUCCEEDED else OrderStepStatus.PENDING,
            kioskId = KIOSK_ID,
            expiresAt = Instant.now().plusSeconds(30),
            paymentResult = OrderPaymentResult(
                paymentLogId = 10L,
                pointsUsed = pointsUsed,
                cardAmount = cardAmount,
                approvalNumber = approvalNumber,
                transactionId = transactionId,
            ),
        )
    }

    private class FakeOrderRepository(
        private val order: OrderAggregate? = null,
    ) : OrderRepository {
        override fun findById(orderId: Long): OrderAggregate? = order?.takeIf { it.orderId == orderId }
        override fun findPersistedById(orderId: Long): PersistedOrder? = null
        override fun save(order: OrderAggregate): OrderAggregate = order
        override fun save(order: OrderAggregate, persistenceVersion: Long): OrderAggregate = order
        override fun findExpiredNonFinalOrderIds(now: Instant): List<Long> = emptyList()
        override fun findOrderIdsRequiringCompensation(limit: Int): List<Long> = emptyList()
    }

    private fun getReceiptQueryService(orderRepository: OrderRepository): GetReceiptQueryService {
        return GetReceiptQueryService(
            orderRepository = orderRepository,
            receiptResponseMapper = ReceiptResponseMapper(),
        )
    }

    private companion object {
        const val ORDER_ID = 1L
        const val USER_ID = 7L
        const val ITEM_ID = 101L
        const val KIOSK_ID = "kiosk-1"
    }
}
