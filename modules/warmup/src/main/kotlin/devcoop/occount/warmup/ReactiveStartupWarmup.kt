package devcoop.occount.warmup

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext
import org.springframework.context.event.EventListener
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.time.Duration
import kotlin.system.measureTimeMillis

@Component
@ConditionalOnClass(WebClient::class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnProperty(
    prefix = "app.startup-warmup",
    name = ["enabled", "http-enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class ReactiveStartupWarmup(
    private val applicationContext: ReactiveWebServerApplicationContext,
    private val environment: Environment,
    private val properties: StartupWarmupProperties,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun warmup() {
        val port = applicationContext.webServer?.port ?: -1
        if (port <= 0) {
            log.info("Reactive startup warmup skipped (no active web server port)")
            return
        }

        val rounds = properties.httpRepeat.coerceAtLeast(1)
        val endpoints = properties.httpEndpoints
            .takeIf { it.isNotEmpty() }
            ?: listOf(HttpWarmupTargets.defaultEndpoint(environment, properties))

        val baseUri = HttpWarmupTargets.resolveBase(port, environment)
        val client = WebClient.builder().baseUrl(baseUri).build()
        val timeout = properties.httpTimeout

        val elapsed = measureTimeMillis {
            repeat(rounds) { round ->
                for (endpoint in endpoints) {
                    runCatching {
                        sendOne(client, endpoint, timeout).block(timeout)
                    }.onSuccess { status ->
                        if (status != null && status >= 500) {
                            log.warn(
                                "Reactive startup warmup [{}/{}] returned status {} for {} {}",
                                round + 1, rounds, status, endpoint.method, endpoint.path,
                            )
                        }
                    }.onFailure { exception ->
                        log.warn(
                            "Reactive startup warmup [{}/{}] failed for {} {}",
                            round + 1, rounds, endpoint.method, endpoint.path, exception,
                        )
                    }
                }
            }
        }

        log.info(
            "Reactive startup warmup completed ({} rounds, {} endpoints) in {} ms",
            rounds, endpoints.size, elapsed,
        )
    }

    private fun sendOne(client: WebClient, endpoint: WarmupEndpoint, timeout: Duration): Mono<Int> {
        val method = HttpMethod.valueOf(endpoint.method.uppercase())
        var request = client.method(method).uri(endpoint.path)
        endpoint.headers.forEach { (name, value) -> request = request.header(name, value) }
        val withBody = if (method == HttpMethod.POST || method == HttpMethod.PUT || method == HttpMethod.PATCH) {
            request.header("Content-Type", endpoint.contentType)
                .bodyValue(endpoint.body.orEmpty())
        } else {
            request
        }

        return withBody.exchangeToMono { Mono.just(it.statusCode().value()) }
            .timeout(timeout)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ReactiveStartupWarmup::class.java)
    }
}
