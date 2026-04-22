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

@Component
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(prefix = "app.startup-warmup", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@ConditionalOnProperty(prefix = "app.startup-warmup", name = ["servlet-enabled"], havingValue = "true", matchIfMissing = true)
class ServletStartupWarmup(
    private val applicationContext: ServletWebServerApplicationContext,
    private val environment: Environment,
    private val properties: StartupWarmupProperties,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun warmup() {
        val port = applicationContext.webServer?.port ?: return
        if (port <= 0) {
            return
        }

        val target = ServletWarmupTarget.resolve(port, environment, properties)
        val client = HttpClient.newBuilder()
            .connectTimeout(properties.servletTimeout)
            .build()
        val request = HttpRequest.newBuilder(target)
            .GET()
            .timeout(properties.servletTimeout)
            .build()

        runCatching {
            client.send(request, HttpResponse.BodyHandlers.discarding())
        }.onSuccess { response ->
            if (response.statusCode() >= 500) {
                log.warn("Servlet startup warmup returned status {} for {}", response.statusCode(), target)
                return@onSuccess
            }

            log.info("Servlet startup warmup completed with status {} for {}", response.statusCode(), target)
        }.onFailure { exception ->
            log.warn("Servlet startup warmup failed for {}", target, exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(ServletStartupWarmup::class.java)
    }
}

internal object ServletWarmupTarget {
    fun resolve(
        port: Int,
        environment: Environment,
        properties: StartupWarmupProperties,
    ): URI {
        val scheme = if (environment.getProperty("server.ssl.enabled", Boolean::class.java, false)) "https" else "http"
        val configuredPath = properties.servletPath
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalizePath)
        val path = configuredPath ?: defaultPath(environment)
        return URI.create("$scheme://127.0.0.1:$port$path")
    }

    private fun defaultPath(environment: Environment): String {
        val contextPath = normalizePath(environment.getProperty("server.servlet.context-path"))
        val actuatorBasePath = normalizePath(environment.getProperty("management.endpoints.web.base-path") ?: "/actuator")
        return "$contextPath$actuatorBasePath/health"
    }

    private fun normalizePath(path: String?): String {
        val value = path?.trim().orEmpty()
        if (value.isEmpty() || value == "/") {
            return ""
        }

        return (if (value.startsWith("/")) value else "/$value").removeSuffix("/")
    }
}
