package devcoop.occount.member.infrastructure.portone

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import devcoop.occount.member.application.exception.IdentityNotVerifiedException
import devcoop.occount.member.application.exception.IdentityVerificationFailedException
import devcoop.occount.member.application.output.IdentityVerificationClient
import devcoop.occount.member.application.output.VerifiedIdentity
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDate
import java.time.format.DateTimeParseException

@JsonIgnoreProperties(ignoreUnknown = true)
private data class PortOneVerificationResponse(
    val status: String?,
    val verifiedCustomer: VerifiedCustomer?,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class VerifiedCustomer(
        val ci: String?,
        val name: String?,
        val phoneNumber: String?,
        val birthDate: String?,
    )
}

@Component
@EnableConfigurationProperties(PortOneProperties::class)
class PortOneIdentityVerificationClient(
    private val properties: PortOneProperties,
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build(),
    private val apiBaseUrl: String = PORTONE_API_URL,
) : IdentityVerificationClient {

    private val objectMapper = jacksonObjectMapper()

    override fun verify(identityVerificationId: String): VerifiedIdentity {
        val request = HttpRequest.newBuilder(URI.create("$apiBaseUrl/$identityVerificationId"))
            .header("Authorization", "PortOne ${properties.secretKey}")
            .header("Content-Type", "application/json")
            .timeout(Duration.ofSeconds(30))
            .GET()
            .build()

        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (e: Exception) {
            log.error("PortOne API 호출 실패 - identityVerificationId={}", identityVerificationId, e)
            throw IdentityVerificationFailedException()
        }

        if (response.statusCode() !in 200..299) {
            log.error("PortOne API 오류 응답 - status={}, body={}", response.statusCode(), response.body())
            throw IdentityVerificationFailedException()
        }

        val body = try {
            objectMapper.readValue<PortOneVerificationResponse>(response.body())
        } catch (e: Exception) {
            log.error("PortOne 응답 파싱 실패 - identityVerificationId={}", identityVerificationId, e)
            throw IdentityVerificationFailedException()
        }

        if (body.status != "VERIFIED") {
            log.warn("본인인증 미완료 상태 - status={}", body.status)
            throw IdentityNotVerifiedException()
        }

        val customer = body.verifiedCustomer
        val ciNumber = customer?.ci.orEmpty()
        val username = customer?.name.orEmpty()
        val phone = customer?.phoneNumber.orEmpty()
        val birthDate = parseBirthDate(customer?.birthDate)

        if (ciNumber.isBlank() || username.isBlank()) {
            log.error("PortOne 응답에 필수 값 누락 - identityVerificationId={}", identityVerificationId)
            throw IdentityVerificationFailedException()
        }

        return VerifiedIdentity(
            ciNumber = ciNumber,
            username = username,
            phone = phone,
            birthDate = birthDate,
        )
    }

    private fun parseBirthDate(rawBirthDate: String?): LocalDate? {
        if (rawBirthDate.isNullOrBlank()) {
            return null
        }
        return try {
            LocalDate.parse(rawBirthDate)
        } catch (e: DateTimeParseException) {
            log.warn("PortOne 생년월일 파싱 실패 - birthDate={}", rawBirthDate, e)
            null
        }
    }

    companion object {
        private const val PORTONE_API_URL = "https://api.portone.io/identity-verifications"
        private val log = LoggerFactory.getLogger(PortOneIdentityVerificationClient::class.java)
    }
}
