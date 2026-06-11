package devcoop.occount.payment.api.wallet

import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.AuthUser
import devcoop.occount.payment.api.dto.response.WalletPointResponse
import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/wallet")
class WalletController(
    private val getWalletPointQueryService: GetWalletPointQueryService,
) {
    @GetMapping("/point")
    fun getBalance(@AuthUser principal: AuthPrincipal): WalletPointResponse {
        return WalletPointResponse(getWalletPointQueryService.getPoint(principal.userId))
    }
}
