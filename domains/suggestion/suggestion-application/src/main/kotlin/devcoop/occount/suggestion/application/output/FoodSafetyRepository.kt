package devcoop.occount.suggestion.application.output

data class FoodSafetySearchItem(
    val typeNSeq: Long,
    val name: String,
    val company: String,
    val kcalInfo: String,
)

data class FoodSafetyProductDetail(
    val typeNSeq: Long,
    val name: String,
    val isAllowed: Boolean,
)

interface FoodSafetyRepository {
    fun search(keyword: String): List<FoodSafetySearchItem>
    fun getDetail(typeNSeq: Long): FoodSafetyProductDetail?
}
