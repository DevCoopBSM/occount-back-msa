package devcoop.occount.investment.infrastructure.client

import devcoop.occount.investment.application.exception.PortOnePaymentLookupException
import devcoop.occount.investment.application.output.PortOnePayment
import devcoop.occount.investment.application.output.PortOnePaymentPort
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/**
 * PortOne 결제 단건 조회 어댑터.
 * 응답은 키 그대로 Map 으로 파싱해(전역 snake_case 전략의 영향을 받지 않도록) status/amount.total 만 취한다.
 */
@Component
@EnableConfigurationProperties(PortOneProperties::class)
class PortOnePaymentClient(
    private val properties: PortOneProperties,
    private val objectMapper: ObjectMapper,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build(),
    private val apiBaseUrl: String = PORTONE_API_URL,
) : PortOnePaymentPort {

    override fun fetchPayment(paymentId: String): PortOnePayment {
        val request = HttpRequest.newBuilder(URI.create("$apiBaseUrl/$paymentId"))
            .header("Authorization", "PortOne ${properties.secretKey}")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.error("PortOne 결제 조회 호출 실패 - paymentId={}", paymentId, e)
            throw PortOnePaymentLookupException()
        }

        if (response.statusCode() !in 200..299) {
            log.error("PortOne 결제 조회 오류 응답 - status={}, paymentId={}", response.statusCode(), paymentId)
            throw PortOnePaymentLookupException()
        }

        return parse(response.body(), paymentId)
    }

    private fun parse(body: String, paymentId: String): PortOnePayment {
        return try {
            val root = objectMapper.readValue(body, Map::class.java)
            val status = root["status"] as? String ?: throw PortOnePaymentLookupException()
            val amountNode = root["amount"] as? Map<*, *> ?: throw PortOnePaymentLookupException()
            val total = (amountNode["total"] as? Number)?.toInt() ?: throw PortOnePaymentLookupException()
            PortOnePayment(status = status, amount = total)
        } catch (e: PortOnePaymentLookupException) {
            throw e
        } catch (e: Exception) {
            log.error("PortOne 결제 응답 파싱 실패 - paymentId={}", paymentId, e)
            throw PortOnePaymentLookupException()
        }
    }

    companion object {
        private const val PORTONE_API_URL = "https://api.portone.io/payments"
        private val log = LoggerFactory.getLogger(PortOnePaymentClient::class.java)
    }
}
