package devcoop.occount.payment.infrastructure.member

import devcoop.occount.payment.application.payment.MemberPaymentReadPort
import devcoop.occount.payment.application.payment.PaymentUserInfo
import devcoop.occount.payment.application.payment.PaymentUserNotFoundException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

@Component
class HttpMemberPaymentReadClient(
    @param:Qualifier("memberReadApiRestClient") private val memberReadApiRestClient: RestClient,
) : MemberPaymentReadPort {
    override fun getUser(userId: Long): PaymentUserInfo {
        return try {
            memberReadApiRestClient.get()
                .uri("/users/{userId}/payment-info", userId)
                .retrieve()
                .body(UserPaymentInfoReadResponse::class.java)
                ?.let { response ->
                    PaymentUserInfo(
                        userId = response.userId,
                        email = response.email,
                    )
                }
                ?: throw PaymentUserNotFoundException()
        } catch (e: HttpClientErrorException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw PaymentUserNotFoundException()
            }
            throw e
        }
    }
}
