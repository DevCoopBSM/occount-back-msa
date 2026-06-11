package devcoop.occount.payment.api.dto.response

import devcoop.occount.payment.application.usecase.wallet.admincharge.AdminChargeResult

data class AdminChargeWalletResponse(
    val chargedCount: Int,
    val failedCount: Int,
    val results: List<AdminChargeItemResponse>,
) {
    companion object {
        fun from(result: AdminChargeResult): AdminChargeWalletResponse =
            AdminChargeWalletResponse(
                chargedCount = result.chargedCount,
                failedCount = result.failedCount,
                results = result.results.map {
                    AdminChargeItemResponse(
                        userId = it.userId,
                        status = it.status.name,
                        message = it.message,
                    )
                },
            )
    }
}

data class AdminChargeItemResponse(
    val userId: Long,
    val status: String,
    val message: String?,
)
