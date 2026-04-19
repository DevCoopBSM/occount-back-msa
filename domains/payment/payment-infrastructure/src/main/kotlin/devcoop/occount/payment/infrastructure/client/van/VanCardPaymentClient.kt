package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.response.VanResult
import devcoop.occount.core.common.exception.BusinessBaseException
import devcoop.occount.payment.application.exception.PaymentCancelledException
import devcoop.occount.payment.application.exception.InvalidPaymentRequestException
import devcoop.occount.payment.application.exception.PaymentFailedException
import devcoop.occount.payment.application.exception.PaymentTimeoutException
import devcoop.occount.payment.application.exception.TransactionInProgressException
import devcoop.occount.payment.application.output.CardPaymentPort
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class VanCardPaymentClient(
    private val vanTerminalRegistry: VanTerminalRegistry,
) : CardPaymentPort {
    private val log = LoggerFactory.getLogger(VanCardPaymentClient::class.java)

    override fun approve(amount: Int, items: List<ItemCommand>, kioskId: String, paymentKey: String?): VanResult {
        log.info("카드결제 요청 - 키오스크: {}, 금액: {}원, 상품 수: {}개", kioskId, amount, items.size)
        return execute("카드결제") {
            vanTerminalRegistry.get(kioskId).approve(amount = amount, items = items, paymentKey = paymentKey)
        }
    }

    override fun refund(
        transactionId: String?,
        approvalNumber: String?,
        approvalDate: String,
        amount: Int,
        kioskId: String,
    ): VanResult {
        if (approvalNumber == null) throw InvalidPaymentRequestException()
        log.info("카드환불 요청 - 키오스크: {}, 승인번호: {}, 금액: {}원", kioskId, approvalNumber, amount)
        return execute("카드환불") {
            vanTerminalRegistry.get(kioskId).refund(
                approvalNumber = approvalNumber,
                approvalDate = approvalDate,
                amount = amount,
            )
        }
    }

    override fun requestPendingApprovalCancellation(paymentKey: String, kioskId: String) {
        vanTerminalRegistry.get(kioskId).requestPendingApprovalCancellation(paymentKey)
    }

    private fun execute(actionName: String, operation: () -> VanResult): VanResult {
        val result = try {
            operation()
        } catch (e: BusinessBaseException) {
            throw e
        } catch (e: Exception) {
            log.error("{} 처리 중 오류 발생: {}", actionName, e.message, e)
            throw PaymentFailedException()
        }

        if (result.success) {
            return result
        }

        when (result.errorCode) {
            "TRANSACTION_TIMEOUT", "TIMEOUT" -> {
                log.error("{} 타임아웃 발생", actionName)
                throw PaymentTimeoutException()
            }

            "TRANSACTION_IN_PROGRESS" -> {
                log.warn("진행 중인 거래 감지됨")
                throw TransactionInProgressException()
            }

            "USER_CANCELLED" -> throw PaymentCancelledException()

            "CONNECTION_FAILED" -> throw PaymentFailedException()
            null -> throw InvalidPaymentRequestException()

            else -> throw InvalidPaymentRequestException()
        }
    }
}
