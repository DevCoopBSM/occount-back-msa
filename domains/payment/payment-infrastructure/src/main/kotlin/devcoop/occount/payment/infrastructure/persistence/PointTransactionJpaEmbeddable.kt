package devcoop.occount.payment.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class PointTransactionJpaEmbeddable(
    @field:Column(name = "before_point", nullable = false)
    private var beforePoint: Int = 0,
    @field:Column(name = "transaction_point", nullable = false)
    private var transactionPoint: Int = 0,
    @field:Column(name = "after_point", nullable = false)
    private var afterPoint: Int = 0,
) {
    fun getBeforePoint() = beforePoint
    fun getTransactionPoint() = transactionPoint
    fun getAfterPoint() = afterPoint
}
