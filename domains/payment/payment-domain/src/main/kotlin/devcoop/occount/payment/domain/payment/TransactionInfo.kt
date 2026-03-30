package devcoop.occount.payment.domain.payment

class TransactionInfo(
    private var transactionId: String? = null,
    private var approvalNumber: String? = null,
    private var cardNumber: String? = null,
    private var amount: Int? = null,
    private var installmentMonths: Int? = null,
    private var approvalDate: String? = null,
    private var approvalTime: String? = null,
    private var terminalId: String? = null,
    private var merchantNumber: String? = null,
) {
    fun transactionId(): String? = transactionId
    fun approvalNumber(): String? = approvalNumber
    fun cardNumber(): String? = cardNumber
    fun amount(): Int? = amount
    fun installmentMonths(): Int? = installmentMonths
    fun approvalDate(): String? = approvalDate
    fun approvalTime(): String? = approvalTime
    fun terminalId(): String? = terminalId
    fun merchantNumber(): String? = merchantNumber
}
