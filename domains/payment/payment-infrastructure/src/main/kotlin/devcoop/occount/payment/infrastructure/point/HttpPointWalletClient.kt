package devcoop.occount.payment.infrastructure.point

import devcoop.occount.payment.application.payment.PointWalletPort
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class HttpPointWalletClient(
    @param:Qualifier("pointApiRestClient") private val pointApiRestClient: RestClient,
) : PointWalletPort {
    override fun getBalance(userId: Long): Int {
        return pointApiRestClient.get()
            .uri("/points/{userId}/balance", userId)
            .retrieve()
            .body(PointBalanceReadResponse::class.java)
            ?.balance
            ?: 0
    }

    override fun charge(userId: Long, amount: Int): Int {
        return sendCommand("/points/{userId}/charge", userId, amount)
    }

    override fun deduct(userId: Long, amount: Int): Int {
        return sendCommand("/points/{userId}/deduct", userId, amount)
    }

    private fun sendCommand(path: String, userId: Long, amount: Int): Int {
        return pointApiRestClient.post()
            .uri(path, userId)
            .body(PointAmountRequest(amount))
            .retrieve()
            .body(PointBalanceReadResponse::class.java)
            ?.balance
            ?: 0
    }
}
