package devcoop.occount.payment.application.dto.response

import devcoop.occount.payment.domain.type.CardType
import devcoop.occount.payment.domain.vo.CardInfo

data class CardResult(
    val acquirerCode: String?,
    val acquirerName: String?,
    val issuerCode: String?,
    val issuerName: String?,
    val cardType: CardType?,
    val cardCategory: String?,
    val cardName: String?,
    val cardBrand: String?
) {
    companion object {
        fun from(cardInfo: CardInfo): CardResult {
            return CardResult(
                acquirerCode = cardInfo.acquirerCode(),
                acquirerName = cardInfo.acquirerName(),
                issuerCode = cardInfo.issuerCode(),
                issuerName = cardInfo.issuerName(),
                cardType = cardInfo.cardType(),
                cardCategory = cardInfo.cardCategory(),
                cardName = cardInfo.cardName(),
                cardBrand = cardInfo.cardBrand(),
            )
        }

        fun toDomain(cardResult: CardResult): CardInfo {
            return CardInfo(
                issuerCode = cardResult.issuerCode,
                issuerName = cardResult.issuerName,
                acquirerCode = cardResult.acquirerCode,
                acquirerName = cardResult.acquirerName,
                cardType = cardResult.cardType,
                cardCategory = cardResult.cardCategory,
                cardName = cardResult.cardName,
                cardBrand = cardResult.cardBrand,
            )
        }
    }
}
