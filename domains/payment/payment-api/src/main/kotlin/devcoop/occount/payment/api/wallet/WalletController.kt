package devcoop.occount.payment.api.wallet

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.payment.api.dto.request.ChargeWalletCommand
import devcoop.occount.payment.application.query.chargelog.ChargeLogResult
import devcoop.occount.payment.application.query.chargelog.GetChargeHistoryQueryService
import devcoop.occount.payment.application.query.wallet.GetWalletBalanceQueryService
import devcoop.occount.payment.application.query.wallet.WalletBalanceResponse
import devcoop.occount.payment.application.usecase.wallet.ChargeWalletRequest
import devcoop.occount.payment.application.usecase.wallet.ChargeWalletUseCase
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/wallet")
class WalletController(
    private val getWalletBalanceQueryService: GetWalletBalanceQueryService,
    private val chargeWalletUseCase: ChargeWalletUseCase,
    private val getChargeHistoryQueryService: GetChargeHistoryQueryService,
) {
    @GetMapping("/balance")
    fun getBalance(httpRequest: HttpServletRequest): WalletBalanceResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getWalletBalanceQueryService.getBalance(userId)
    }

    @PostMapping("/charge")
    @ResponseStatus(HttpStatus.OK)
    fun charge(
        @Valid @RequestBody request: ChargeWalletCommand,
        httpServletRequest: HttpServletRequest,
    ) {
        val userId = RequestAuthPrincipalResolver.resolve(httpServletRequest).userId
        chargeWalletUseCase.charge(ChargeWalletRequest(userId, request.amount))
    }

    @GetMapping("/charges")
    @ResponseStatus(HttpStatus.OK)
    fun getChargeHistory(httpRequest: HttpServletRequest): List<ChargeLogResult> {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return getChargeHistoryQueryService.getChargeHistory(userId)
    }
}
