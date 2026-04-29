package devcoop.occount.suggestion.infrastructure.persistence.aripick

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "aripick_like",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_aripick_like_proposal_user", columnNames = ["proposal_id", "user_id"]),
    ],
)
class AripickLikeJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "like_id")
    private var likeId: Long = 0L,
    @field:Column(name = "proposal_id", nullable = false)
    private var proposalId: Long = 0L,
    @field:Column(name = "user_id", nullable = false)
    private var userId: Long = 0L,
)
