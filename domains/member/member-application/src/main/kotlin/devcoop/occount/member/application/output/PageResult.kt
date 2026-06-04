package devcoop.occount.member.application.output

data class PageResult<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalCount: Int,
    val totalPages: Int,
)
