package devcoop.occount.payment.application.usecase.payment

import devcoop.occount.core.common.event.OrderPaymentCancellationRequestedEvent
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.application.output.OrderPaymentCancellationRequestResult
import devcoop.occount.payment.application.output.OrderPaymentExecutionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CancelPendingOrderPaymentUseCase(
    private val cardPaymentPort: CardPaymentPort,
    private val orderPaymentExecutionRepository: OrderPaymentExecutionRepository,
) {
    fun cancel(event: OrderPaymentCancellationRequestedEvent) {
        log.info("결제 대기 취소 요청 처리 - 주문={}", event.orderId)
        if (orderPaymentExecutionRepository.requestCancellation(event.orderId) ==
            OrderPaymentCancellationRequestResult.TERMINAL_CANCELLATION_REQUIRED) {
            cardPaymentPort.requestPendingApprovalCancellation("order-${event.orderId}", event.kioskId)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(CancelPendingOrderPaymentUseCase::class.java)
    }
}
