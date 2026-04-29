package devcoop.occount.suggestion.infrastructure.external.foodsafety

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FoodSafetyHtmlParserTest {
    @Test
    fun `search html extracts typeNSeq, name, company and kcal`() {
        val html = """
            <div class="food_link mt20">
              <a href="sfoodview.do?typenseq=14116"><span class="company">신라면 큰사발면</span><span>㈜농심</span><span class="kcal">347.37kcal / 114g</span></a>
              <a href="sfoodview.do?typenseq=14308"><span class="company">신라면더레드큰사발면</span><span>농심</span><span class="kcal">355.56kcal / 117g</span></a>
            </div>
        """.trimIndent()

        val result = parseSearchHtml(html)

        assertEquals(2, result.size)
        assertEquals(14116L, result[0].typeNSeq)
        assertEquals("신라면 큰사발면", result[0].name)
        assertEquals("㈜농심", result[0].company)
        assertEquals("347.37kcal / 114g", result[0].kcalInfo)
    }

    @Test
    fun `detail html with not-high-calorie message is allowed`() {
        val html = """
            <p class="text_box mt20"><b>고열량ㆍ저영양 식품</b>이 아닙니다.</p>
            <strong class="text_point mt20">뮤직기가바이트스트로베리향(MUZICGIGABITESTRAWBERRY) / 일화/MUNCHY FOOD INDUSTRIES SDN BHD</strong>
        """.trimIndent()

        val result = parseDetailHtml(243L, html)

        assertNotNull(result)
        assertTrue(result!!.isAllowed)
        assertEquals("뮤직기가바이트스트로베리향(MUZICGIGABITESTRAWBERRY)", result.name)
    }

    @Test
    fun `detail html with high-calorie message is rejected`() {
        val html = """
            <p class="text_box mt20" style="background: #ff9a9a;"><b>고열량ㆍ저영양 식품</b>입니다.</p>
            <strong class="text_point mt20">신라면 큰사발면 / ㈜농심</strong>
        """.trimIndent()

        val result = parseDetailHtml(14116L, html)

        assertNotNull(result)
        assertFalse(result!!.isAllowed)
        assertEquals("신라면 큰사발면", result.name)
    }
}
