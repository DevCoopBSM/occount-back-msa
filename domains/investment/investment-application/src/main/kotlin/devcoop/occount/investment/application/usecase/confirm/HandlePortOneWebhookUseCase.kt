package devcoop.occount.investment.application.usecase.confirm

import devcoop.occount.investment.application.output.PortOnePaymentPort
import devcoop.occount.investment.application.output.PortOneWebhookGateway
import devcoop.occount.investment.application.usecase.prepare.PrepareInvestmentUseCase
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * PortOne 출자 결제 웹훅의 진입 유스케이스.
 *
 * 1) 서명 검증·파싱(게이트웨이) → 2) 결제 완료 이벤트/출자 결제(prefix)만 통과 →
 * 3) PortOne 결제 조회로 PAID·금액 확정(HTTP, 트랜잭션 밖) → 4) [ConfirmInvestmentUseCase] 로 확정(트랜잭션).
 */
@Service
class HandlePortOneWebhookUseCase(
    private val portOneWebhookGateway: PortOneWebhookGateway,
    private val portOnePaymentPort: PortOnePaymentPort,
    private val confirmInvestmentUseCase: ConfirmInvestmentUseCase,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun handle(rawBody: String, headers: Map<String, String>) {
        val notification = portOneWebhookGateway.parseAndVerify(rawBody, headers) ?: return

        // 결제 완료(Paid)만 처리한다. Ready(가상계좌 발급 등 미결제)는 이후 Paid 웹훅에서 처리된다.
        if (notification.eventType != PAID_EVENT_TYPE) {
            return
        }

        val paymentId = notification.paymentId
        if (!paymentId.startsWith(PrepareInvestmentUseCase.INVESTMENT_PAYMENT_PREFIX)) {
            // 출자 결제가 아닌 paymentId — 다른 도메인(충전 등) 결제이므로 무시한다.
            return
        }

        val payment = portOnePaymentPort.fetchPayment(paymentId)
        if (!payment.isPaid()) {
            log.info("아직 결제 완료 상태가 아닙니다. paymentId={} status={}", paymentId, payment.status)
            return
        }

        confirmInvestmentUseCase.confirm(paymentId = paymentId, paidAmount = payment.amount)
    }

    companion object {
        private const val PAID_EVENT_TYPE = "Transaction.Paid"
    }
}
