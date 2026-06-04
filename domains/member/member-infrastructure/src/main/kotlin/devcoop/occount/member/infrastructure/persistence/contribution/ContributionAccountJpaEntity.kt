package devcoop.occount.member.infrastructure.persistence.contribution

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "contribution_account")
class ContributionAccountJpaEntity(
    @Id
    @field:Column(nullable = false)
    val userId: Long = 0L,
    @field:Column(nullable = false)
    val balance: Int = 0,
    @Version
    @field:Column(nullable = false)
    val version: Long? = null,
)
