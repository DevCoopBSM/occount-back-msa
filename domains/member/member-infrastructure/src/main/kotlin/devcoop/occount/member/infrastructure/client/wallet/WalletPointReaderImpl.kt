package devcoop.occount.member.infrastructure.client.wallet

import devcoop.occount.member.application.exception.PointNotFoundException
import devcoop.occount.member.application.output.WalletPointReader
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Repository
import org.springframework.web.client.RestClientResponseException

@Repository
class WalletPointReaderImpl(
    private val walletClient: WalletClient,
) : WalletPointReader {
    override fun getPoint(userId: Long): Int {
        return try {
            walletClient.getPoint(userId.toString()).point
        } catch (e: RestClientResponseException) {
            if (e.statusCode == HttpStatus.NOT_FOUND) {
                throw PointNotFoundException()
            }
            throw e
        }
    }
}
