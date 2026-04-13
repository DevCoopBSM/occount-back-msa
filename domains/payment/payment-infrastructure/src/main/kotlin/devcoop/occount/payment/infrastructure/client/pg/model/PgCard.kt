package devcoop.occount.payment.infrastructure.client.pg.model

import devcoop.occount.payment.domain.payment.CardType

data class PgCard(
    val acquirerCode: String?,
    val acquirerName: String?,
    val issuerCode: String?,
    val issuerName: String?,
    val cardType: CardType?,
    val cardCategory: String?,
    val cardName: String?,
    val cardBrand: String?,
)
