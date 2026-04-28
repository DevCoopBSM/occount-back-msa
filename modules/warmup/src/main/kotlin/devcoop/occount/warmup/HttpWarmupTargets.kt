package devcoop.occount.warmup

import org.springframework.core.env.Environment

internal object HttpWarmupTargets {
    fun resolveBase(port: Int, environment: Environment): String {
        val scheme = if (environment.getProperty("server.ssl.enabled", Boolean::class.java, false)) "https" else "http"
        return "$scheme://127.0.0.1:$port"
    }

    fun defaultEndpoint(environment: Environment, properties: StartupWarmupProperties): WarmupEndpoint {
        val configuredPath = properties.httpPath
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalizePath)
        val path = configuredPath ?: defaultActuatorPath(environment)
        return WarmupEndpoint(path = path, method = "GET")
    }

    private fun defaultActuatorPath(environment: Environment): String {
        val contextPath = normalizePath(environment.getProperty("server.servlet.context-path"))
        val actuatorBasePath = normalizePath(environment.getProperty("management.endpoints.web.base-path") ?: "/actuator")
        return "$contextPath$actuatorBasePath/health/ping"
    }

    private fun normalizePath(path: String?): String {
        val value = path?.trim().orEmpty()
        if (value.isEmpty() || value == "/") {
            return ""
        }

        return (if (value.startsWith("/")) value else "/$value").removeSuffix("/")
    }
}