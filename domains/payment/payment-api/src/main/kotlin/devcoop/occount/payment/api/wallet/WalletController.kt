package devcoop.occount.payment.api.wallet

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.payment.api.dto.response.WalletPointResponse
import devcoop.occount.payment.application.query.chargelog.ChargeLogResult
import devcoop.occount.payment.application.query.chargelog.GetChargeHistoryQueryService
import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/wallet")
class WalletController(
    private val getWalletPointQueryService: GetWalletPointQueryService,
    private val getChargeHistoryQueryService: GetChargeHistoryQueryService,
) {
    @GetMapping("/point")
    fun getPoint(httpRequest: HttpServletRequest): WalletPointResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return WalletPointResponse(getWalletPointQueryService.getPoint(userId))
    }

    @GetMapping("/charges")
    @ResponseStatus(HttpStatus.OK)
    fun getChargeHistory(httpRequest: HttpServletRequest): List<ChargeLogResult> {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getChargeHistoryQueryService.getChargeHistory(userId)
    }
}
