package devcoop.occount.suggestion.application.usecase.aripick

data class AripickLikeToggleResponse(
    val proposalId: Long,
    val liked: Boolean,
    val likeCount: Int,
)
