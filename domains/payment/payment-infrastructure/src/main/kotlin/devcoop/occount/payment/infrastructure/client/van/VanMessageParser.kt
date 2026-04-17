package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.dto.response.AdditionalResult
import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.application.dto.response.VanResult
import devcoop.occount.payment.domain.payment.CardType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class VanMessageParser(
    private val protocolSpec: VanProtocolSpec,
) {
    private val log = LoggerFactory.getLogger(VanMessageParser::class.java)
    private val eucKr: Charset = Charset.forName("EUC-KR")
    private val dateTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun parsePaymentResponse(response: ByteArray): VanResult? {
        val startTime = LocalDateTime.now()
        val rawResponse = protocolSpec.toHex(response)

        return try {
            if (response.isEmpty()) {
                return failureResult(
                    message = "알 수 없는 응답: 빈 응답",
                    errorCode = "UNKNOWN_RESPONSE",
                    rawResponse = rawResponse,
                )
            }

            if (response.first() != protocolSpec.stxByte) {
                return parseNonStxResponse(rawResponse)
            }

            val fields = response.toString(eucKr).split(protocolSpec.separatorChar)
            log.info("응답 필드 개수: {}", fields.size)
            log.debug("응답 필드 데이터: {}", fields)

            if (fields.size == CARD_INSERT_MSG_FIELD_COUNT && field(fields, F_CARD_NUMBER).contains("카드 삽입")) {
                log.info("중간 메시지 수신: {}", field(fields, F_CARD_NUMBER).trim())
                return null
            }

            val messageType = field(fields, F_MESSAGE_NUMBER).drop(1).take(4)
            val isCancel = messageType == CANCEL_MESSAGE_TYPE
            val offset = if (isCancel) CANCEL_OFFSET else 0
            val statusField = field(fields, F_STATUS + offset)

            if (statusField.startsWith("9Q")) {
                val rejectReason = fields.getOrNull(F_IC_CREDIT)?.split(protocolSpec.recordSeparatorChar)?.getOrNull(1) ?: "알 수 없는 거절 사유"
                return failureResult(
                    message = "거래가 거절되었습니다: $rejectReason",
                    errorCode = "TRANSACTION_REJECTED",
                    transaction = TransactionResult(
                        messageNumber = field(fields, F_MESSAGE_NUMBER).drop(1).ifBlank { null },
                        typeCode = field(fields, F_TYPE_CODE).blankToNull(),
                        cardNumber = field(fields, F_CARD_NUMBER).blankToNull(),
                        amount = field(fields, F_AMOUNT).toIntOrNull() ?: 0,
                        installmentMonths = field(fields, F_INSTALLMENT_MONTHS).toIntOrNull() ?: 0,
                        cancelType = field(fields, F_CANCEL_TYPE).blankToNull(),
                        approvalNumber = null,
                        approvalDate = field(fields, F_APPROVAL_DATE).blankToNull(),
                        approvalTime = field(fields, F_APPROVAL_TIME).blankToNull(),
                        transactionId = field(fields, F_TRANSACTION_ID).blankToNull(),
                        terminalId = field(fields, F_TERMINAL_ID).blankToNull(),
                        merchantNumber = field(fields, F_MERCHANT_NUMBER).blankToNull(),
                        rejectCode = statusField,
                        rejectMessage = rejectReason,
                    ),
                    rawResponse = rawResponse,
                )
            }

            val statusAndApproval = statusField.split(protocolSpec.recordSeparatorChar)
            val cardStatus = statusAndApproval.firstOrNull().orEmpty()
            val approvalNumber = statusAndApproval.getOrNull(1).orEmpty().ifBlank { field(fields, F_MERCHANT_NUMBER) }

            if (approvalNumber.isBlank()) {
                return null
            }

            val cardInfo = field(fields, F_ISSUER_NAME).trim()
            val cardBrand = when {
                cardInfo.contains("VISA") -> "VISA"
                cardInfo.contains("MASTER") -> "MASTER"
                else -> null
            }
            val cardName = when (cardBrand) {
                "VISA" -> cardInfo.replace("VISA", "").trim().blankToNull()
                "MASTER" -> cardInfo.replace("MASTER", "").trim().blankToNull()
                else -> cardInfo.blankToNull()
            }

            val endTime = LocalDateTime.now()

            VanResult(
                success = true,
                message = if (isCancel) "정상취소 완료" else "정상승인 완료",
                errorCode = null,
                transaction = TransactionResult(
                    messageNumber = field(fields, F_MESSAGE_NUMBER).drop(1).ifBlank { null },
                    typeCode = field(fields, F_TYPE_CODE).blankToNull(),
                    cardNumber = field(fields, F_CARD_NUMBER).blankToNull(),
                    amount = field(fields, F_AMOUNT).toIntOrNull(),
                    installmentMonths = field(fields, F_INSTALLMENT_MONTHS).toIntOrNull(),
                    cancelType = field(fields, F_CANCEL_TYPE).blankToNull(),
                    approvalNumber = approvalNumber,
                    approvalDate = field(fields, F_APPROVAL_DATE).blankToNull(),
                    approvalTime = field(fields, F_APPROVAL_TIME).blankToNull(),
                    transactionId = field(fields, F_TRANSACTION_ID).blankToNull(),
                    terminalId = field(fields, F_TERMINAL_ID).blankToNull(),
                    merchantNumber = field(fields, F_MERCHANT_NUMBER).blankToNull(),
                    rejectCode = null,
                    rejectMessage = null,
                ),
                card = CardResult(
                    acquirerCode = field(fields, F_ACQUIRER_CODE).blankToNull(),
                    acquirerName = field(fields, F_ACQUIRER_NAME).blankToNull(),
                    issuerCode = field(fields, F_ISSUER_CODE).blankToNull(),
                    issuerName = field(fields, F_ISSUER_NAME).blankToNull(),
                    cardType = if (field(fields, F_CARD_TYPE) == "2") CardType.CREDIT else CardType.DEBIT,
                    cardCategory = cardStatus.blankToNull(),
                    cardName = cardName,
                    cardBrand = cardBrand,
                ),
                additional = AdditionalResult(
                    approvalStatus = if (isCancel) "CANCELLED" else "APPROVED",
                    approvalCode = approvalNumber,
                    icCreditApproval = field(fields, F_IC_CREDIT).blankToNull(),
                    transactionUuid = field(fields, F_UUID).blankToNull(),
                    vanMessage = statusField.blankToNull(),
                    processingTime = Duration.between(startTime, endTime).toMillis().toFloat() / 1000f,
                    requestAt = startTime.format(dateTimeFormatter),
                    responseAt = endTime.format(dateTimeFormatter),
                ),
                rawResponse = rawResponse,
            )
        } catch (e: Exception) {
            log.error("응답 파싱 에러: {}", e.message, e)
            failureResult(
                message = e.message,
                errorCode = "PARSING_ERROR",
                rawResponse = rawResponse,
            )
        }
    }

    private fun parseNonStxResponse(rawResponse: String): VanResult {
        return when (rawResponse) {
            protocolSpec.nakHex -> failureResult(
                message = "거래가 거절되었습니다",
                errorCode = "TRANSACTION_REJECTED",
                rawResponse = rawResponse,
            )

            protocolSpec.dleCompletedHex -> VanResult(
                success = true,
                message = "정상취소 완료",
                errorCode = null,
                transaction = null,
                card = null,
                additional = null,
                rawResponse = rawResponse,
            )

            else -> failureResult(
                message = "알 수 없는 응답: $rawResponse",
                errorCode = "UNKNOWN_RESPONSE",
                rawResponse = rawResponse,
            )
        }
    }

    private fun failureResult(
        message: String?,
        errorCode: String,
        transaction: TransactionResult? = null,
        rawResponse: String?,
    ): VanResult {
        return VanResult(
            success = false,
            message = message,
            errorCode = errorCode,
            transaction = transaction,
            card = null,
            additional = null,
            rawResponse = rawResponse,
        )
    }

    private fun field(fields: List<String>, index: Int): String {
        return fields.getOrNull(index).orEmpty()
    }

    private fun String.blankToNull(): String? {
        return ifBlank { null }
    }

    companion object {
        private const val F_MESSAGE_NUMBER = 0
        private const val F_TYPE_CODE = 1
        private const val F_CARD_NUMBER = 2
        private const val F_AMOUNT = 3
        private const val F_INSTALLMENT_MONTHS = 4
        private const val F_CANCEL_TYPE = 5
        private const val F_TERMINAL_ID = 6
        private const val F_MERCHANT_NUMBER = 7
        private const val F_APPROVAL_DATE = 8
        private const val F_APPROVAL_TIME = 9
        private const val F_TRANSACTION_ID = 10
        private const val F_ACQUIRER_CODE = 11
        private const val F_ACQUIRER_NAME = 12
        private const val F_ISSUER_NAME = 13
        private const val F_ISSUER_CODE = 14
        private const val F_CARD_TYPE = 15
        private const val F_STATUS = 16       // 취소 메시지는 F_STATUS + CANCEL_OFFSET
        private const val F_IC_CREDIT = 17
        private const val F_UUID = 18
        private const val CANCEL_OFFSET = 1
        private const val CANCEL_MESSAGE_TYPE = "0203"
        private const val CARD_INSERT_MSG_FIELD_COUNT = 5
    }
}
