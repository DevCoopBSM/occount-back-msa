package devcoop.occount.payment.infrastructure.persistence.chargelog

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class PointTransactionJpaEmbeddable(
    @field:Column(name = "before_point", nullable = false)
    private var beforePoint: Int = 0,
    @field:Column(name = "change_point", nullable = false)
    private var changeAmount: Int = 0,
    @field:Column(name = "after_point", nullable = false)
    private var afterPoint: Int = 0,
) {
    fun getBeforePoint() = beforePoint
    fun getChangePoint() = changeAmount
    fun getAfterPoint() = afterPoint
}
