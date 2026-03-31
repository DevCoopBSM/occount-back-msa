package devcoop.occount.payment.infrastructure.persistence.paymentlog

import devcoop.occount.payment.domain.payment.CardType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated

@Embeddable
class CardInfoJpaEmbeddable(
    @field:Column(name = "issuer_code")
    private var issuerCode: String? = null,
    @field:Column(name = "issuer_name")
    private var issuerName: String? = null,
    @field:Column(name = "acquirer_code")
    private var acquirerCode: String? = null,
    @field:Column(name = "acquirer_name")
    private var acquirerName: String? = null,
    @Enumerated(EnumType.STRING)
    @field:Column(name = "card_type")
    private var cardType: CardType? = null,
    @field:Column(name = "card_category")
    private var cardCategory: String? = null,
    @field:Column(name = "card_name")
    private var cardName: String? = null,
    @field:Column(name = "card_brand")
    private var cardBrand: String? = null,
) {
    fun getIssuerCode() = issuerCode
    fun getIssuerName() = issuerName
    fun getAcquirerCode() = acquirerCode
    fun getAcquirerName() = acquirerName
    fun getCardType() = cardType
    fun getCardCategory() = cardCategory
    fun getCardName() = cardName
    fun getCardBrand() = cardBrand
}
