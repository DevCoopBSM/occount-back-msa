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

        useCase.cancel(OrderPaymentCancellationRequestedEvent(orderId = "order-1", kioskId = "kiosk-1", userId = 1L))

        assertEquals("order-1", cardPaymentPort.cancelRequestedPaymentKey)
    }

    @Test
    fun `cancel keeps state but skips terminal cancellation when no active payment exists`() {
        val cardPaymentPort = FakeCardPaymentPort()
        val executionRepository = FakeOrderPaymentExecutionRepository(
            cancellationRequestResult = OrderPaymentCancellationRequestResult.NO_ACTIVE_PAYMENT,
        )
        val useCase = CancelPendingOrderPaymentUseCase(cardPaymentPort, executionRepository)

        useCase.cancel(OrderPaymentCancellationRequestedEvent(orderId = "order-1", kioskId = "kiosk-1", userId = 1L))

        assertEquals(null, cardPaymentPort.cancelRequestedPaymentKey)
        assertEquals("order-1", executionRepository.lastCancellationOrderId)
    }

    private class FakeCardPaymentPort : CardPaymentPort {
        var cancelRequestedPaymentKey: String? = null

        override fun approve(amount: Int, items: List<ItemCommand>, kioskId: String, paymentKey: String?): VanResult {
            error("not used in this test")
        }

        override fun refund(transactionId: String?, approvalNumber: String?, approvalDate: String, amount: Int, kioskId: String): VanResult {
            error("not used in this test")
        }

        override fun requestPendingApprovalCancellation(paymentKey: String, kioskId: String) {
            cancelRequestedPaymentKey = paymentKey
        }
    }

    private class FakeOrderPaymentExecutionRepository(
        private val cancellationRequestResult: OrderPaymentCancellationRequestResult,
    ) : OrderPaymentExecutionRepository {
        var lastCancellationOrderId: String? = null

        override fun startProcessing(orderId: String): OrderPaymentExecutionStartResult = OrderPaymentExecutionStartResult.STARTED

        override fun requestCancellation(orderId: String): OrderPaymentCancellationRequestResult {
            lastCancellationOrderId = orderId
            return cancellationRequestResult
        }

        override fun isCancellationRequested(orderId: String): Boolean = false
        override fun markCompleted(orderId: String) = Unit
        override fun markFailed(orderId: String) = Unit
        override fun markCancelled(orderId: String) = Unit
    }
}
