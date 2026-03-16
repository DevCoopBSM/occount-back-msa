package devcoop.occount.payment.api.payment

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.payment.application.payment.PaymentRequest
import devcoop.occount.payment.application.payment.PaymentResponse
import devcoop.occount.payment.application.payment.PaymentService
import devcoop.occount.payment.domain.ChargeLog
import devcoop.occount.payment.domain.PaymentLog
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.type.TransactionType
import devcoop.occount.payment.domain.vo.PointTransaction
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import java.time.LocalDateTime

class PaymentControllerTest {
    @Test
    fun `execute payment delegates to payment service with authenticated user id header`() {
        val paymentService = mock(PaymentService::class.java)
        val controller = PaymentController(paymentService)
        val request = PaymentRequest(
            type = TransactionType.PAYMENT,
            charge = null,
            payment = null,
        )
        val expected = PaymentResponse.forPayment(
            type = PaymentType.POINT,
            totalAmount = 1000,
            paymentAmount = 1000,
            pointsUsed = 1000,
            cardAmount = null,
            remainingPoints = 0,
            approvalNumber = null,
            transactionId = null,
        )

        `when`(paymentService.execute(request, 7L)).thenReturn(expected)

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "7")
        }

        val actual = controller.executePayment(request, httpRequest)

        assertSame(expected, actual)
        verify(paymentService).execute(request, 7L)
    }

    @Test
    fun `get payment history delegates with authenticated user id header`() {
        val paymentService = mock(PaymentService::class.java)
        val controller = PaymentController(paymentService)
        val history = listOf(
            PaymentLog(
                userId = 9L,
                paymentType = PaymentType.POINT,
                totalAmount = 3000,
            ),
        )

        `when`(paymentService.getPaymentHistory(9L)).thenReturn(history)

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "9")
        }

        val actual = controller.getPaymentHistory(httpRequest)

        assertSame(history, actual)
        verify(paymentService).getPaymentHistory(9L)
    }

    @Test
    fun `get charge history by date range delegates to payment service`() {
        val paymentService = mock(PaymentService::class.java)
        val controller = PaymentController(paymentService)
        val startDate = LocalDateTime.of(2026, 3, 1, 0, 0)
        val endDate = LocalDateTime.of(2026, 3, 31, 23, 59)
        val charges = listOf(
            ChargeLog(
                userId = 9L,
                chargeAmount = 5000,
                pointTransaction = PointTransaction(0, 5000, 5000),
            ),
        )

        `when`(paymentService.getChargeHistoryByDateRange(9L, startDate, endDate)).thenReturn(charges)

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "9")
        }

        val actual = controller.getChargeHistoryByDateRange(httpRequest, startDate, endDate)

        assertSame(charges, actual)
        verify(paymentService).getChargeHistoryByDateRange(9L, startDate, endDate)
    }
}
