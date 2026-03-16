package devcoop.occount.payment.application.dto.response

import devcoop.occount.payment.domain.type.CardType

data class CardInfo(
    val acquirerCode: String?,
    val acquirerName: String?,
    val issuerCode: String?,
    val issuerName: String?,
    val cardType: CardType?,
    val cardCategory: String?,
    val cardName: String?,
    val cardBrand: String?
)
