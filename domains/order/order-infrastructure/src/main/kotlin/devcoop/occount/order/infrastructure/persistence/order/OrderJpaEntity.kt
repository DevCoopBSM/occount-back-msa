package devcoop.occount.order.infrastructure.persistence.order

import devcoop.occount.order.domain.order.OrderStatus
import devcoop.occount.order.domain.order.OrderStepStatus
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant

@Entity
@Table(name = "orders")
class OrderJpaEntity(
    @Id
    @field:Column(name = "order_id", nullable = false, length = 36)
    private var orderId: String = "",
    @field:Column(name = "user_id", nullable = true)
    private var userId: Long? = null,
    @field:OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    private var requestedLines: MutableList<RequestedOrderLineJpaEntity> = mutableListOf(),
    @field:OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true)
    private var lines: MutableList<OrderLineJpaEntity> = mutableListOf(),
    @field:Column(name = "total_amount", nullable = false)
    private var totalAmount: Int = 0,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "status", nullable = false)
    private var status: OrderStatus = OrderStatus.PENDING,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "payment_status", nullable = false)
    private var paymentStatus: OrderStepStatus = OrderStepStatus.PENDING,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "stock_status", nullable = false)
    private var stockStatus: OrderStepStatus = OrderStepStatus.PENDING,
    @field:Column(name = "cancel_requested", nullable = false)
    private var cancelRequested: Boolean = false,
    @field:Column(name = "failure_reason")
    private var failureReason: String? = null,
    @field:Column(name = "kiosk_id", nullable = false)
    private var kioskId: String = "",
    @field:Column(name = "expires_at", nullable = false)
    private var expiresAt: Instant = Instant.now(),
    @field:Column(name = "payment_log_id")
    private var paymentLogId: Long? = null,
    @field:Column(name = "points_used", nullable = false)
    private var pointsUsed: Int = 0,
    @field:Column(name = "card_amount", nullable = false)
    private var cardAmount: Int = 0,
    @field:Column(name = "transaction_id")
    private var transactionId: String? = null,
    @field:Column(name = "approval_number")
    private var approvalNumber: String? = null,
    @field:Column(name = "payment_requested", nullable = false)
    private var paymentRequested: Boolean = false,
    @field:Column(name = "payment_cancellation_requested", nullable = false)
    private var paymentCancellationRequested: Boolean = false,
    @field:Column(name = "payment_compensation_requested", nullable = false)
    private var paymentCompensationRequested: Boolean = false,
    @field:Column(name = "stock_compensation_requested", nullable = false)
    private var stockCompensationRequested: Boolean = false,
    @Version
    @field:Column(name = "version", nullable = false)
    private var version: Long = 0L,
) {
    fun replaceRequestedLines(lines: MutableList<RequestedOrderLineJpaEntity>) {
        requestedLines = lines
    }

    fun replaceLines(lines: MutableList<OrderLineJpaEntity>) {
        this.lines = lines
    }

    fun getOrderId() = orderId
    fun getUserId() = userId
    fun getRequestedLines() = requestedLines.toList()
    fun getLines() = lines.toList()
    fun getTotalAmount() = totalAmount
    fun getStatus() = status
    fun getPaymentStatus() = paymentStatus
    fun getStockStatus() = stockStatus
    fun isCancelRequested() = cancelRequested
    fun getFailureReason() = failureReason
    fun getKioskId() = kioskId
    fun getExpiresAt() = expiresAt
    fun getPaymentLogId() = paymentLogId
    fun getPointsUsed() = pointsUsed
    fun getCardAmount() = cardAmount
    fun getTransactionId() = transactionId
    fun getApprovalNumber() = approvalNumber
    fun isPaymentRequested() = paymentRequested
    fun isPaymentCancellationRequested() = paymentCancellationRequested
    fun isPaymentCompensationRequested() = paymentCompensationRequested
    fun isStockCompensationRequested() = stockCompensationRequested
    fun getVersion() = version
}
