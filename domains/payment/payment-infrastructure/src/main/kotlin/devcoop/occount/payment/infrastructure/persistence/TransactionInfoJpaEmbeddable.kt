package devcoop.occount.payment.infrastructure.persistence

import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
class TransactionInfoJpaEmbeddable(
    @field:Column(name = "transaction_id")
    private var transactionId: String? = null,
    @field:Column(name = "approval_number")
    private var approvalNumber: String? = null,
    @field:Column(name = "card_number")
    private var cardNumber: String? = null,
    @field:Column(name = "amount")
    private var amount: Int? = null,
    @field:Column(name = "installment_months")
    private var installmentMonths: Int? = null,
    @field:Column(name = "approval_date")
    private var approvalDate: String? = null,
    @field:Column(name = "approval_time")
    private var approvalTime: String? = null,
    @field:Column(name = "terminal_id")
    private var terminalId: String? = null,
    @field:Column(name = "merchant_number")
    private var merchantNumber: String? = null,
) {
    fun getTransactionId() = transactionId
    fun getApprovalNumber() = approvalNumber
    fun getCardNumber() = cardNumber
    fun getAmount() = amount
    fun getInstallmentMonths() = installmentMonths
    fun getApprovalDate() = approvalDate
    fun getApprovalTime() = approvalTime
    fun getTerminalId() = terminalId
    fun getMerchantNumber() = merchantNumber
}
