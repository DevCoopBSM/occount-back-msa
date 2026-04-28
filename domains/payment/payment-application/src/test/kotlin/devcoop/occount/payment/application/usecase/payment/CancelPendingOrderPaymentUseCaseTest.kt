package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.OrderPaymentCancellationRequestedEvent
import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.VanResult
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.OrderPaymentCancellationRequestResult
import devcoop.occount.payment.application.output.OrderPaymentExecutionRepository
import devcoop.occount.payment.application.output.OrderPaymentExecutionStartResult
import kotlin.test.Test
import kotlin.test.assertEquals

class CancelPendingOrderPaymentUseCaseTest {
    @Test
    fun `cancel requests pending approval cancellation for the order`() {
        val cardPaymentPort = FakeCardPaymentPort()
        val executionRepository = FakeOrderPaymentExecutionRepository(
            cancellationRequestResult = OrderPaymentCancellationRequestResult.TERMINAL_CANCELLATION_REQUIRED,
        )
        val useCase = CancelPendingOrderPaymentUseCase(cardPaymentPort, executionRepository)

        useCase.cancel(OrderPaymentCancellationRequestedEvent(orderId = 1L, kioskId = "kiosk-1", userId = 1L))

        assertEquals(1L, cardPaymentPort.cancelRequestedPaymentKey)
    }

    @Test
    fun `cancel keeps state but skips terminal cancellation when no active payment exists`() {
        val cardPaymentPort = FakeCardPaymentPort()
        val executionRepository = FakeOrderPaymentExecutionRepository(
            cancellationRequestResult = OrderPaymentCancellationRequestResult.NO_ACTIVE_PAYMENT,
        )
        val useCase = CancelPendingOrderPaymentUseCase(cardPaymentPort, executionRepository)

        useCase.cancel(OrderPaymentCancellationRequestedEvent(orderId = 1L, kioskId = "kiosk-1", userId = 1L))

        assertEquals(null, cardPaymentPort.cancelRequestedPaymentKey)
        assertEquals(1L, executionRepository.lastCancellationOrderId)
    }

    private class FakeCardPaymentPort : CardPaymentPort {
        var cancelRequestedPaymentKey: Long? = null

        override fun approve(amount: Int, items: List<ItemCommand>, kioskId: String, paymentKey: Long?): VanResult {
            error("not used in this test")
        }

        override fun refund(transactionId: String?, approvalNumber: String?, approvalDate: String, terminalId: String?, amount: Int, kioskId: String): VanResult {
            error("not used in this test")
        }

        override fun requestPendingApprovalCancellation(paymentKey: Long, kioskId: String) {
            cancelRequestedPaymentKey = paymentKey
        }
    }

    private class FakeOrderPaymentExecutionRepository(
        private val cancellationRequestResult: OrderPaymentCancellationRequestResult,
    ) : OrderPaymentExecutionRepository {
        var lastCancellationOrderId: Long? = null

        override fun startProcessing(orderId: Long): OrderPaymentExecutionStartResult = OrderPaymentExecutionStartResult.STARTED

        override fun requestCancellation(orderId: Long): OrderPaymentCancellationRequestResult {
            lastCancellationOrderId = orderId
            return cancellationRequestResult
        }

        override fun isCancellationRequested(orderId: Long): Boolean = false
        override fun markCompleted(orderId: Long) = Unit
        override fun markFailed(orderId: Long) = Unit
        override fun markCancelled(orderId: Long) = Unit
    }
}
