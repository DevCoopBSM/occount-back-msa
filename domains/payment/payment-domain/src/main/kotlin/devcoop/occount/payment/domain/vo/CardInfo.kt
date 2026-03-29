package devcoop.occount.payment.domain.vo

import devcoop.occount.payment.domain.type.CardType

class CardInfo(
    private var issuerCode: String? = null,
    private var issuerName: String? = null,
    private var acquirerCode: String? = null,
    private var acquirerName: String? = null,
    private var cardType: CardType? = null,
    private var cardCategory: String? = null,
    private var cardName: String? = null,
    private var cardBrand: String? = null,
) {
    fun issuerCode(): String? = issuerCode
    fun issuerName(): String? = issuerName
    fun acquirerCode(): String? = acquirerCode
    fun acquirerName(): String? = acquirerName
    fun cardType(): CardType? = cardType
    fun cardCategory(): String? = cardCategory
    fun cardName(): String? = cardName
    fun cardBrand(): String? = cardBrand
}
