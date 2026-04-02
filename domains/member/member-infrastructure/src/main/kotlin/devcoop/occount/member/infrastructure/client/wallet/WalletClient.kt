package devcoop.occount.member.infrastructure.client.wallet

import devcoop.occount.core.common.auth.AuthHeaders
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.service.annotation.GetExchange
import org.springframework.web.service.annotation.HttpExchange

@HttpExchange
interface WalletClient {
    @GetExchange("/wallet/point")
    fun getPoint(
        @RequestHeader(AuthHeaders.AUTHENTICATED_USER_ID) userId: String,
    ): WalletPointClientResponse
}
