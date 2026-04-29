package devcoop.occount.suggestion.application.output

interface FoodSafetyRepository {
    fun search(keyword: String): List<FoodSafetySearchItem>
    fun getDetail(typeNSeq: Long): FoodSafetyProductDetail?
}
