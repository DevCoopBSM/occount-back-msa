package devcoop.occount.payment.application.shared

import devcoop.occount.payment.domain.payment.PaymentType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentResponseTest {
    @Test
    fun `for charge returns card response with paymentLogId`() {
        val response = PaymentResponse.forCharge(
            paymentLogId = 42L,
            chargedAmount = 4000,
            approvalNumber = "APP-1",
            transactionId = "TX-1",
        )

        assertEquals("success", response.status)
        assertEquals(PaymentType.CARD, response.type)
        assertEquals(42L, response.paymentLogId)
        assertEquals(4000, response.totalAmount)
        assertEquals(4000, response.chargedAmount)
        assertNull(response.remainingPoints)
        assertNull(response.pointsUsed)
    }

    @Test
    fun `for payment returns provided payment type and amounts`() {
        val response = PaymentResponse.forPayment(
            type = PaymentType.MIXED,
            totalAmount = 8000,
            paymentAmount = 8000,
            pointsUsed = 3000,
            cardAmount = 5000,
            remainingPoints = 0,
            approvalNumber = "APP-2",
            transactionId = "TX-2",
            paymentLogId = 99L,
        )

        assertEquals("success", response.status)
        assertEquals(PaymentType.MIXED, response.type)
        assertEquals(8000, response.totalAmount)
        assertEquals(8000, response.paymentAmount)
        assertEquals(3000, response.pointsUsed)
        assertEquals(5000, response.cardAmount)
        assertEquals(0, response.remainingPoints)
        assertEquals("APP-2", response.approvalNumber)
        assertEquals("TX-2", response.transactionId)
        assertEquals(99L, response.paymentLogId)
    }
}
