package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionTransactionType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "contribution_transaction")
class ContributionTransactionJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @field:Column(nullable = false)
    val userId: Long = 0L,
    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false, length = 20)
    val type: ContributionTransactionType = ContributionTransactionType.DEPOSIT,
    @field:Column(nullable = false)
    val amount: Int = 0,
    @field:Column(nullable = false)
    val beforeBalance: Int = 0,
    @field:Column(nullable = false)
    val afterBalance: Int = 0,
    @field:Column(nullable = false)
    val sourceRequestId: Long = 0L,
    @field:Column(nullable = false)
    val occurredAt: LocalDateTime = LocalDateTime.now(),
)
