package devcoop.occount.member.infrastructure.mail

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSenderImpl
import tools.jackson.databind.ObjectMapper
import tools.jackson.module.kotlin.readValue
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class OAuthJavaMailSender(
    private val email: String,
    private val clientId: String,
    private val clientSecret: String,
    private val refreshToken: String,
    private val objectMapper: ObjectMapper,
) : JavaMailSenderImpl() {

    private val httpClient: HttpClient = HttpClient.newHttpClient()

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
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val parsed = objectMapper.readValue<Map<String, Any>>(response.body())

        return parsed["access_token"] as? String
            ?: error("Google OAuth2 토큰 갱신 실패: ${response.body()}")
    }

    companion object {
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    }
}
