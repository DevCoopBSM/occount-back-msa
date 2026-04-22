package devcoop.occount.warmup

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(StartupWarmupProperties::class)
class StartupWarmupConfiguration
