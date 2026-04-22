package devcoop.occount.observability.config

import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter
import io.opentelemetry.sdk.trace.export.SpanExporter
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LegacyOtlpTracingProperties::class)
@ConditionalOnClass(OtlpHttpSpanExporter::class)
class ObservabilityTracingConfiguration {
    @Bean
    @ConditionalOnMissingBean(SpanExporter::class)
    @ConditionalOnProperty(prefix = "management.otlp.tracing", name = ["endpoint"])
    fun otlpHttpSpanExporter(
        properties: LegacyOtlpTracingProperties,
    ): SpanExporter {
        val builder = OtlpHttpSpanExporter.builder()
        properties.endpoint?.let(builder::setEndpoint)
        properties.connectTimeout?.let(builder::setConnectTimeout)
        properties.timeout?.let(builder::setTimeout)
        properties.compression?.let(builder::setCompression)
        properties.headers.forEach(builder::addHeader)
        return builder.build()
    }
}
