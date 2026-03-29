package devcoop.occount.payment.application.dto.response

import devcoop.occount.payment.domain.vo.TransactionInfo

data class TransactionResult(
    val messageNumber: String?,
    val typeCode: String?,
    val cardNumber: String?,
    val amount: Int?,
    val installmentMonths: Int?,
    val cancelType: String?,
    val approvalNumber: String?,
    val approvalDate: String?,
    val approvalTime: String?,
    val transactionId: String?,
    val terminalId: String?,
    val merchantNumber: String?,
    val rejectCode: String?,
    val rejectMessage: String?
) {
    companion object {
        fun from(transactionInfo: TransactionInfo): TransactionResult {
            return TransactionResult(
                messageNumber = null,
                typeCode = null,
                cardNumber = transactionInfo.cardNumber(),
                amount = transactionInfo.amount(),
                installmentMonths = transactionInfo.installmentMonths(),
                cancelType = null,
                approvalNumber = transactionInfo.approvalNumber(),
                approvalDate = transactionInfo.approvalDate(),
                approvalTime = transactionInfo.approvalTime(),
                transactionId = transactionInfo.transactionId(),
                terminalId = transactionInfo.terminalId(),
                merchantNumber = transactionInfo.merchantNumber(),
                rejectCode = null,
                rejectMessage = null,
            )
        }

        fun toDomain(transactionResult: TransactionResult): TransactionInfo {
            return TransactionInfo(
                transactionId = transactionResult.transactionId,
                approvalNumber = transactionResult.approvalNumber,
                cardNumber = transactionResult.cardNumber,
                amount = transactionResult.amount,
                installmentMonths = transactionResult.installmentMonths,
                approvalDate = transactionResult.approvalDate,
                approvalTime = transactionResult.approvalTime,
                terminalId = transactionResult.terminalId,
                merchantNumber = transactionResult.merchantNumber,
            )
        }
    }
}
