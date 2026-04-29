package devcoop.occount.suggestion.application.query

import devcoop.occount.suggestion.application.output.FoodSafetySearchItem
import devcoop.occount.suggestion.application.support.FakeFoodSafetyRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AripickFoodQueryServiceTest {
    @Test
    fun `search returns extracted food items`() {
        val service = AripickFoodQueryService(
            FakeFoodSafetyRepository(
                searchItems = listOf(
                    FoodSafetySearchItem(
                        typeNSeq = 14116L,
                        name = "신라면 큰사발면",
                        company = "㈜농심",
                        kcalInfo = "347.37kcal / 114g",
                    ),
                ),
            ),
        )

        val result = service.search("신라면")

        assertEquals(1, result.items.size)
        assertEquals(14116L, result.items.first().typeNSeq)
    }
}
