package devcoop.occount.suggestion.application.query

import devcoop.occount.suggestion.application.output.FoodSafetyRepository
import org.springframework.stereotype.Service

@Service
class AripickFoodQueryService(
    private val foodSafetyRepository: FoodSafetyRepository,
) {
    fun search(keyword: String): AripickFoodSearchResponse {
        val items = foodSafetyRepository.search(keyword).map {
            AripickFoodSearchItemResponse(
                typeNSeq = it.typeNSeq,
                name = it.name,
                company = it.company,
                kcalInfo = it.kcalInfo,
            )
        }
        return AripickFoodSearchResponse(items)
    }
}
