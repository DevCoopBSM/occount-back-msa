package devcoop.occount.payment.infrastructure.pg

import devcoop.occount.payment.application.dto.request.PgRequest
import devcoop.occount.payment.application.dto.request.ProductInfo
import devcoop.occount.payment.application.dto.response.PgResponse
import devcoop.occount.payment.application.payment.CardPaymentPort
import devcoop.occount.payment.domain.exception.InvalidPaymentRequestException
import devcoop.occount.payment.domain.exception.PaymentFailedException
import devcoop.occount.payment.domain.exception.PaymentTimeoutException
import devcoop.occount.payment.domain.exception.TransactionInProgressException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class PgCardPaymentClient(
    @param:Qualifier("pgClient") private val paymentRestClient: RestClient,
) : CardPaymentPort {
    private val log = LoggerFactory.getLogger(PgCardPaymentClient::class.java)

    override fun approve(amount: Int, products: List<ProductInfo>): PgResponse {
        val request = PgRequest(amount = amount, products = products)
        log.info("카드결제 요청 - 금액: {}원, 상품 수: {}개", amount, products.size)

        return try {
            val response = paymentRestClient.post()
                .uri("/api/payment")
                .body(request)
                .retrieve()
                .onStatus({ status -> status.value() == 400 }) { _, _ ->
                    throw InvalidPaymentRequestException()
                }
                .onStatus({ status -> status.value() == 409 }) { _, _ ->
                    log.warn("진행 중인 거래 감지됨")
                    throw TransactionInProgressException()
                }
                .onStatus({ status -> status.value() == 408 }) { _, _ ->
                    log.error("결제 요청 타임아웃 발생")
                    throw PaymentTimeoutException()
                }
                .body(PgResponse::class.java)

            if (response != null) {
                log.info(
                    "카드 결제 응답 - 성공 여부: {}, 메시지: {}, 거래 ID: {}, 승인번호: {}",
                    response.success,
                    response.message,
                    response.transaction?.transactionId ?: "N/A",
                    response.transaction?.approvalNumber ?: "N/A",
                )
                response
            } else {
                throw PaymentFailedException()
            }
        } catch (e: Exception) {
            when (e) {
                is InvalidPaymentRequestException,
                is TransactionInProgressException,
                is PaymentTimeoutException,
                -> throw e

                else -> {
                    log.error("카드결제 처리 중 오류 발생: ${e.message}", e)
                    throw PaymentFailedException()
                }
            }
        }
    }
}
