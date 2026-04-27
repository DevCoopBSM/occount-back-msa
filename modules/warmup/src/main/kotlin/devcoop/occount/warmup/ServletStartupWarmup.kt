package devcoop.occount.warmup

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.system.measureTimeMillis

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
    prefix = "app.startup-warmup",
    name = ["enabled", "servlet-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class ServletStartupWarmup(
    private val applicationContext: ServletWebServerApplicationContext,
    private val environment: Environment,
    private val properties: StartupWarmupProperties,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun warmup() {
        val port = applicationContext.webServer?.port ?: -1
        if (port <= 0) {
            log.info("Servlet startup warmup skipped (no active web server port)")
            return
        }

        val rounds = properties.servletRepeat.coerceAtLeast(1)
        val endpoints = properties.servletEndpoints
            .takeIf { it.isNotEmpty() }
            ?: listOf(ServletWarmupTargets.defaultEndpoint(environment, properties))

        val baseUri = ServletWarmupTargets.resolveBase(port, environment)
        val client = HttpClient.newBuilder()
            .connectTimeout(properties.servletTimeout)
            .build()
        val targets = endpoints.map { it to URI.create("$baseUri${it.path}") }

        val elapsed = measureTimeMillis {
            repeat(rounds) { round ->
                for ((endpoint, uri) in targets) {
                    val request = buildRequest(endpoint, uri)
                    runCatching {
                        client.send(request, HttpResponse.BodyHandlers.discarding())
                    }.onSuccess { response ->
                        if (response.statusCode() >= 500) {
                            log.warn(
                                "Servlet startup warmup [{}/{}] returned status {} for {} {}",
                                round + 1, rounds, response.statusCode(), endpoint.method, uri,
                            )
                        }
                    }.onFailure { exception ->
                        log.warn(
                            "Servlet startup warmup [{}/{}] failed for {} {}",
                            round + 1, rounds, endpoint.method, uri, exception,
                        )
                    }
                }
            }
        }

        log.info(
            "Servlet startup warmup completed ({} rounds, {} endpoints) in {} ms",
            rounds, endpoints.size, elapsed,
        )
    }

    private fun buildRequest(endpoint: WarmupEndpoint, uri: URI): HttpRequest {
        val builder = HttpRequest.newBuilder(uri).timeout(properties.servletTimeout)
        return when (endpoint.method.uppercase()) {
            "POST" -> builder
                .POST(HttpRequest.BodyPublishers.ofString(endpoint.body.orEmpty()))
                .header("Content-Type", endpoint.contentType)
                .build()
            else -> builder.GET().build()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ServletStartupWarmup::class.java)
    }
}
