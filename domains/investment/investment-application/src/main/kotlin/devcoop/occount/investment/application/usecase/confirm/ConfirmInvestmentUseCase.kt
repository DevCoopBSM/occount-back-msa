package devcoop.occount.investment.application.usecase.confirm

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.investment.application.event.InvestmentRegisteredEvent
import devcoop.occount.investment.application.exception.InvestmentAmountMismatchException
import devcoop.occount.investment.application.exception.PendingInvestmentNotFoundException
import devcoop.occount.investment.application.output.InvestmentRepository
import devcoop.occount.investment.domain.investment.InvestmentStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * 결제 확정된 출자를 CONFIRMED 로 전환하고 등록 이벤트를 발행한다.
 *
 * 트랜잭션 경계 안에서 (출자 조회 + 확정 저장 + outbox 이벤트 적재)만 수행한다.
 * PortOne 결제 조회 같은 외부 I/O 는 호출 측([HandlePortOneWebhookUseCase])에서 트랜잭션 밖에 둔다.
 * paymentId 단위로 멱등하다 — 이미 CONFIRMED 면 아무 일도 하지 않는다.
 */
@Service
class ConfirmInvestmentUseCase(
    private val investmentRepository: InvestmentRepository,
    private val eventPublisher: EventPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun confirm(paymentId: String, paidAmount: Int) {
        val investment = investmentRepository.findByPaymentId(paymentId)
            ?: throw PendingInvestmentNotFoundException()

        if (investment.status == InvestmentStatus.CONFIRMED) {
            log.info("이미 확정된 출자입니다. paymentId={}", paymentId)
            return
        }

        if (investment.amount != paidAmount) {
            throw InvestmentAmountMismatchException()
        }

        // 동시 재전송 레이스에서도 단 한 트랜잭션만 PENDING→CONFIRMED 전이에 성공하고,
        // 그 트랜잭션만 등록 이벤트를 발행하도록 조건부 UPDATE 결과로 게이트한다.
        val transitioned = investmentRepository.confirmIfPending(paymentId, LocalDateTime.now())
        if (!transitioned) {
            log.info("이미 다른 처리에서 확정된 출자입니다(레이스). paymentId={}", paymentId)
            return
        }

        eventPublisher.publish(
            topic = DomainTopics.INVESTMENT_REGISTERED,
            key = investment.userId.toString(),
            eventType = DomainEventTypes.INVESTMENT_REGISTERED,
            payload = InvestmentRegisteredEvent(
                investmentId = investment.investmentId,
                userId = investment.userId,
                amount = investment.amount,
            ),
        )
        log.info("출자 확정 완료. investmentId={} userId={}", investment.investmentId, investment.userId)
    }
}
