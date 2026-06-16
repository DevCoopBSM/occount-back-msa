package devcoop.occount.investment.application.usecase.prepare

import devcoop.occount.investment.application.output.InvestmentRepository
import devcoop.occount.investment.domain.investment.Investment
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 인증 사용자의 출자 결제를 시작한다.
 * 서버가 paymentId("I-"+UUID)를 발급하고 PENDING 출자를 저장한 뒤 paymentId 를 돌려준다.
 * 결제 확정은 PortOne 웹훅으로 비동기 처리된다.
 */
@Service
class PrepareInvestmentUseCase(
    private val investmentRepository: InvestmentRepository,
) {
    @Transactional
    fun prepare(command: PrepareInvestmentCommand): PrepareInvestmentResult {
        val paymentId = INVESTMENT_PAYMENT_PREFIX + UUID.randomUUID().toString()
        val pending = Investment.pending(
            userId = command.userId,
            paymentId = paymentId,
            amount = command.amount,
        )
        investmentRepository.save(pending)
        return PrepareInvestmentResult(paymentId = paymentId)
    }

    companion object {
        /** 출자 결제 식별 접두사. 충전 등 다른 PortOne 결제와 구분한다(레거시 "I" 규칙 계승). */
        const val INVESTMENT_PAYMENT_PREFIX = "I-"
    }
}
