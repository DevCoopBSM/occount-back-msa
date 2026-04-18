package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.domain.payment.CardType

/**
 * VAN 응답 전문의 원시 데이터를 의미있는 필드로 매핑한 데이터 클래스
 * 필드 순서가 변경되어도 영향받지 않도록 필드명으로 접근
 */
data class VanRawResponse(
    private val protocolCodes: VanProtocolCodes,
    val messageNumber: String?,
    val typeCode: String?,
    val cardNumber: String?,
    val amount: Int?,
    val installmentMonths: Int?,
    val cancelType: String?,
    val terminalId: String?,
    val merchantNumber: String?,
    val approvalDate: String?,
    val approvalTime: String?,
    val transactionId: String?,
    val acquirerCode: String?,
    val acquirerName: String?,
    val issuerName: String?,
    val issuerCode: String?,
    val cardType: CardType?,
    val status: String?,
    val icCredit: String?,
    val uuid: String?,
) {
    fun isCardInsertMessage(): Boolean {
        return protocolCodes.cardInsertKeyword.isNotBlank() && cardNumber?.contains(protocolCodes.cardInsertKeyword) == true
    }

    fun isRejected(): Boolean {
        return protocolCodes.rejectStatusPrefix.isNotBlank() && status?.startsWith(protocolCodes.rejectStatusPrefix) == true
    }

    fun isCancelMessage(): Boolean {
        return messageNumber?.drop(1)?.take(4) == protocolCodes.cancelMessageType
    }

    fun getApprovalInfo(): Pair<String?, String?> {
        val statusAndApproval = status?.split('\u001e') // Record Separator
        return Pair(
            statusAndApproval?.firstOrNull(),
            statusAndApproval?.getOrNull(1)?.ifBlank { merchantNumber }
        )
    }

    fun extractCardInfo(): Triple<String?, String?, String?> {
        val cardInfo = issuerName?.trim() ?: ""
        val cardBrand = when {
            cardInfo.contains("VISA") -> "VISA"
            cardInfo.contains("MASTER") -> "MASTER"
            else -> null
        }
        val cardName = when (cardBrand) {
            "VISA" -> cardInfo.replace("VISA", "").trim().takeIf { it.isNotBlank() }
            "MASTER" -> cardInfo.replace("MASTER", "").trim().takeIf { it.isNotBlank() }
            else -> cardInfo.takeIf { it.isNotBlank() }
        }
        return Triple(cardBrand, cardName, cardInfo)
    }

    fun getRejectionReason(): String {
        return icCredit?.split('\u001e')?.getOrNull(1) ?: "알 수 없는 거절 사유"
    }
}
