package devcoop.occount.payment.infrastructure.client.pg.protocol

import devcoop.occount.payment.domain.payment.CardType
import devcoop.occount.payment.infrastructure.client.pg.model.PgAdditional
import devcoop.occount.payment.infrastructure.client.pg.model.PgCard
import devcoop.occount.payment.infrastructure.client.pg.model.PgResponse
import devcoop.occount.payment.infrastructure.client.pg.model.PgTransaction
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PgMessageParser {
    private val log = LoggerFactory.getLogger(PgMessageParser::class.java)
    private val eucKr: Charset = Charset.forName("EUC-KR")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun parsePaymentResponse(response: ByteArray): PgResponse? {
        val startTime = LocalDateTime.now()
        return try {
            if (!response.startsWithStx()) {
                when {
                    response.isSingleNak() -> PgResponse(
                        success = false,
                        message = "거래가 거절되었습니다",
                        errorCode = "TRANSACTION_REJECTED",
                        transaction = null,
                        card = null,
                        additional = null,
                        rawResponse = response.toHexString(),
                    )

                    response.contentEquals(DLE_COMPLETE) -> PgResponse(
                        success = true,
                        message = "정상취소 완료",
                        errorCode = null,
                        transaction = null,
                        card = null,
                        additional = null,
                        rawResponse = response.toHexString(),
                    )

                    else -> PgResponse(
                        success = false,
                        message = "알 수 없는 응답: ${response.toHexString()}",
                        errorCode = "UNKNOWN_RESPONSE",
                        transaction = null,
                        card = null,
                        additional = null,
                        rawResponse = response.toHexString(),
                    )
                }
            }

            val message = response.toString(eucKr)
            val fields = message.split(FIELD_SEPARATOR)
            val messageType = fields.getOrElse(0) { "" }.drop(1).take(4)
            val isCancel = messageType == "0203"
            val statusField = getField(fields, if (isCancel) 17 else 16)

            if (statusField.startsWith("9Q")) {
                val rejectReason = getField(fields, 17).split(RECORD_SEPARATOR).getOrElse(1) { "알 수 없는 거절 사유" }
                return PgResponse(
                    success = false,
                    message = "거래가 거절되었습니다: $rejectReason",
                    errorCode = "TRANSACTION_REJECTED",
                    transaction = PgTransaction(
                        messageNumber = getField(fields, 0).drop(1),
                        typeCode = getField(fields, 1),
                        cardNumber = getField(fields, 2),
                        amount = getField(fields, 3).toIntOrNull(),
                        installmentMonths = getField(fields, 4).toIntOrNull(),
                        cancelType = getField(fields, 5),
                        approvalNumber = "",
                        approvalDate = getField(fields, 8),
                        approvalTime = getField(fields, 9),
                        transactionId = getField(fields, 10),
                        terminalId = getField(fields, 6),
                        merchantNumber = getField(fields, 7),
                        rejectCode = statusField,
                        rejectMessage = rejectReason,
                    ),
                    card = null,
                    additional = null,
                    rawResponse = response.toHexString(),
                )
            }

            val statusParts = statusField.split(RECORD_SEPARATOR)
            val cardStatus = statusParts.firstOrNull().orEmpty()
            val approvalNumber = statusParts.getOrNull(1).orEmpty().ifBlank { getField(fields, 7) }

            if (approvalNumber.isBlank()) {
                return null
            }

            val issuerNameField = getField(fields, 13).trim()
            val (cardBrand, cardName) = extractCardBrand(issuerNameField)
            val endTime = LocalDateTime.now()

            PgResponse(
                success = true,
                message = if (isCancel) "정상취소 완료" else "정상승인 완료",
                errorCode = null,
                transaction = PgTransaction(
                    messageNumber = getField(fields, 0).drop(1),
                    typeCode = getField(fields, 1),
                    cardNumber = getField(fields, 2),
                    amount = getField(fields, 3).toIntOrNull(),
                    installmentMonths = getField(fields, 4).toIntOrNull(),
                    cancelType = getField(fields, 5),
                    approvalNumber = approvalNumber,
                    approvalDate = getField(fields, 8),
                    approvalTime = getField(fields, 9),
                    transactionId = getField(fields, 10),
                    terminalId = getField(fields, 6),
                    merchantNumber = getField(fields, 7),
                    rejectCode = null,
                    rejectMessage = null,
                ),
                card = PgCard(
                    acquirerCode = getField(fields, 11),
                    acquirerName = getField(fields, 12),
                    issuerCode = getField(fields, 14),
                    issuerName = issuerNameField,
                    cardType = if (getField(fields, 15) == "2") CardType.CREDIT else CardType.DEBIT,
                    cardCategory = cardStatus,
                    cardName = cardName,
                    cardBrand = cardBrand,
                ),
                additional = PgAdditional(
                    approvalStatus = if (isCancel) "CANCELLED" else "APPROVED",
                    approvalCode = approvalNumber,
                    icCreditApproval = getField(fields, 17),
                    transactionUuid = getField(fields, 18),
                    vanMessage = statusField,
                    processingTime = Duration.between(startTime, endTime).toMillis().toFloat() / 1_000f,
                    requestAt = startTime.format(dateTimeFormatter),
                    responseAt = endTime.format(dateTimeFormatter),
                ),
                rawResponse = response.toHexString(),
            )
        } catch (ex: Exception) {
            log.error("Failed to parse PG response: {}", response.toHexString(), ex)
            PgResponse(
                success = false,
                message = ex.message,
                errorCode = "PARSING_ERROR",
                transaction = null,
                card = null,
                additional = null,
                rawResponse = response.toHexString(),
            )
        }
    }

    fun isIntermediateStxResponse(response: ByteArray): Boolean {
        if (!response.startsWithStx()) {
            return false
        }
        val fields = response.toString(eucKr).split(FIELD_SEPARATOR)
        return fields.size == 5 && getField(fields, 2).contains("카드 삽입")
    }

    private fun getField(fields: List<String>, index: Int): String {
        return fields.getOrNull(index).orEmpty()
    }

    private fun extractCardBrand(raw: String): Pair<String?, String?> {
        return when {
            raw.contains("VISA") -> "VISA" to raw.replace("VISA", "").trim()
            raw.contains("MASTER") -> "MASTER" to raw.replace("MASTER", "").trim()
            else -> null to raw.ifBlank { null }
        }
    }

    private fun ByteArray.startsWithStx(): Boolean = isNotEmpty() && this[0] == STX

    private fun ByteArray.isSingleNak(): Boolean {
        return size == 3 && this[0] == NAK
    }

    private fun ByteArray.toHexString(): String = joinToString("") { "%02x".format(it) }

    private const val STX: Byte = 0x02
    private const val NAK: Byte = 0x15
    private val DLE_COMPLETE: ByteArray = byteArrayOf(0x10, 0x10, 0x10)
    private const val FIELD_SEPARATOR: Char = '\u001c'
    private const val RECORD_SEPARATOR: Char = '\u001e'
}
