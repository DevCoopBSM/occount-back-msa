package devcoop.occount.suggestion.application.query

data class AripickFoodSearchResponse(
    val items: List<AripickFoodSearchItemResponse>,
)

data class AripickFoodSearchItemResponse(
    val typeNSeq: Long,
    val name: String,
    val company: String,
    val kcalInfo: String,
)
