package devcoop.occount.payment.infrastructure.client.van

import devcoop.occount.payment.application.dto.request.ItemCommand
import devcoop.occount.payment.application.dto.request.VanCommand
import devcoop.occount.payment.application.dto.response.VanResult
import devcoop.occount.payment.application.exception.InvalidPaymentRequestException
import devcoop.occount.payment.application.exception.PaymentFailedException
import devcoop.occount.payment.application.exception.PaymentTimeoutException
import devcoop.occount.payment.application.exception.TransactionInProgressException
import devcoop.occount.payment.application.output.CardPaymentPort
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class VanCardPaymentClient(
    @Qualifier("vanClient") private val restClient: RestClient,
) : CardPaymentPort {
    private val log = LoggerFactory.getLogger(VanCardPaymentClient::class.java)

    override fun approve(amount: Int, items: List<ItemCommand>): VanResult {
        log.info("카드결제 요청 - 금액: {}원, 상품 수: {}개", amount, items.size)
        return execute("/api/payment", VanCommand(amount = amount, items = items), "카드결제")
    }

    override fun cancel(
        transactionId: String?,
        approvalNumber: String?,
        approvalDate: String,
        amount: Int,
    ): VanResult {
        if (approvalNumber == null) throw InvalidPaymentRequestException()
        log.info("카드취소 요청 - 승인번호: {}, 금액: {}원", approvalNumber, amount)
        return execute(
            "/api/payment/cancel",
            VanCancelCommand(approvalNumber = approvalNumber, approvalDate = approvalDate, amount = amount),
            "카드취소",
        )
    }

    private fun execute(uri: String, body: Any, actionName: String): VanResult {
        return try {
            restClient.post()
                .uri(uri)
                .body(body)
                .retrieve()
                .onStatus({ it.value() == 400 }) { _, _ -> throw InvalidPaymentRequestException() }
                .onStatus({ it.value() == 409 }) { _, _ ->
                    log.warn("진행 중인 거래 감지됨")
                    throw TransactionInProgressException()
                }
                .onStatus({ it.value() == 408 }) { _, _ ->
                    log.error("{} 타임아웃 발생", actionName)
                    throw PaymentTimeoutException()
                }
                .body(VanResult::class.java)
                ?: throw PaymentFailedException()
        } catch (e: Exception) {
            when (e) {
                is InvalidPaymentRequestException,
                is TransactionInProgressException,
                is PaymentTimeoutException,
                -> throw e
                else -> {
                    log.error("{} 처리 중 오류 발생: {}", actionName, e.message, e)
                    throw PaymentFailedException()
                }
            }
        }
    }
}
