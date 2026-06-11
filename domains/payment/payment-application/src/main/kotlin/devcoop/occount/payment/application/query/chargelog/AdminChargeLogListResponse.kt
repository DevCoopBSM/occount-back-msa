package devcoop.occount.payment.application.query.chargelog

data class AdminChargeLogListResponse(
    val charges: List<AdminChargeLogResult>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)
