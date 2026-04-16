package devcoop.occount.payment.infrastructure.client.pg.adapter

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.AdditionalResult
import devcoop.occount.payment.application.dto.response.CardResult
import devcoop.occount.payment.application.dto.response.PgResult
import devcoop.occount.payment.application.dto.response.TransactionResult
import devcoop.occount.payment.application.exception.InvalidPaymentRequestException
import devcoop.occount.payment.application.exception.PaymentFailedException
import devcoop.occount.payment.application.exception.PaymentTimeoutException
import devcoop.occount.payment.application.exception.TransactionInProgressException
import devcoop.occount.payment.application.output.CardPaymentPort
import devcoop.occount.payment.infrastructure.client.pg.model.PgProduct
import devcoop.occount.payment.infrastructure.client.pg.model.PgResponse
import devcoop.occount.payment.infrastructure.client.pg.terminal.PgTerminalClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class PgCardPaymentClient(
    private val pgTerminalClient: PgTerminalClient,
) : CardPaymentPort {
    companion object {
        private val log = LoggerFactory.getLogger(PgCardPaymentClient::class.java)
    }

    override fun cancel(transactionId: String?, approvalNumber: String?, approvalDate: String, amount: Int): PgResult {
        if (approvalNumber == null) throw InvalidPaymentRequestException()
        return handleResult(
            pgTerminalClient.cancel(
                amount = amount,
                approvalDate = approvalDate,
                approvalNumber = approvalNumber,
            ).toApplicationResult(),
            "카드 취소",
        )
    }

    override fun approve(amount: Int, items: List<ItemCommand>): PgResult {
        return handleResult(
            pgTerminalClient.approve(amount, items.map(::toPgProduct)).toApplicationResult(),
            "카드 결제",
        )
    }

    private fun toPgProduct(item: ItemCommand): PgProduct {
        return PgProduct(
            name = item.name,
            quantity = item.quantity,
            total = item.total,
        )
    }

    private fun handleResult(result: PgResult, action: String): PgResult {
        if (result.success) {
            return result
        }

        when (result.errorCode) {
            "TIMEOUT", "TRANSACTION_TIMEOUT" -> throw PaymentTimeoutException()
            "TRANSACTION_IN_PROGRESS" -> throw TransactionInProgressException()
            "USER_CANCELLED", "TRANSACTION_REJECTED", "UNKNOWN_RESPONSE", "PARSING_ERROR" -> {
                log.warn("{} 실패 - code: {}, message: {}", action, result.errorCode, result.message)
                throw InvalidPaymentRequestException()
            }

            null -> {
                log.warn("{} 실패 - errorCode 없음, message: {}", action, result.message)
                throw PaymentFailedException()
            }
            else -> {
                log.warn("{} 실패 - code: {}, message: {}", action, result.errorCode, result.message)
                throw PaymentFailedException()
            }
        }
    }

    private fun PgResponse.toApplicationResult(): PgResult {
        return PgResult(
            success = success,
            message = message,
            errorCode = errorCode,
            transaction = transaction?.let {
                TransactionResult(
                    messageNumber = it.messageNumber,
                    typeCode = it.typeCode,
                    cardNumber = it.cardNumber,
                    amount = it.amount,
                    installmentMonths = it.installmentMonths,
                    cancelType = it.cancelType,
                    approvalNumber = it.approvalNumber,
                    approvalDate = it.approvalDate,
                    approvalTime = it.approvalTime,
                    transactionId = it.transactionId,
                    terminalId = it.terminalId,
                    merchantNumber = it.merchantNumber,
                    rejectCode = it.rejectCode,
                    rejectMessage = it.rejectMessage,
                )
            },
            card = card?.let {
                CardResult(
                    acquirerCode = it.acquirerCode,
                    acquirerName = it.acquirerName,
                    issuerCode = it.issuerCode,
                    issuerName = it.issuerName,
                    cardType = it.cardType,
                    cardCategory = it.cardCategory,
                    cardName = it.cardName,
                    cardBrand = it.cardBrand,
                )
            },
            additional = additional?.let {
                AdditionalResult(
                    approvalStatus = it.approvalStatus,
                    approvalCode = it.approvalCode,
                    icCreditApproval = it.icCreditApproval,
                    transactionUuid = it.transactionUuid,
                    vanMessage = it.vanMessage,
                    processingTime = it.processingTime,
                    requestAt = it.requestAt,
                    responseAt = it.responseAt,
                )
            },
            rawResponse = rawResponse,
        )
    }
}
