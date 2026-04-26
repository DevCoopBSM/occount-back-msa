package devcoop.occount.db.config

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(OpenTelemetry::class, JdbcTelemetry::class)
class DataSourceObservationConfig {

    @Bean
    @Primary
    fun observedDataSource(
        @Qualifier("dataSource") dataSource: DataSource,
        openTelemetry: ObjectProvider<OpenTelemetry>,
    ): DataSource {
        val otel = openTelemetry.ifAvailable ?: return dataSource
        return JdbcTelemetry.create(otel).wrap(dataSource)
    }
}
