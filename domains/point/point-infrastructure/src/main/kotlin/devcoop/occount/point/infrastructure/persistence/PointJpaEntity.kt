package devcoop.occount.point.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version

@Entity
@Table(name = "user_point")
class PointJpaEntity(
    @Id
    @field:Column(name = "user_id", nullable = false)
    private var userId: Long = 0L,
    @field:Column(nullable = false)
    private var balance: Int = 0,
    @Version
    @field:Column(nullable = false)
    private var version: Long = 0L,
) {
    fun getUserId() = userId
    fun getBalance() = balance
    fun getVersion() = version
}
