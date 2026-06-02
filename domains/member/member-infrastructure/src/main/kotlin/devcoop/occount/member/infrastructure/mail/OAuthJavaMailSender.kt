package devcoop.occount.member.infrastructure.mail

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

class OAuthJavaMailSender(
    private val email: String,
    private val clientId: String,
    private val clientSecret: String,
    private val refreshToken: String,
    private val objectMapper: ObjectMapper,
) : JavaMailSenderImpl() {

    private val logger = LoggerFactory.getLogger(OAuthJavaMailSender::class.java)
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build()

    private var cachedToken: String? = null
    private var tokenExpiresAt: Instant = Instant.EPOCH

    init {
        host = "smtp.gmail.com"
        port = 587
        username = email
        val props = javaMailProperties
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.auth.mechanisms"] = "XOAUTH2"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.ssl.trust"] = "smtp.gmail.com"
    }

    override fun doSend(mimeMessages: Array<out MimeMessage>, originalMessages: Array<out Any>?) {
        password = fetchAccessToken()
        super.doSend(mimeMessages, originalMessages)
    }

    private fun fetchAccessToken(): String {
        val body = "grant_type=refresh_token" +
            "&client_id=${clientId}" +
            "&client_secret=${clientSecret}" +
            "&refresh_token=${refreshToken}"

        val request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(30))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            logger.debug("OAuth2 token refresh failed body: {}", response.body())
            error("Google OAuth2 token refresh failed with status ${response.statusCode()}")
        }
        val parsed = objectMapper.readValue<Map<String, Any>>(response.body())

        return parsed["access_token"] as? String
            ?: error("Google OAuth2 token refresh failed: access_token not found in response")
    }

    companion object {
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    }
}
