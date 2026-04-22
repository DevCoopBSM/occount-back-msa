package devcoop.occount.db.config

import com.zaxxer.hikari.HikariDataSource
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.jdbc.datasource.JdbcTelemetry
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

@Configuration
class DataSourceObservationConfig {

    @Bean
    @Primary
    fun observedDataSource(
        dataSource: HikariDataSource,
        openTelemetry: OpenTelemetry
    ): DataSource = JdbcTelemetry.create(openTelemetry).wrap(dataSource)
}
