package devcoop.occount.member.application.query

data class PageMeta(
    val page: Int,
    val size: Int,
    val totalCount: Int,
    val totalPages: Int,
)
