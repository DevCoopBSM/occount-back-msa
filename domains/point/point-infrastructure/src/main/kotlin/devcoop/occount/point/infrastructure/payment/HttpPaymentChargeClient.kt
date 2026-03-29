package devcoop.occount.point.infrastructure.payment

import devcoop.occount.core.common.auth.AuthHeaders
import devcoop.occount.point.application.output.ChargePaymentPort
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class HttpPaymentChargeClient(
    @param:Qualifier("paymentApiRestClient") private val paymentApiRestClient: RestClient,
) : ChargePaymentPort {

    override fun processCharge(userId: Long, amount: Int): Long {
        val response = paymentApiRestClient.post()
            .uri("/payments/charge")
            .header(AuthHeaders.AUTHENTICATED_USER_ID, userId.toString())
            .body(ChargeRequest(amount))
            .retrieve()
            .body<ChargeResponse>()

        return response?.paymentLogId
            ?: throw IllegalStateException("Payment charge failed: paymentLogId not returned")
    }
}

private data class ChargeRequest(val amount: Int)

private data class ChargeResponse(val paymentLogId: Long?)
