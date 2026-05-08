package devcoop.occount.db.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OpenTelemetry::class, JdbcTelemetry::class)
class DataSourceObservationConfig {

    @Bean
    fun dataSourceTracingPostProcessor(
        openTelemetry: ObjectProvider<OpenTelemetry>,
    ): BeanPostProcessor = object : BeanPostProcessor {
        override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
            if (bean is DataSource) {
                val otel = openTelemetry.ifAvailable ?: return bean
                return JdbcTelemetry.create(otel).wrap(bean)
            }
            return bean
        }
    }
}
