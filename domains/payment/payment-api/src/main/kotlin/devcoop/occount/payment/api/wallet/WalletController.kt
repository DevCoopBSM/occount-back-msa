package devcoop.occount.payment.api.wallet

import devcoop.occount.core.common.auth.RequestAuthPrincipalResolver
import devcoop.occount.payment.api.dto.request.BulkChargeWalletRequest
import devcoop.occount.payment.api.dto.response.WalletPointResponse
import devcoop.occount.payment.application.query.wallet.GetWalletPointQueryService
import devcoop.occount.payment.application.usecase.wallet.charge.BulkChargeWalletRequest as BulkChargeWalletCommand
import devcoop.occount.payment.application.usecase.wallet.charge.BulkChargeWalletUseCase
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
    private val getWalletPointQueryService: GetWalletPointQueryService,
    private val bulkChargeWalletUseCase: BulkChargeWalletUseCase,
) {
    @GetMapping("/point")
    fun getBalance(httpRequest: HttpServletRequest): WalletPointResponse {
        val userId = RequestAuthPrincipalResolver.resolve(httpRequest).userId
        return WalletPointResponse(getWalletPointQueryService.getPoint(userId))
    }

    @PostMapping("/point/bulk")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun bulkCharge(
        @Valid @RequestBody request: BulkChargeWalletRequest,
    ) {
        bulkChargeWalletUseCase.charge(
            BulkChargeWalletCommand(
                userIds = request.userIds,
                amount = request.amount,
                reason = request.reason,
            ),
        )
    }
}
