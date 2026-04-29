package devcoop.occount.suggestion.infrastructure.persistence.aripick

import devcoop.occount.suggestion.domain.aripick.AripickStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "aripick_proposal")
class AripickJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "proposal_id")
    private var proposalId: Long = 0L,
    @field:Column(name = "name", nullable = false)
    private var name: String = "",
    @field:Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private var reason: String = "",
    @field:Column(name = "proposal_date", nullable = false)
    private var proposalDate: LocalDate = LocalDate.now(),
    @field:Column(name = "proposer_id", nullable = false)
    private var proposerId: Long = 0L,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "status", nullable = false)
    private var status: AripickStatus = AripickStatus.PENDING,
    @field:Column(name = "like_count", nullable = false)
    private var likeCount: Int = 0,
) {
    fun getProposalId() = proposalId

    fun getName() = name

    fun getReason() = reason

    fun getProposalDate() = proposalDate

    fun getProposerId() = proposerId

    fun getStatus() = status

    fun getLikeCount() = likeCount
}
