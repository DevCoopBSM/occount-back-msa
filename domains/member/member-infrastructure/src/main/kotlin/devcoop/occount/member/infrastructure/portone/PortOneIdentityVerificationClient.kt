package devcoop.occount.member.infrastructure.portone

import devcoop.occount.member.application.exception.IdentityVerificationFailedException
import devcoop.occount.member.application.output.IdentityVerificationClient
import devcoop.occount.member.application.output.VerifiedIdentity
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

private data class PortOneVerificationResponse(
    val status: String?,
    val verifiedCustomer: VerifiedCustomer?,
) {
    data class VerifiedCustomer(
        val ci: String?,
        val name: String?,
        val phoneNumber: String?,
    )
}

@Component
@EnableConfigurationProperties(PortOneProperties::class)
class PortOneIdentityVerificationClient(
    private val properties: PortOneProperties,
    private val objectMapper: ObjectMapper,
) : IdentityVerificationClient {

    private val httpClient: HttpClient = HttpClient.newHttpClient()

    override fun verify(identityVerificationId: String): VerifiedIdentity {
        val request = HttpRequest.newBuilder(URI.create("$PORTONE_API_URL/$identityVerificationId"))
            .header("Authorization", "PortOne ${properties.secretKey}")
            .header("Content-Type", "application/json")
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

        val body = objectMapper.readValue<PortOneVerificationResponse>(response.body())

        if (body.status != "VERIFIED") {
            log.warn("본인인증 미완료 상태 - status={}", body.status)
            throw IdentityVerificationFailedException()
        }

        val customer = body.verifiedCustomer
        val ciNumber = customer?.ci.orEmpty()
        val username = customer?.name.orEmpty()
        val phone = customer?.phoneNumber.orEmpty()

        if (ciNumber.isBlank() || username.isBlank()) {
            log.error("PortOne 응답에 필수 값 누락 - identityVerificationId={}", identityVerificationId)
            throw IdentityVerificationFailedException()
        }

        return VerifiedIdentity(
            ciNumber = ciNumber,
            username = username,
            phone = phone,
        )
    }

    companion object {
        private const val PORTONE_API_URL = "https://api.portone.io/identity-verifications"
        private val log = LoggerFactory.getLogger(PortOneIdentityVerificationClient::class.java)
    }
}
