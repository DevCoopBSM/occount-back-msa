package devcoop.occount.payment.application.usecase.wallet.admincharge

data class AdminChargeResult(
    val results: List<AdminChargeItemResult>,
) {
    val chargedCount: Int get() = results.count { it.status == AdminChargeStatus.CHARGED }
    val failedCount: Int get() = results.count { it.status == AdminChargeStatus.FAILED }
}

data class AdminChargeItemResult(
    val userId: Long,
    val status: AdminChargeStatus,
    val message: String? = null,
) {
    companion object {
        fun charged(userId: Long): AdminChargeItemResult =
            AdminChargeItemResult(userId, AdminChargeStatus.CHARGED)

        fun failed(userId: Long, message: String): AdminChargeItemResult =
            AdminChargeItemResult(userId, AdminChargeStatus.FAILED, message)
    }
}

enum class AdminChargeStatus {
    CHARGED,
    FAILED,
}
