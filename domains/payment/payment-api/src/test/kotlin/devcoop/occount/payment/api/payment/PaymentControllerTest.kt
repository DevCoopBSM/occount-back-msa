package devcoop.occount.payment.api.payment

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.payment.application.query.chargelog.ChargeLogResult
import devcoop.occount.payment.application.query.chargelog.GetChargeHistoryQueryService
import devcoop.occount.payment.application.query.paymentlog.GetPaymentHistoryQueryService
import devcoop.occount.payment.application.query.paymentlog.PaymentLogResult
import devcoop.occount.payment.application.shared.PaymentFacade
import devcoop.occount.payment.application.shared.PaymentRequest
import devcoop.occount.payment.application.shared.PaymentResponse
import devcoop.occount.payment.application.shared.PointTransactionResult
import devcoop.occount.payment.domain.type.PaymentType
import devcoop.occount.payment.domain.type.RefundState
import devcoop.occount.payment.domain.type.TransactionType
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.mock.web.MockHttpServletRequest
import java.time.LocalDateTime

class PaymentControllerTest {
    private val paymentFacade = mock(PaymentFacade::class.java)
    private val getPaymentHistoryQueryService = mock(GetPaymentHistoryQueryService::class.java)
    private val getChargeHistoryQueryService = mock(GetChargeHistoryQueryService::class.java)
    private val controller = PaymentController(
        paymentFacade = paymentFacade,
        getPaymentHistoryQueryService = getPaymentHistoryQueryService,
        getChargeHistoryQueryService = getChargeHistoryQueryService,
    )

    @Test
    fun `execute payment delegates to facade with authenticated user id header`() {
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

        `when`(paymentFacade.execute(7L, request)).thenReturn(expected)

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "7")
        }

        val actual = controller.executePayment(request, httpRequest)

        assertSame(expected, actual)
        verify(paymentFacade).execute(7L, request)
    }

    @Test
    fun `get payment history delegates with authenticated user id header`() {
        val history = listOf(
            PaymentLogResult(
                paymentId = 1L,
                userId = 9L,
                paymentDate = LocalDateTime.of(2026, 3, 1, 12, 0),
                paymentType = PaymentType.POINT,
                totalAmount = 3000,
                pointTransaction = null,
                cardInfo = null,
                transactionInfo = null,
                managedEmail = null,
                eventType = null,
            ),
        )

        `when`(getPaymentHistoryQueryService.getPaymentHistory(9L)).thenReturn(history)

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "9")
        }

        val actual = controller.getPaymentHistory(httpRequest)

        assertSame(history, actual)
        verify(getPaymentHistoryQueryService).getPaymentHistory(9L)
    }

    @Test
    fun `get charge history by date range delegates to query service`() {
        val startDate = LocalDateTime.of(2026, 3, 1, 0, 0)
        val endDate = LocalDateTime.of(2026, 3, 31, 23, 59)
        val charges = listOf(
            ChargeLogResult(
                chargeId = 1L,
                userId = 9L,
                chargeDate = LocalDateTime.of(2026, 3, 15, 12, 0),
                chargeAmount = 5000,
                pointTransaction = PointTransactionResult(0, 5000, 5000),
                cardInfo = null,
                transactionInfo = null,
                managedEmail = null,
                reason = null,
                refundState = RefundState.NONE,
                refundDate = null,
                refundRequesterId = null,
            ),
        )

        `when`(getChargeHistoryQueryService.getChargeHistoryByDateRange(9L, startDate, endDate)).thenReturn(charges)

        val httpRequest = MockHttpServletRequest().apply {
            addHeader(AuthHeaders.AUTHENTICATED_USER_ID, "9")
        }

        val actual = controller.getChargeHistoryByDateRange(httpRequest, startDate, endDate)

        assertSame(charges, actual)
        verify(getChargeHistoryQueryService).getChargeHistoryByDateRange(9L, startDate, endDate)
    }
}
