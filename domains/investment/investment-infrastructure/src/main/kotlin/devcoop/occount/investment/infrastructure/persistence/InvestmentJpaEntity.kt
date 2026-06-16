package devcoop.occount.investment.infrastructure.persistence

import devcoop.occount.investment.domain.investment.InvestmentStatus
import devcoop.occount.investment.domain.investment.InvestmentType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "investment",
    indexes = [Index(name = "idx_investment_user_id", columnList = "user_id, investment_id")],
)
class InvestmentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @field:Column(name = "investment_id")
    private var investmentId: Long = 0L,
    @field:Column(name = "user_id", nullable = false)
    private var userId: Long = 0L,
    @field:Column(name = "payment_id", nullable = false, unique = true, length = 80)
    private var paymentId: String = "",
    @field:Column(nullable = false)
    private var amount: Int = 0,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "investment_type", nullable = false)
    private var type: InvestmentType = InvestmentType.DEPOSIT,
    @Enumerated(EnumType.STRING)
    @field:Column(nullable = false)
    private var status: InvestmentStatus = InvestmentStatus.PENDING,
    @field:Column(name = "deposit_date")
    private var depositDate: LocalDateTime? = null,
    @field:Column(name = "conversion_date")
    private var conversionDate: LocalDateTime? = null,
    @field:Column(name = "confirm_method", length = 255)
    private var confirmMethod: String = "",
) {
    fun getInvestmentId() = investmentId
    fun getUserId() = userId
    fun getPaymentId() = paymentId
    fun getAmount() = amount
    fun getType() = type
    fun getStatus() = status
    fun getDepositDate() = depositDate
    fun getConversionDate() = conversionDate
    fun getConfirmMethod() = confirmMethod
}
