package devcoop.occount.investment.infrastructure.persistence

import devcoop.occount.investment.domain.investment.InvestmentStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface InvestmentPersistenceRepository : JpaRepository<InvestmentJpaEntity, Long> {
    fun findByPaymentId(paymentId: String): InvestmentJpaEntity?
    fun findByUserId(userId: Long, pageable: Pageable): Page<InvestmentJpaEntity>

    /**
     * PENDING → CONFIRMED 원자적 전이. 조건절(`status = :pending`)로 동시 확정 레이스를 1행만 통과시킨다.
     * @return 갱신된 행 수(전이 성공 1, 이미 확정/없음 0).
     */
    @Modifying
    @Query(
        """
        UPDATE InvestmentJpaEntity i
        SET i.status = :confirmed, i.depositDate = :confirmedAt
        WHERE i.paymentId = :paymentId AND i.status = :pending
        """,
    )
    fun confirmIfPending(
        @Param("paymentId") paymentId: String,
        @Param("confirmedAt") confirmedAt: LocalDateTime,
        @Param("pending") pending: InvestmentStatus,
        @Param("confirmed") confirmed: InvestmentStatus,
    ): Int
}
