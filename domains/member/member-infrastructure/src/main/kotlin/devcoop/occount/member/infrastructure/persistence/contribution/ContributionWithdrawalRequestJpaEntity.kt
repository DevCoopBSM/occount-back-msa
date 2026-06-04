package devcoop.occount.member.infrastructure.persistence.contribution

import devcoop.occount.member.domain.contribution.ContributionWithdrawalRequestStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDateTime

@Entity
@Table(name = "contribution_withdrawal_request")
class ContributionWithdrawalRequestJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @field:Column(nullable = false)
    val userId: Long = 0L,
    @field:Column(nullable = false)
    val amount: Int = 0,
    @field:Column(length = 100)
    val bankName: String? = null,
    @field:Column(length = 100)
    val accountNumber: String? = null,
    @field:Column(length = 100)
    val accountHolder: String? = null,
    @field:Column(length = 500)
    val memo: String? = null,
    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false, length = 20)
    val status: ContributionWithdrawalRequestStatus = ContributionWithdrawalRequestStatus.PENDING,
    @field:Column(length = 500)
    val rejectionReason: String? = null,
    @field:Column(nullable = false)
    val requestedAt: LocalDateTime = LocalDateTime.now(),
    @Version
    @field:Column(nullable = false)
    val version: Long? = null,
)
