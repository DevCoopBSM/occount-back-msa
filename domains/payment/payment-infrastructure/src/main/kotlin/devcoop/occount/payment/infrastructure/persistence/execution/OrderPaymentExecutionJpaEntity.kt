package devcoop.occount.payment.infrastructure.persistence.execution

import devcoop.occount.payment.application.output.OrderPaymentExecutionState
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "order_payment_execution")
class OrderPaymentExecutionJpaEntity(
    @Id
    @field:Column(name = "order_id", nullable = false, updatable = false)
    private var orderId: Long = 0L,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "state", nullable = false)
    private var state: OrderPaymentExecutionState = OrderPaymentExecutionState.PROCESSING,
    @field:Column(name = "cancellation_requested", nullable = false)
    private var cancellationRequested: Boolean = false,
    @field:CreationTimestamp
    @field:Column(name = "created_at", nullable = false, updatable = false)
    private var createdAt: LocalDateTime = LocalDateTime.now(),
    @field:UpdateTimestamp
    @field:Column(name = "updated_at", nullable = false)
    private var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun getOrderId(): Long = orderId

    fun getState(): OrderPaymentExecutionState = state

    fun isCancellationRequested(): Boolean = cancellationRequested

    fun markCancellationRequested() {
        cancellationRequested = true
    }

    fun changeState(nextState: OrderPaymentExecutionState) {
        state = nextState
    }
}
