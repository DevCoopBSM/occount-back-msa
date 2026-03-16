package devcoop.occount.core.common.error

data class ErrorResponse(
    val message: String,
) {
    companion object {
        fun of(errorMessage: ErrorMessage): ErrorResponse {
            return ErrorResponse(
                message = errorMessage.message
            )
        }
    }
}
