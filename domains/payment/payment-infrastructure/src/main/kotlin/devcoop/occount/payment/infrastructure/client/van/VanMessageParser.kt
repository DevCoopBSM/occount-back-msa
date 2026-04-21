package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.dto.response.AdditionalResult
import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.application.dto.response.VanResult
import devcoop.occount.payment.domain.payment.CardType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Component
class VanMessageParser(
    private val protocolSpec: VanProtocolSpec,
    private val vanResponseParser: VanResponseParser,
) {
    private val log = LoggerFactory.getLogger(VanMessageParser::class.java)
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

            val vanResponse = vanResponseParser.parseToRawResponse(response) ?: run {
                log.debug("제어 문자 또는 파싱 불가 응답 (무시): {}", rawResponse)
                return null
            }

            convertToVanResult(vanResponse, startTime, rawResponse)
        } catch (e: Exception) {
            log.error("응답 파싱 에러: {}", e.message, e)
            failureResult(
                message = e.message,
                errorCode = "PARSING_ERROR",
                rawResponse = rawResponse,
            )
        }
    }

    private fun convertToVanResult(
        vanResponse: VanRawResponse,
        startTime: LocalDateTime,
        rawResponse: String,
    ): VanResult? {
        if (vanResponse.isCardInsertMessage()) {
            log.info("중간 메시지 수신: {}", vanResponse.cardNumber?.trim())
            return null
        }

        if (vanResponse.isRejected()) {
            val rejectReason = vanResponse.getRejectionReason()
            return failureResult(
                message = "거래가 거절되었습니다: $rejectReason",
                errorCode = "TRANSACTION_REJECTED",
                transaction = createTransactionResult(
                    vanResponse,
                    rejectCode = vanResponse.status,
                    rejectMessage = rejectReason,
                ),
                rawResponse = rawResponse,
            )
        }

        val (cardStatus, approvalNumber) = vanResponse.getApprovalInfo()
        if (approvalNumber.isNullOrBlank()) {
            return null
        }

        val (cardBrand, cardName, _) = vanResponse.extractCardInfo()
        val isCancel = vanResponse.isCancelMessage()
        val endTime = LocalDateTime.now()

        return VanResult(
            success = true,
            message = if (isCancel) "정상취소 완료" else "정상승인 완료",
            errorCode = null,
            transaction = createTransactionResult(vanResponse, approvalNumber = approvalNumber),
            card = createCardResult(vanResponse, cardStatus, cardBrand, cardName),
            additional = createAdditionalResult(vanResponse, isCancel, approvalNumber, startTime, endTime),
            rawResponse = rawResponse,
        )
    }

    private fun parseNonStxResponse(rawResponse: String): VanResult? {
        return when (rawResponse) {
            protocolSpec.nakHex -> failureResult(
                message = "거래가 거절되었습니다",
                errorCode = "TRANSACTION_REJECTED",
                rawResponse = rawResponse,
            )

            else -> null
        }
    }

    private fun createTransactionResult(
        vanResponse: VanRawResponse,
        approvalNumber: String? = null,
        rejectCode: String? = null,
        rejectMessage: String? = null,
    ): TransactionResult {
        return TransactionResult(
            messageNumber = vanResponse.serviceType,
            typeCode = vanResponse.typeCode,
            cardNumber = vanResponse.cardNumber,
            amount = vanResponse.amount ?: 0,
            installmentMonths = vanResponse.installmentMonths ?: 0,
            cancelType = vanResponse.cancelType,
            approvalNumber = approvalNumber,
            approvalDate = vanResponse.approvalDate,
            approvalTime = vanResponse.approvalTime,
            transactionId = vanResponse.transactionId,
            terminalId = vanResponse.terminalId,
            merchantNumber = vanResponse.merchantNumber,
            rejectCode = rejectCode,
            rejectMessage = rejectMessage,
        )
    }

    private fun createCardResult(
        vanResponse: VanRawResponse,
        cardStatus: String?,
        cardBrand: String?,
        cardName: String?,
    ): CardResult {
        return CardResult(
            acquirerCode = vanResponse.acquirerCode,
            acquirerName = vanResponse.acquirerName,
            issuerCode = vanResponse.issuerCode,
            issuerName = vanResponse.issuerName,
            cardType = vanResponse.cardType ?: CardType.DEBIT,
            cardCategory = cardStatus,
            cardName = cardName,
            cardBrand = cardBrand,
        )
    }

    private fun createAdditionalResult(
        vanResponse: VanRawResponse,
        isCancel: Boolean,
        approvalNumber: String?,
        startTime: LocalDateTime,
        endTime: LocalDateTime,
    ): AdditionalResult {
        return AdditionalResult(
            approvalStatus = if (isCancel) "CANCELLED" else "APPROVED",
            approvalCode = approvalNumber,
            icCreditApproval = vanResponse.icCredit,
            transactionUuid = vanResponse.uuid,
            vanMessage = vanResponse.status,
            processingTime = Duration.between(startTime, endTime).toMillis().toFloat() / 1000f,
            requestAt = startTime.format(dateTimeFormatter),
            responseAt = endTime.format(dateTimeFormatter),
        )
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
}
