package devcoop.occount.suggestion.application.query

data class AripickStatsResponse(
    val totalProposals: Long,
    val approved: Long,
    val pending: Long,
    val rejected: Long,
)
