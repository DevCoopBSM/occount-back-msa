package devcoop.occount.payment.infrastructure.watchdog

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Component
@ConditionalOnProperty(prefix = "alerts", name = ["webhook-url"])
class DiscordAlertNotifier(
    private val properties: AlertProperties,
    private val objectMapper: ObjectMapper,
) {
    private val client: HttpClient = HttpClient.newHttpClient()

    fun send(message: String) {
        val webhookUrl = properties.webhookUrl ?: return
        val body = objectMapper.writeValueAsString(mapOf("content" to message))
        val request = HttpRequest.newBuilder(URI.create(webhookUrl))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()

        client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept { response ->
                if (response.statusCode() !in 200..299) {
                    log.warn("Discord webhook 응답 오류 - status={}", response.statusCode())
                }
            }
            .exceptionally { ex ->
                log.error("Discord webhook 송신 실패", ex)
                null
            }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DiscordAlertNotifier::class.java)
    }
}
