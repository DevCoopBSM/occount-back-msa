package devcoop.occount.suggestion.infrastructure.external.foodsafety

import devcoop.occount.suggestion.application.output.FoodSafetyProductDetail
import devcoop.occount.suggestion.application.output.FoodSafetyRepository
import devcoop.occount.suggestion.application.output.FoodSafetySearchItem
import devcoop.occount.suggestion.domain.aripick.AripickFoodSafetyUnavailableException
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

@Repository
class FoodSafetyRepositoryImpl(
    @param:Value("\${app.food-safety.base-url}") private val baseUrl: String,
) : FoodSafetyRepository {
    private val log = LoggerFactory.getLogger(FoodSafetyRepositoryImpl::class.java)

    private val client = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(3))
        .build()

    override fun search(keyword: String): List<FoodSafetySearchItem> {
        val encoded = URLEncoder.encode(keyword, StandardCharsets.UTF_8)
        val html = requestHtml(
            "${baseUrl.trimEnd('/')}/hilow/qfood/sfoodlist.do?searchValue=$encoded",
        )

        return parseSearchHtml(html)
    }

    override fun getDetail(typeNSeq: Long): FoodSafetyProductDetail? {
        val html = requestHtml(
            "${baseUrl.trimEnd('/')}/hilow/qfood/sfoodview.do?typenseq=$typeNSeq",
        )

        return parseDetailHtml(typeNSeq, html)
    }

    private fun requestHtml(url: String): String {
        return try {
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build()

            val response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            if (response.statusCode() in 200..299) {
                response.body()
            } else {
                log.warn("FoodSafety request failed. status={}, url={}", response.statusCode(), url)
                throw AripickFoodSafetyUnavailableException()
            }
        } catch (e: AripickFoodSafetyUnavailableException) {
            throw e
        } catch (e: Exception) {
            log.warn("FoodSafety request exception. url={}, message={}", url, e.message, e)
            throw AripickFoodSafetyUnavailableException()
        }
    }

}

internal fun parseSearchHtml(html: String): List<FoodSafetySearchItem> {
    val linkPattern = Regex(
        "<a\\s+href=\"sfoodview\\.do\\?typenseq=(\\d+)\"[^>]*>(.*?)</a>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    val spanPattern = Regex(
        "<span(?:\\s+class=\"([^\"]+)\")?>(.*?)</span>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )

    return linkPattern.findAll(html).mapNotNull { match ->
        val typeNSeq = match.groupValues[1].toLongOrNull() ?: return@mapNotNull null
        val content = match.groupValues[2]

        val spans = spanPattern.findAll(content).map { span ->
            val clazz = span.groupValues[1]
            val text = stripTag(span.groupValues[2])
            clazz to text
        }.toList()

        val name = spans.firstOrNull { it.first == "company" }?.second ?: return@mapNotNull null
        val company = spans.firstOrNull { it.first.isEmpty() }?.second ?: ""
        val kcalInfo = spans.firstOrNull { it.first == "kcal" }?.second ?: ""

        FoodSafetySearchItem(
            typeNSeq = typeNSeq,
            name = name,
            company = company,
            kcalInfo = kcalInfo,
        )
    }.toList()
}

internal fun parseDetailHtml(
    typeNSeq: Long,
    html: String,
): FoodSafetyProductDetail? {
    val titlePattern = Regex(
        "<strong\\s+class=\"text_point\\s+mt20\">(.*?)</strong>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    )
    val rawTitle = titlePattern.find(html)?.groupValues?.get(1)
    val productName = rawTitle
        ?.let(::stripTag)
        ?.substringBefore("/")
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: return null

    val allowed = Regex(
        "<p\\s+class=\"text_box\\s+mt20\"[^>]*>\\s*<b>\\s*고열량ㆍ저영양\\s*식품\\s*</b>\\s*이\\s*아닙니다\\.?\\s*</p>",
        setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE),
    ).containsMatchIn(html)

    return FoodSafetyProductDetail(
        typeNSeq = typeNSeq,
        name = productName,
        isAllowed = allowed,
    )
}

internal fun stripTag(raw: String): String {
    return raw
        .replace(Regex("<[^>]*>"), "")
        .replace("&nbsp;", " ")
        .trim()
}
