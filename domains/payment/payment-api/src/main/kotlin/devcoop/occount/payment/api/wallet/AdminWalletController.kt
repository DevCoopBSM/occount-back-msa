package devcoop.occount.payment.api.wallet

import devcoop.occount.core.common.auth.AuthPrincipal
import devcoop.occount.core.common.auth.AuthUser
import devcoop.occount.payment.api.dto.request.AdminChargeWalletRequest
import devcoop.occount.payment.api.dto.response.AdminChargeWalletResponse
import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeCommand
import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeWalletUseCase
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/wallet")
class AdminWalletController(
    private val adminChargeWalletUseCase: AdminChargeWalletUseCase,
) {
    @PostMapping("/admin/charge")
    fun chargeWallets(
        @AuthUser principal: AuthPrincipal,
        @Valid @RequestBody request: AdminChargeWalletRequest,
    ): AdminChargeWalletResponse {
        val result = adminChargeWalletUseCase.charge(
            AdminChargeCommand(
                adminId = principal.userId,
                userIds = request.userIds,
                amount = request.amount,
                reason = request.reason,
            ),
        )
        return AdminChargeWalletResponse.from(result)
    }
}
