package devcoop.occount.warmup

import jakarta.persistence.EntityManagerFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.server.reactive.context.ReactiveWebServerApplicationContext
import org.springframework.boot.web.server.servlet.context.ServletWebServerApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.env.Environment
import org.springframework.web.reactive.function.client.WebClient

@AutoConfiguration
@EnableConfigurationProperties(StartupWarmupProperties::class)
class StartupWarmupConfiguration {

    @Bean
    @ConditionalOnClass(EntityManagerFactory::class)
    @ConditionalOnBean(EntityManagerFactory::class)
    @ConditionalOnProperty(
        prefix = "app.startup-warmup",
        name = ["enabled", "jpa-enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun jpaStartupWarmup(
        entityManagerFactory: EntityManagerFactory,
        properties: StartupWarmupProperties,
    ): JpaStartupWarmup = JpaStartupWarmup(entityManagerFactory, properties)

    @Bean
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    @ConditionalOnProperty(
        prefix = "app.startup-warmup",
        name = ["enabled", "http-enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun servletStartupWarmup(
        applicationContext: ServletWebServerApplicationContext,
        environment: Environment,
        properties: StartupWarmupProperties,
    ): ServletStartupWarmup = ServletStartupWarmup(applicationContext, environment, properties)

    @Bean
    @ConditionalOnClass(WebClient::class)
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    @ConditionalOnProperty(
        prefix = "app.startup-warmup",
        name = ["enabled", "http-enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun reactiveStartupWarmup(
        applicationContext: ReactiveWebServerApplicationContext,
        environment: Environment,
        properties: StartupWarmupProperties,
    ): ReactiveStartupWarmup = ReactiveStartupWarmup(applicationContext, environment, properties)

    @Bean
    @ConditionalOnProperty(
        prefix = "app.startup-warmup",
        name = ["enabled", "business-enabled"],
        havingValue = "true",
        matchIfMissing = true,
    )
    fun businessWarmupRunner(
        warmups: List<BusinessWarmup>,
        properties: StartupWarmupProperties,
    ): BusinessWarmupRunner = BusinessWarmupRunner(warmups, properties)
}
