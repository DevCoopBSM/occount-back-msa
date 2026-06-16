package devcoop.occount.investment.application.output

import devcoop.occount.investment.domain.investment.Investment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

interface InvestmentRepository {
    fun findByPaymentId(paymentId: String): Investment?
    fun findById(investmentId: Long): Investment?
    fun findByUserId(userId: Long, pageable: Pageable): Page<Investment>
    fun findAll(pageable: Pageable): Page<Investment>
    fun save(investment: Investment): Investment

    /**
     * PENDING 상태의 출자를 원자적으로 CONFIRMED 로 전환한다.
     * 단 한 트랜잭션만 전이에 성공하도록 조건부 UPDATE(`status = PENDING`)로 구현해야 한다.
     * @return 이 호출이 실제로 전이시켰으면 true(1행), 이미 다른 곳에서 확정됐으면 false(0행).
     */
    fun confirmIfPending(paymentId: String, confirmedAt: LocalDateTime): Boolean
}
