package devcoop.occount.member.application.query

data class AdminUserListResponse(
    val users: List<AdminUserSummaryResponse>,
    val totalCount: Long,
    val totalPages: Int,
    val currentPage: Int,
    val pageSize: Int,
)
