package devcoop.occount.investment.application.usecase.admin

import devcoop.occount.core.common.event.DomainEventTypes
import devcoop.occount.core.common.event.DomainTopics
import devcoop.occount.core.common.event.EventPublisher
import devcoop.occount.investment.application.event.InvestmentRegisteredEvent
import devcoop.occount.investment.application.output.InvestmentRepository
import devcoop.occount.investment.domain.investment.Investment
import devcoop.occount.investment.domain.investment.InvestmentType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * 관리자가 출자를 수기로 등록한다(즉시 확정).
 * PortOne 결제 없이 생성하므로 paymentId 는 "ADMIN-"+UUID 로 발급한다.
 * 확정과 동시에 등록 이벤트를 발행해 회원 등급 승격이 동일하게 동작한다.
 */
@Service
class CreateInvestmentByAdminUseCase(
    private val investmentRepository: InvestmentRepository,
    private val eventPublisher: EventPublisher,
) {
    @Transactional
    fun create(command: AdminCreateInvestmentCommand) {
        val investment = Investment.confirmedByAdmin(
            userId = command.userId,
            paymentId = ADMIN_PAYMENT_PREFIX + UUID.randomUUID().toString(),
            amount = command.amount,
            type = InvestmentType.valueOf(command.type),
            depositDate = command.depositDate,
            conversionDate = command.conversionDate,
            confirmMethod = command.confirmMethod,
        )

        val saved = investmentRepository.save(investment)

        eventPublisher.publish(
            topic = DomainTopics.INVESTMENT_REGISTERED,
            key = saved.userId.toString(),
            eventType = DomainEventTypes.INVESTMENT_REGISTERED,
            payload = InvestmentRegisteredEvent(
                investmentId = saved.investmentId,
                userId = saved.userId,
                amount = saved.amount,
            ),
        )
    }

    companion object {
        const val ADMIN_PAYMENT_PREFIX = "ADMIN-"
    }
}
