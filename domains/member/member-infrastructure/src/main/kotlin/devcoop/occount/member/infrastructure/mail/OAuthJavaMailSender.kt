package devcoop.occount.member.infrastructure.mail

import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSenderImpl
import java.net.InetSocketAddress
import java.net.ProxySelector
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
) : JavaMailSenderImpl() {

    private val logger = LoggerFactory.getLogger(OAuthJavaMailSender::class.java)
    private val httpClient: HttpClient = buildHttpClient()

    private fun buildHttpClient(): HttpClient {
        val builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60))
        val proxyHost = System.getenv("HTTPS_PROXY_HOST") ?: System.getProperty("https.proxyHost")
        val proxyPort = (System.getenv("HTTPS_PROXY_PORT") ?: System.getProperty("https.proxyPort"))?.toIntOrNull()
        if (proxyHost != null && proxyPort != null) {
            builder.proxy(ProxySelector.of(InetSocketAddress(proxyHost, proxyPort)))
        }
        return builder.build()
    }

    @Volatile private var cachedToken: String? = null
    @Volatile private var tokenExpiresAt: Instant = Instant.EPOCH

    init {
        host = "smtp.gmail.com"
        port = 587
        username = email
        val props = javaMailProperties
        props["mail.smtp.auth"] = "true"
        props["mail.smtp.auth.mechanisms"] = "XOAUTH2"
        props["mail.smtp.starttls.enable"] = "true"
        props["mail.smtp.ssl.trust"] = "smtp.gmail.com"
        val proxyHost = System.getenv("HTTPS_PROXY_HOST") ?: System.getProperty("https.proxyHost")
        val proxyPort = System.getenv("HTTPS_PROXY_PORT") ?: System.getProperty("https.proxyPort")
        if (proxyHost != null && proxyPort != null) {
            props["mail.smtp.proxy.host"] = proxyHost
            props["mail.smtp.proxy.port"] = proxyPort
        }
    }

    override fun doSend(mimeMessages: Array<out MimeMessage>, originalMessages: Array<out Any>?) {
        password = getAccessToken()
        super.doSend(mimeMessages, originalMessages)
    }

    @Synchronized
    private fun getAccessToken(): String {
        val token = cachedToken
        if (token != null && Instant.now() < tokenExpiresAt.minus(Duration.ofMinutes(1))) {
            return token
        }
        return fetchAccessToken()
    }

    private fun fetchAccessToken(): String {
        val requestBody = "grant_type=refresh_token" +
            "&client_id=${clientId}" +
            "&client_secret=${clientSecret}" +
            "&refresh_token=${refreshToken}"

        val request = HttpRequest.newBuilder(URI.create(TOKEN_ENDPOINT))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .timeout(Duration.ofSeconds(30))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            logger.debug("OAuth2 token refresh failed body: {}", response.body())
            error("Google OAuth2 token refresh failed with status ${response.statusCode()}")
        }

        val body = response.body()
        val accessToken = Regex(""""access_token"\s*:\s*"([^"]+)"""").find(body)?.groupValues?.get(1)
            ?: error("Google OAuth2 token refresh failed: access_token not found in response")
        val expiresIn = Regex(""""expires_in"\s*:\s*(\d+)""").find(body)?.groupValues?.get(1)?.toLongOrNull() ?: 3600L
        cachedToken = accessToken
        tokenExpiresAt = Instant.now().plusSeconds(expiresIn)
        return accessToken
    }

    companion object {
        private const val TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token"
    }
}
